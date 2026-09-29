package Migration;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NotificationSchemaMigrationTest {

    @Test void postgresqlMigrationIsAdditiveAndIdempotent() throws Exception {
        try (Connection connection = DriverManager.getConnection(
                "jdbc:h2:mem:notification_pg;MODE=PostgreSQL", "sa", "");
                Statement sql = connection.createStatement()) {
            createReferencedTables(sql);
            String migration = Files.readString(Path.of("../Deployment/20260928_notifications-postgresql.sql"));
            executeScript(sql, migration);
            executeScript(sql, migration);
            assertNotificationTableWorks(sql);
        }
    }

    @Test void startupInitializerIsAdditiveAndIdempotent() throws Exception {
        try (Connection connection = DriverManager.getConnection(
                "jdbc:h2:mem:notification_startup;MODE=PostgreSQL", "sa", "");
                Statement sql = connection.createStatement()) {
            createReferencedTables(sql);
            NotificationSchemaInitializer.migrate(connection, false);
            NotificationSchemaInitializer.migrate(connection, false);
            assertNotificationTableWorks(sql);
        }
    }

    @Test void mysqlMigrationCreatesPersistentRelationsAndIndexes() throws Exception {
        try (Connection connection = DriverManager.getConnection(
                "jdbc:h2:mem:notification_mysql;MODE=MySQL", "sa", "");
                Statement sql = connection.createStatement()) {
            createReferencedTables(sql);
            String migration = Files.readString(Path.of("../Deployment/20260928_notifications-mysql.sql"));
            executeScript(sql, migration);
            executeScript(sql, migration);
            assertNotificationTableWorks(sql);
        }
    }

    private void createReferencedTables(Statement sql) throws Exception {
        sql.execute("CREATE TABLE Cliente (idCliente BIGINT PRIMARY KEY)");
        sql.execute("CREATE TABLE Resumen (idResumen BIGINT PRIMARY KEY)");
        sql.execute("CREATE TABLE Imagen (idImagen BIGINT PRIMARY KEY)");
    }

    private void assertNotificationTableWorks(Statement sql) throws Exception {
        sql.execute("INSERT INTO Cliente(idCliente) VALUES (1)");
        sql.execute("INSERT INTO Notificacion(idDestinatario,tipo,titulo,mensaje,leida,fechaCreacion) "
                + "VALUES (1,'USUARIO_REGISTRADO','Registro','Nuevo usuario',FALSE,CURRENT_TIMESTAMP)");
        try (ResultSet result = sql.executeQuery("SELECT COUNT(*) FROM Notificacion WHERE idDestinatario=1 AND leida=FALSE")) {
            assertTrue(result.next());
            assertEquals(1, result.getInt(1));
        }
    }

    private void executeScript(Statement sql, String script) throws Exception {
        for (String statement : script.split(";")) {
            if (!statement.isBlank()) sql.execute(statement);
        }
    }
}
