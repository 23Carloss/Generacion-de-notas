package Util;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Centralizes application configuration provided by the environment. */
public final class AppConfig {

    private static final int DEFAULT_SESSION_IDLE_TIMEOUT_SECONDS = 900;
    private static final int DEFAULT_MAX_REQUEST_BYTES = 1_572_864;

    private AppConfig() {
    }

    public static Map<String, String> databaseProperties() {
        String url = value("DB_URL", null);
        String user = value("DB_USER", null);
        String password = value("DB_PASSWORD", null);

        // En producción las credenciales deben llegar por variables de entorno
        // (o propiedades -D). Durante el desarrollo local, si no se configura
        // ninguna de ellas, JPA toma los valores definidos en persistence.xml.
        if (url == null && user == null && password == null) {
            return Collections.emptyMap();
        }

        if (url == null || user == null || password == null) {
            throw new IllegalStateException(
                    "DB_URL, DB_USER y DB_PASSWORD deben configurarse juntos.");
        }

        String schemaAction = value("DB_SCHEMA_ACTION", "none");
        if (!Set.of("none", "create").contains(schemaAction)) {
            throw new IllegalStateException("DB_SCHEMA_ACTION debe ser 'none' o 'create'.");
        }
        return Map.of(
                "javax.persistence.jdbc.driver", driverFor(url),
                "javax.persistence.jdbc.url", url,
                "javax.persistence.jdbc.user", user,
                "javax.persistence.jdbc.password", password,
                "javax.persistence.schema-generation.database.action", schemaAction
        );
    }

    public static Set<String> allowedOrigins() {
        String configured = value("APP_ALLOWED_ORIGINS", "http://localhost:5173");
        Set<String> origins = Arrays.stream(configured.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (origins.isEmpty() || origins.contains("*")) {
            throw new IllegalStateException("APP_ALLOWED_ORIGINS debe contener orígenes explícitos, nunca '*'.");
        }
        return Collections.unmodifiableSet(origins);
    }

    public static int sessionIdleTimeoutSeconds() {
        return positiveInteger("SESSION_IDLE_TIMEOUT_SECONDS", DEFAULT_SESSION_IDLE_TIMEOUT_SECONDS, 60, 86_400);
    }

    public static int maxRequestBytes() {
        return positiveInteger("MAX_REQUEST_BYTES", DEFAULT_MAX_REQUEST_BYTES, 1_024, 10_485_760);
    }

    public static boolean hstsEnabled() {
        return Boolean.parseBoolean(value("APP_ENABLE_HSTS", "false"));
    }

    private static String driverFor(String url) {
        if (url.startsWith("jdbc:mysql:")) {
            return "com.mysql.cj.jdbc.Driver";
        }
        if (url.startsWith("jdbc:postgresql:")) {
            return "org.postgresql.Driver";
        }
        throw new IllegalStateException("DB_URL debe iniciar con jdbc:mysql: o jdbc:postgresql:.");
    }

    private static String value(String name, String defaultValue) {
        String fromEnvironment = System.getenv(name);
        if (fromEnvironment != null && !fromEnvironment.isBlank()) {
            return fromEnvironment.trim();
        }
        String fromProperty = System.getProperty(name);
        if (fromProperty != null && !fromProperty.isBlank()) {
            return fromProperty.trim();
        }
        return defaultValue;
    }

    private static int positiveInteger(String name, int defaultValue, int minimum, int maximum) {
        String configured = value(name, null);
        if (configured == null) {
            return defaultValue;
        }
        try {
            int parsed = Integer.parseInt(configured);
            if (parsed < minimum || parsed > maximum) {
                throw new IllegalStateException(name + " debe estar entre " + minimum + " y " + maximum + ".");
            }
            return parsed;
        } catch (NumberFormatException e) {
            throw new IllegalStateException(name + " debe ser un número entero.");
        }
    }
}
