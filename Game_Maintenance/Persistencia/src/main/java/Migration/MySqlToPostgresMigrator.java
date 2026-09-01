package Migration;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Copies the application's existing MySQL records into an empty PostgreSQL
 * schema. It intentionally preserves primary keys, foreign keys, roles and
 * password hashes. Database credentials are obtained only from environment
 * variables or JVM properties; they are never written to a file.
 */
public final class MySqlToPostgresMigrator {

    private static final List<String> TABLES = List.of(
            "cliente", "resumen", "dispositivo", "trabajo", "pieza", "imagen", "resumen_dispositivos"
    );

    private MySqlToPostgresMigrator() {
    }

    public static void main(String[] args) throws Exception {
        Config config = Config.fromEnvironment();
        Class.forName("com.mysql.cj.jdbc.Driver");
        Class.forName("org.postgresql.Driver");

        try (Connection source = DriverManager.getConnection(config.sourceUrl, config.sourceUser, config.sourcePassword);
                Connection target = DriverManager.getConnection(config.targetUrl, config.targetUser, config.targetPassword)) {
            verifyDatabase(source, "MySQL");
            verifyDatabase(target, "PostgreSQL");

            Map<String, String> sourceTables = tablesByLowercaseName(source);
            Map<String, String> targetTables = tablesByLowercaseName(target);
            verifySchema(sourceTables, targetTables);
            verifyTargetIsEmpty(target, targetTables);

            target.setAutoCommit(false);
            try {
                for (String table : TABLES) {
                    int copied = copyTable(source, target, sourceTables.get(table), targetTables.get(table));
                    System.out.println(table + ": " + copied + " registros copiados.");
                }
                synchronizeSequences(target, targetTables);
                target.commit();
                System.out.println("Migración completada correctamente.");
            } catch (Exception e) {
                target.rollback();
                throw e;
            }
        }
    }

    private static void verifyDatabase(Connection connection, String expected) throws SQLException {
        String product = connection.getMetaData().getDatabaseProductName();
        if (!product.toLowerCase(Locale.ROOT).contains(expected.toLowerCase(Locale.ROOT))) {
            throw new IllegalStateException("Se esperaba " + expected + " pero se recibió " + product + ".");
        }
    }

    private static Map<String, String> tablesByLowercaseName(Connection connection) throws SQLException {
        Map<String, String> tables = new LinkedHashMap<>();
        try (ResultSet result = connection.getMetaData().getTables(null, null, "%", new String[]{"TABLE"})) {
            while (result.next()) {
                String name = result.getString("TABLE_NAME");
                tables.putIfAbsent(name.toLowerCase(Locale.ROOT), name);
            }
        }
        return tables;
    }

    private static void verifySchema(Map<String, String> sourceTables, Map<String, String> targetTables) {
        for (String table : TABLES) {
            if (!sourceTables.containsKey(table)) {
                throw new IllegalStateException("La base MySQL no contiene la tabla " + table + ".");
            }
            if (!targetTables.containsKey(table)) {
                throw new IllegalStateException(
                        "PostgreSQL no contiene la tabla " + table + ". Crea primero el esquema con DB_SCHEMA_ACTION=create.");
            }
        }
    }

    private static void verifyTargetIsEmpty(Connection target, Map<String, String> targetTables) throws SQLException {
        for (String table : TABLES) {
            try (Statement statement = target.createStatement();
                    ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM " + quote(targetTables.get(table)))) {
                result.next();
                if (result.getLong(1) != 0) {
                    throw new IllegalStateException(
                            "PostgreSQL ya contiene datos en " + table + ". La migración se detuvo para evitar duplicados.");
                }
            }
        }
    }

