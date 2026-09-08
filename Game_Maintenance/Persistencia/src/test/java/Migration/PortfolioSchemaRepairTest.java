package Migration;

import java.sql.*;
import java.util.Map;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class PortfolioSchemaRepairTest {
    @Test void disabledByDefaultDoesNotConnectOrModifyAnything() {
        assertFalse(Util.AppConfig.portfolioSchemaRepairEnabled());
        assertDoesNotThrow(() -> PortfolioSchemaRepair.runIfEnabled(Map.of()));
    }

    @Test void optInRejectsMissingConfigAndCreateBeforeConnecting() {
        System.setProperty("APP_REPAIR_PORTFOLIO_SCHEMA", "true");
        try {
            assertThrows(IllegalStateException.class, () -> PortfolioSchemaRepair.runIfEnabled(Map.of()));
            assertThrows(IllegalStateException.class, () -> PortfolioSchemaRepair.runIfEnabled(Map.of(
                    "javax.persistence.jdbc.url", "jdbc:postgresql://127.0.0.1:1/unused",
                    "javax.persistence.schema-generation.database.action", "create")));
        } finally { System.clearProperty("APP_REPAIR_PORTFOLIO_SCHEMA"); }
    }

    @Test void repairsOnlyResolvedSchemaPreservesRecordsAndIsRepeatableOnRealPostgres() throws Exception {
        String url = System.getenv("GM_TEST_PG_URL");
        Assumptions.assumeTrue(url != null, "Optional: disposable loopback PostgreSQL required");
        assertTrue(url.matches("jdbc:postgresql://127\\.0\\.0\\.1:18766/postgres"), "Never run against production");
        try (Connection c = DriverManager.getConnection(url, "gm_audit", ""); Statement sql = c.createStatement()) {
            String schema = "portfolio_" + java.util.UUID.randomUUID().toString().replace("-", "");
            sql.execute("CREATE SCHEMA " + schema);
            sql.execute("CREATE TABLE " + schema + ".resumen (idresumen BIGINT PRIMARY KEY, privado TEXT)");
            sql.execute("INSERT INTO " + schema + ".resumen VALUES (1, 'private fixture only')");
            sql.execute("CREATE SCHEMA " + schema + "_other");
            sql.execute("CREATE TABLE " + schema + "_other.resumen (idresumen BIGINT PRIMARY KEY)");
            sql.execute("SET search_path TO " + schema + ", " + schema + "_other");
            SQLException missing = assertThrows(SQLException.class, () -> sql.executeQuery("SELECT resenaPublica FROM Resumen"));
            assertEquals("42703", missing.getSQLState());
            PortfolioSchemaRepair.repair(c);
            sql.execute("UPDATE Resumen SET resenapublica = 'review fixture only'");
            PortfolioSchemaRepair.repair(c);
            System.setProperty("APP_REPAIR_PORTFOLIO_SCHEMA", "true");
            try {
                PortfolioSchemaRepair.runIfEnabled(Map.of(
                        "javax.persistence.jdbc.url", url + "?currentSchema=" + schema,
                        "javax.persistence.jdbc.user", "gm_audit",
                        "javax.persistence.jdbc.password", "",
                        "javax.persistence.schema-generation.database.action", "none"));
            } finally { System.clearProperty("APP_REPAIR_PORTFOLIO_SCHEMA"); }
            try (ResultSet r = sql.executeQuery("SELECT idresumen, privado, resenaPublica FROM Resumen")) {
                assertTrue(r.next()); assertEquals(1L, r.getLong(1));
                assertEquals("private fixture only", r.getString(2)); assertEquals("review fixture only", r.getString(3));
                assertFalse(r.next());
            }
            assertEquals("42703", assertThrows(SQLException.class,
                    () -> sql.executeQuery("SELECT resenapublica FROM " + schema + "_other.resumen")).getSQLState());
            sql.execute("SET search_path TO " + schema + "_other");
            sql.execute("ALTER TABLE Resumen ADD COLUMN resenapublica INTEGER");
            assertEquals("42804", assertThrows(SQLException.class, () -> PortfolioSchemaRepair.repair(c)).getSQLState());
            sql.execute("SET search_path TO nonexistent_schema");
            assertEquals("42P01", assertThrows(SQLException.class, () -> PortfolioSchemaRepair.repair(c)).getSQLState());
        }
    }
}
