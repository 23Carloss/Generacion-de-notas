package Util;

import java.sql.SQLException;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.logging.Logger;

/** Log only error codes/types: exception messages can contain SQL, credentials or customer data. */
public final class DatabaseDiagnostics {
    private static final Logger LOG = Logger.getLogger(DatabaseDiagnostics.class.getName());
    private DatabaseDiagnostics() {}

    public static void log(String operation, Throwable error) {
        Set<Throwable> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Throwable cause = error; cause != null && visited.add(cause); cause = cause.getCause()) {
            if (cause instanceof SQLException sql) {
                String state = sql.getSQLState();
                state = state != null && state.matches("[A-Z0-9]{5}") ? state : "unknown";
                LOG.severe(operation + ": SQLState=" + state + ", code=" + sql.getErrorCode());
                if ("42703".equals(state) || "42S22".equals(state) || "42122".equals(state)) {
                    LOG.severe("DB_COLUMN_MISSING: falta una columna. Verificar el esquema activo y la migración "
                            + "Deployment/portfolio-postgresql.sql (Resumen.resenaPublica). No recrear la base de datos.");
                }
                return;
            }
        }
        LOG.severe(operation + ": " + error.getClass().getSimpleName());
    }
}
