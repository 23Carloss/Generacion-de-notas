package ServerMain;

import Servlets.ClienteServlet;
import Servlets.ResumenServlet;
import ConexionDB.ManejadorConexiones;

import java.util.EnumSet;
import javax.servlet.DispatcherType;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import persistencia.CorsFilter;

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

        int puerto = 8080;
        Server server = new Server(puerto);

        ServletContextHandler contexto = new ServletContextHandler(ServletContextHandler.SESSIONS);
        contexto.setContextPath("/GameMaintenance");
        server.setHandler(contexto);

        contexto.addFilter(new FilterHolder(new CorsFilter()), "/*", EnumSet.of(DispatcherType.REQUEST));
        contexto.addServlet(new ServletHolder(new ClienteServlet()), "/api/clientes/*");
        contexto.addServlet(new ServletHolder(new ResumenServlet()), "/api/resumenes/*");

        Runtime.getRuntime().addShutdownHook(new Thread(ManejadorConexiones::cerrar));

        server.start();
        System.out.println("=====================================================");
        System.out.println(" Game Maintenance API lista en:");
        System.out.println(" http://localhost:" + puerto + "/GameMaintenance/api");
        System.out.println("=====================================================");
        server.join();
    }
}
