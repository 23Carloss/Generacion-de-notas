package ServerMain;

import Servlets.ClienteServlet;
import Servlets.ResumenServlet;
import ConexionDB.ManejadorConexiones;
import Servlets.AuthServlet;
import Util.AuthFilter;
import Util.ErrorHandlingFilter;
import Util.RequestSizeFilter;
import Util.SecurityHeadersFilter;

import java.util.EnumSet;
import javax.servlet.DispatcherType;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import Util.CorsFilter;

/**
 * Punto de entrada del backend. Levanta un servidor Jetty embebido (no
 * necesitas instalar Tomcat/GlassFish) que expone la API REST usada por
 * game-maintenance-frontend, conectada a la base de datos MySQL definida en
 * Models/src/main/resources/META-INF/persistence.xml.
 *
 * Cómo correrlo: ver SETUP_BACKEND.md en la raíz de Game_Maintenance.
 */
public class ServerMain {

    public static void main(String[] args) throws Exception {
        ManejadorConexiones.Inicializar();
 
        int puerto = puertoConfigurado();
        Server server = new Server(puerto);
 
        ServletContextHandler contexto = new ServletContextHandler(ServletContextHandler.SESSIONS);
        contexto.setContextPath("/GameMaintenance");
        server.setHandler(contexto);
 
        contexto.addFilter(new FilterHolder(new SecurityHeadersFilter()), "/*", EnumSet.of(DispatcherType.REQUEST));
        contexto.addFilter(new FilterHolder(new CorsFilter()), "/*", EnumSet.of(DispatcherType.REQUEST));
        contexto.addFilter(new FilterHolder(new RequestSizeFilter()), "/*", EnumSet.of(DispatcherType.REQUEST));
        contexto.addFilter(new FilterHolder(new ErrorHandlingFilter()), "/*", EnumSet.of(DispatcherType.REQUEST));
 
        // AuthFilter protege /api/clientes/* y /api/resumenes/*. /api/auth/*
        // se deja fuera a propósito: ahí es donde se obtiene el token, así
        // que tiene que ser accesible sin uno.
        FilterHolder authFilterHolder = new FilterHolder(new AuthFilter());
        contexto.addFilter(authFilterHolder, "/api/clientes/*", EnumSet.of(DispatcherType.REQUEST));
        contexto.addFilter(authFilterHolder, "/api/resumenes/*", EnumSet.of(DispatcherType.REQUEST));
 
        contexto.addServlet(new ServletHolder(new AuthServlet()), "/api/auth/*");
        contexto.addServlet(new ServletHolder(new ClienteServlet()), "/api/clientes/*");
        contexto.addServlet(new ServletHolder(new ResumenServlet()), "/api/resumenes/*");
 
        Runtime.getRuntime().addShutdownHook(new Thread(ManejadorConexiones::cerrar));
 
        server.start();
        System.out.println("=====================================================");
        System.out.println(" Game Maintenance API lista en:");
        System.out.println(" Puerto HTTP: " + puerto);
        System.out.println(" API disponible en /GameMaintenance/api");
        System.out.println("=====================================================");
        server.join();
    }

    /**
     * Render proporciona el puerto HTTP mediante PORT. En desarrollo se
     * conserva 8080 para no modificar el flujo local.
     */
    private static int puertoConfigurado() {
        String valor = System.getenv().getOrDefault("PORT", "8080").trim();
        try {
            int puerto = Integer.parseInt(valor);
            if (puerto < 1 || puerto > 65_535) {
                throw new IllegalArgumentException();
            }
            return puerto;
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("PORT debe ser un entero entre 1 y 65535.");
        }
    }
}
