package Migration;

import Util.DatabaseDiagnostics;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.Map;

/**
 * Ensures the additive notification schema exists before JPA starts.
 *
 * <p>Production deliberately uses DB_SCHEMA_ACTION=none, so new tables must be
 * created by an explicit, bounded migration. Running this on every PostgreSQL
 * startup is safe because all statements are idempotent and no existing rows
 * or columns are changed.</p>
 */
public final class NotificationSchemaInitializer {
    private NotificationSchemaInitializer() {}

    public static void ensure(Map<String, String> properties) {
        String url = properties.get("javax.persistence.jdbc.url");
        String action = properties.get("javax.persistence.schema-generation.database.action");
        if (url == null || !url.startsWith("jdbc:postgresql:") || !"none".equals(action)) return;

        try (Connection connection = DriverManager.getConnection(url,
                properties.get("javax.persistence.jdbc.user"),
                properties.get("javax.persistence.jdbc.password"))) {
            migrate(connection, true);
            System.out.println("NOTIFICATION_SCHEMA_READY: tabla e índices verificados.");
        } catch (SQLException | RuntimeException e) {
            DatabaseDiagnostics.log("inicialización del esquema de notificaciones", e);
            // JDBC exception messages may expose connection details, so do not attach the cause.
            throw new IllegalStateException("NOTIFICATION_SCHEMA_INIT_FAILED: no se inició la API. "
                    + "Revisa permisos DDL, esquema activo y SQLState en los logs. No recrees la base.");
        }
    }

    static void migrate(Connection connection, boolean applyPostgresTimeouts) throws SQLException {
        if (!connection.getAutoCommit()) {
            throw new SQLException("La migración requiere una conexión independiente.");
        }
        connection.setAutoCommit(false);
        try (Statement sql = connection.createStatement()) {
            if (applyPostgresTimeouts) {
                sql.execute("SET LOCAL lock_timeout = '10s'");
                sql.execute("SET LOCAL statement_timeout = '30s'");
            }

            // Refuse to create relations against an empty or unexpected database/schema.
            verifyColumnExists(sql, "SELECT idCliente FROM Cliente WHERE 1 = 0");
            verifyColumnExists(sql, "SELECT idResumen FROM Resumen WHERE 1 = 0");
            verifyColumnExists(sql, "SELECT idImagen FROM Imagen WHERE 1 = 0");

            sql.execute("CREATE TABLE IF NOT EXISTS Notificacion ("
                    + "idNotificacion BIGSERIAL PRIMARY KEY, "
                    + "idDestinatario BIGINT NOT NULL REFERENCES Cliente(idCliente), "
                    + "tipo VARCHAR(40) NOT NULL, "
                    + "titulo VARCHAR(160) NOT NULL, "
                    + "mensaje VARCHAR(1000) NOT NULL, "
                    + "leida BOOLEAN NOT NULL DEFAULT FALSE, "
                    + "fechaCreacion TIMESTAMP NOT NULL, "
                    + "idResumen BIGINT NULL REFERENCES Resumen(idResumen), "
                    + "idImagen BIGINT NULL REFERENCES Imagen(idImagen), "
                    + "idClienteRelacionado BIGINT NULL"
                    + ")");
            sql.execute("CREATE INDEX IF NOT EXISTS idx_notificacion_destinatario_fecha "
                    + "ON Notificacion (idDestinatario, fechaCreacion DESC)");
            sql.execute("CREATE INDEX IF NOT EXISTS idx_notificacion_destinatario_leida "
                    + "ON Notificacion (idDestinatario, leida)");

            validateExistingTable(sql);
            connection.commit();
        } catch (SQLException | RuntimeException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(true);
        }
    }

    private static void verifyColumnExists(Statement sql, String query) throws SQLException {
        try (ResultSet ignored = sql.executeQuery(query)) {
            // Preparing/executing the zero-row query proves that the expected table and key exist.
        }
    }

    private static void validateExistingTable(Statement sql) throws SQLException {
        try (ResultSet rows = sql.executeQuery("SELECT idNotificacion, idDestinatario, tipo, titulo, mensaje, "
                + "leida, fechaCreacion, idResumen, idImagen, idClienteRelacionado "
                + "FROM Notificacion WHERE 1 = 0")) {
            int idType = rows.getMetaData().getColumnType(1);
            int readType = rows.getMetaData().getColumnType(6);
            // PostgreSQL's JDBC driver exposes the native bool type as Types.BIT,
            // while H2 and other drivers commonly expose it as Types.BOOLEAN.
            if (idType != Types.BIGINT || !isBooleanJdbcType(readType)) {
                throw new SQLException("La tabla Notificacion existente no tiene el formato esperado.", "42804");
            }
        }
    }

    static boolean isBooleanJdbcType(int jdbcType) {
        return jdbcType == Types.BOOLEAN || jdbcType == Types.BIT;
    }
}
