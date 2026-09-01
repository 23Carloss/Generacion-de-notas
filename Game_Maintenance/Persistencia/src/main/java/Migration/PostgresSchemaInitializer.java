package Migration;

import ConexionDB.ManejadorConexiones;

/** Creates the JPA schema without starting the public Jetty server. */
public final class PostgresSchemaInitializer {

    private PostgresSchemaInitializer() {
    }

    public static void main(String[] args) {
        ManejadorConexiones.Inicializar();
        ManejadorConexiones.cerrar();
        System.out.println("Esquema PostgreSQL creado correctamente.");
    }
}
