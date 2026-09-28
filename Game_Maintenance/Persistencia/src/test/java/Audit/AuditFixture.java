package Audit;

import ConexionDB.ManejadorConexiones;
import DAOs.ClienteDAO;
import Service.TokenService;
import Servlets.*;
import Util.*;
import hp.models.Cliente;
import java.lang.reflect.Field;
import java.util.EnumSet;
import javax.persistence.*;
import javax.servlet.DispatcherType;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.servlet.*;

/** Test-only application. Uses AuditPU (H2 in memory), never DB_URL/TicketsPU. */
public final class AuditFixture implements AutoCloseable {
    public final Server server = new Server();
    public final LocalConnector connector = new LocalConnector(server);
    public final Cliente admin, user, other;
    public final String adminToken, userToken, otherToken;
    private final EntityManagerFactory factory;
    private final Object previousFactory;
    private final Field factoryField;

    public AuditFixture() throws Exception {
        factory = Persistence.createEntityManagerFactory("AuditPU");
        factoryField = ManejadorConexiones.class.getDeclaredField("emFactory");
        factoryField.setAccessible(true);
        previousFactory = factoryField.get(null);
        factoryField.set(null, factory);
        admin = seed("Administrador prueba", "audit-admin@example.test", Cliente.ROL.ADMINISTRADOR);
        user = seed("Usuario prueba", "audit-user@example.test", Cliente.ROL.USUARIO);
        other = seed("Otro usuario", "audit-other@example.test", Cliente.ROL.USUARIO);
        adminToken = TokenService.emitirToken(admin);
        userToken = TokenService.emitirToken(user);
        otherToken = TokenService.emitirToken(other);
        server.addConnector(connector);
        ServletContextHandler ctx = new ServletContextHandler(ServletContextHandler.SESSIONS);
        ctx.setContextPath("/GameMaintenance");
        server.setHandler(ctx);
        ctx.addFilter(new FilterHolder(new SecurityHeadersFilter()), "/*", EnumSet.of(DispatcherType.REQUEST));
        ctx.addFilter(new FilterHolder(new CorsFilter()), "/*", EnumSet.of(DispatcherType.REQUEST));
        ctx.addFilter(new FilterHolder(new RequestSizeFilter()), "/*", EnumSet.of(DispatcherType.REQUEST));
        ctx.addFilter(new FilterHolder(new ErrorHandlingFilter()), "/*", EnumSet.of(DispatcherType.REQUEST));
        FilterHolder auth = new FilterHolder(new AuthFilter());
        ctx.addFilter(auth, "/api/clientes/*", EnumSet.of(DispatcherType.REQUEST));
        ctx.addFilter(auth, "/api/resumenes/*", EnumSet.of(DispatcherType.REQUEST));
        ctx.addFilter(auth, "/api/trabajos-resueltos/*", EnumSet.of(DispatcherType.REQUEST));
        ctx.addFilter(auth, "/api/notificaciones/*", EnumSet.of(DispatcherType.REQUEST));
        ctx.addServlet(new ServletHolder(new AuthServlet()), "/api/auth/*");
        ctx.addServlet(new ServletHolder(new ClienteServlet()), "/api/clientes/*");
        ctx.addServlet(new ServletHolder(new ResumenServlet()), "/api/resumenes/*");
        ctx.addServlet(new ServletHolder(new TrabajosResueltosServlet()), "/api/trabajos-resueltos/*");
        ctx.addServlet(new ServletHolder(new NotificacionServlet()), "/api/notificaciones/*");
    }

    private Cliente seed(String name, String email, Cliente.ROL role) throws Exception {
        Cliente c = new Cliente();
        c.setNombre(name); c.setCorreo(email); c.setTelefono("6441234567"); c.setRol(role);
        c.setPasswordHash(PasswordUtil.hash("Audit-only-123!"));
        new ClienteDAO().insertar(c);
        return c;
    }

    public void close() throws Exception {
        server.stop();
        TokenService.invalidar(adminToken); TokenService.invalidar(userToken); TokenService.invalidar(otherToken);
        factory.close();
        factoryField.set(null, previousFactory);
    }

    public static void main(String[] args) throws Exception {
        try (AuditFixture f = new AuditFixture()) {
            ServerConnector http = new ServerConnector(f.server);
            http.setHost("127.0.0.1"); http.setPort(18765);
            f.server.addConnector(http);
            f.server.start();
            System.out.println("AUDIT READY: loopback port 18765, disposable in-memory database.");
            f.server.join();
        }
    }
}
