package Migration;

import Util.AppConfig;
import Util.DatabaseDiagnostics;
import java.sql.*;
import java.util.Map;

/** Explicit opt-in, additive repair using the SAME JDBC properties as the API. */
public final class PortfolioSchemaRepair {
    private PortfolioSchemaRepair() {}

    public static void runIfEnabled(Map<String, String> properties) {
        if (!AppConfig.portfolioSchemaRepairEnabled()) return;
        String url = properties.get("javax.persistence.jdbc.url");
        if (url == null || !url.startsWith("jdbc:postgresql:")
                || !"none".equals(properties.get("javax.persistence.schema-generation.database.action"))) {
            throw new IllegalStateException("La reparación requiere DB_URL PostgreSQL, DB_USER, DB_PASSWORD y DB_SCHEMA_ACTION=none.");
        }
        try (Connection connection = DriverManager.getConnection(url,
                properties.get("javax.persistence.jdbc.user"), properties.get("javax.persistence.jdbc.password"))) {
            repair(connection);
            System.out.println("PORTFOLIO_SCHEMA_READY: columna resenapublica verificada en la tabla usada por la API. "
                    + "Puedes desactivar APP_REPAIR_PORTFOLIO_SCHEMA.");
        } catch (SQLException | RuntimeException e) {
            DatabaseDiagnostics.log("reparación del esquema de reseñas", e);
            // Do not attach the JDBC exception: it may include connection details in its message.
            throw new IllegalStateException("PORTFOLIO_SCHEMA_REPAIR_FAILED: no se inició la API. "
                    + "Revisa permisos, esquema activo y SQLState en los logs. No recrees la base.");
        }
    }

    static void repair(Connection connection) throws SQLException {
        if (!connection.getAutoCommit()) throw new SQLException("La reparación requiere una conexión independiente.");
        connection.setAutoCommit(false);
        try (Statement sql = connection.createStatement()) {
            // Bound waiting time; a busy/locked table must not hang a deployment indefinitely.
            sql.execute("SET LOCAL lock_timeout = '10s'");
            sql.execute("SET LOCAL statement_timeout = '30s'");
            String table;
            try (ResultSet rows = sql.executeQuery("SELECT n.nspname, c.relname FROM pg_catalog.pg_class c "
                    + "JOIN pg_catalog.pg_namespace n ON n.oid = c.relnamespace "
                    + "WHERE c.oid = pg_catalog.to_regclass('resumen') AND c.relkind IN ('r', 'p')")) {
                if (!rows.next()) throw new SQLException("No existe la tabla Resumen del esquema activo.", "42P01");
                table = quote(rows.getString(1)) + "." + quote(rows.getString(2));
            }
            // Serializes two simultaneous deployments and pins the resolved table for this transaction.
            sql.execute("LOCK TABLE " + table + " IN ACCESS EXCLUSIVE MODE");
            try (ResultSet ignored = sql.executeQuery("SELECT idResumen FROM " + table + " WHERE 1 = 0")) {
                // Refuse to modify an unrelated table with a similar name.
            }
            sql.execute("ALTER TABLE " + table + " ADD COLUMN IF NOT EXISTS resenapublica VARCHAR(600) NULL");
            try (ResultSet checked = sql.executeQuery("SELECT resenaPublica FROM Resumen WHERE 1 = 0")) {
                int type = checked.getMetaData().getColumnType(1);
                if (type != Types.VARCHAR && type != Types.LONGVARCHAR)
                    throw new SQLException("La columna existente no es de texto.", "42804");
            }
            connection.commit();
        } catch (SQLException | RuntimeException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(true);
        }
    }

    private static String quote(String identifier) { return "\"" + identifier.replace("\"", "\"\"") + "\""; }
}