    private static int copyTable(Connection source, Connection target, String sourceTable, String targetTable)
            throws SQLException {
        Map<String, String> targetColumns = targetColumns(target, targetTable);
        String select = "SELECT * FROM " + quoteMySql(sourceTable);
        int copied = 0;

        try (Statement sourceStatement = source.createStatement(); ResultSet rows = sourceStatement.executeQuery(select)) {
            ResultSetMetaData metadata = rows.getMetaData();
            List<String> sourceColumns = new ArrayList<>();
            List<String> matchingTargetColumns = new ArrayList<>();
            for (int index = 1; index <= metadata.getColumnCount(); index++) {
                String sourceColumn = metadata.getColumnLabel(index);
                String targetColumn = targetColumns.get(sourceColumn.toLowerCase(Locale.ROOT));
                if (targetColumn == null) {
                    throw new IllegalStateException(
                            "La columna " + sourceTable + "." + sourceColumn + " no existe en PostgreSQL.");
                }
                sourceColumns.add(sourceColumn);
                matchingTargetColumns.add(targetColumn);
            }

            String insert = "INSERT INTO " + quote(targetTable) + " ("
                    + String.join(", ", matchingTargetColumns.stream().map(MySqlToPostgresMigrator::quote).toList())
                    + ") VALUES (" + "?, ".repeat(matchingTargetColumns.size() - 1) + "?)";

            try (PreparedStatement destinationStatement = target.prepareStatement(insert)) {
                while (rows.next()) {
                    for (int index = 0; index < sourceColumns.size(); index++) {
                        destinationStatement.setObject(index + 1, rows.getObject(sourceColumns.get(index)));
                    }
                    destinationStatement.executeUpdate();
                    copied++;
                }
            }
        }
        return copied;
    }

    private static Map<String, String> targetColumns(Connection target, String targetTable) throws SQLException {
        Map<String, String> columns = new LinkedHashMap<>();
        try (Statement statement = target.createStatement();
                ResultSet result = statement.executeQuery("SELECT * FROM " + quote(targetTable) + " WHERE 1 = 0")) {
            ResultSetMetaData metadata = result.getMetaData();
            for (int index = 1; index <= metadata.getColumnCount(); index++) {
                String column = metadata.getColumnLabel(index);
                columns.put(column.toLowerCase(Locale.ROOT), column);
            }
        }
        return columns;
    }

    private static void synchronizeSequences(Connection target, Map<String, String> targetTables) throws SQLException {
        DatabaseMetaData metadata = target.getMetaData();
        for (String targetTable : targetTables.values()) {
            Set<String> primaryKeys = new LinkedHashSet<>();
            try (ResultSet keys = metadata.getPrimaryKeys(null, null, targetTable)) {
                while (keys.next()) {
                    primaryKeys.add(keys.getString("COLUMN_NAME"));
                }
            }
            for (String primaryKey : primaryKeys) {
                String sequence = serialSequence(target, targetTable, primaryKey);
                if (sequence == null) {
                    continue;
                }
                String sql = "SELECT setval(CAST(? AS regclass), COALESCE((SELECT MAX(" + quote(primaryKey)
                        + ") FROM " + quote(targetTable) + "), 1), true)";
                try (PreparedStatement statement = target.prepareStatement(sql)) {
                    statement.setString(1, sequence);
                    statement.execute();
                }
            }
        }
    }

    private static String serialSequence(Connection target, String table, String column) throws SQLException {
        try (PreparedStatement statement = target.prepareStatement("SELECT pg_get_serial_sequence(?, ?)")) {
            statement.setString(1, table);
            statement.setString(2, column);
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getString(1);
            }
        }
    }

    private static String quote(String identifier) {
        return '"' + identifier.replace("\"", "\"\"") + '"';
    }

    private static String quoteMySql(String identifier) {
        return '`' + identifier.replace("`", "``") + '`';
    }

    private static final class Config {
        private final String sourceUrl;
        private final String sourceUser;
        private final String sourcePassword;
        private final String targetUrl;
        private final String targetUser;
        private final String targetPassword;

        private Config(String sourceUrl, String sourceUser, String sourcePassword,
                String targetUrl, String targetUser, String targetPassword) {
            this.sourceUrl = sourceUrl;
            this.sourceUser = sourceUser;
            this.sourcePassword = sourcePassword;
            this.targetUrl = targetUrl;
            this.targetUser = targetUser;
            this.targetPassword = targetPassword;
        }

        private static Config fromEnvironment() {
            return new Config(
                    required("MIGRATION_SOURCE_URL"),
                    required("MIGRATION_SOURCE_USER"),
                    required("MIGRATION_SOURCE_PASSWORD"),
                    required("MIGRATION_TARGET_URL"),
                    required("MIGRATION_TARGET_USER"),
                    required("MIGRATION_TARGET_PASSWORD")
            );
        }

        private static String required(String name) {
            String value = System.getenv(name);
            if (value == null || value.isBlank()) {
                value = System.getProperty(name);
            }
            if (value == null || value.isBlank()) {
                throw new IllegalStateException("Falta " + name + ".");
            }
            return value.trim();
        }
    }
}
