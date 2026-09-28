package Audit;

import Util.JsonUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class NotificationApiTest {
    static AuditFixture app;

    record Reply(int status, String body) {
        JsonNode json() throws Exception { return JsonUtil.MAPPER.readTree(body); }
    }

    @BeforeAll static void start() throws Exception { app = new AuditFixture(); app.server.start(); }
    @AfterAll static void stop() throws Exception { if (app != null) app.close(); }

    static Reply send(String method, String path, String token, Object body) throws Exception {
        String data = body == null ? "" : JsonUtil.MAPPER.writeValueAsString(body);
        String response = app.connector.getResponse(method + " /GameMaintenance/api" + path
                + " HTTP/1.1\r\nHost: localhost\r\nConnection: close\r\nContent-Type: application/json\r\n"
                + (token == null ? "" : "Authorization: Bearer " + token + "\r\n")
                + "Content-Length: " + data.getBytes(StandardCharsets.UTF_8).length + "\r\n\r\n" + data);
        return new Reply(Integer.parseInt(response.split(" ")[1]), response.substring(response.indexOf("\r\n\r\n") + 4));
    }

    static ObjectNode ticket(long ownerId) throws Exception {
        return (ObjectNode) JsonUtil.MAPPER.readTree("""
            {"cliente":{"id":%d},"descripcionProblema":"No enciende","comentariosCliente":"Prueba",
             "listaDispositivos":[{"modeloDispositivo":"PS5","detallesDispositivo":"Cable","plataforma":"PLAYSTATION"}],
             "listaTrabajos":[{"tipoTrabajo":"DIAGNOSTICO","precio":100}]}
            """.formatted(ownerId));
    }

    static long createTicket(long ownerId) throws Exception {
        Reply response = send("POST", "/resumenes", app.adminToken, ticket(ownerId));
        assertEquals(201, response.status, response.body);
        return response.json().path("id").asLong();
    }

    static JsonNode notifications(String token) throws Exception {
        Reply response = send("GET", "/notificaciones?page=0&size=100", token, null);
        assertEquals(200, response.status, response.body);
        return response.json().path("notificaciones");
    }

    static long countType(JsonNode items, String type, long ticketId) {
        long count = 0;
        for (JsonNode item : items) {
            if (type.equals(item.path("tipo").asText()) && item.path("ticketId").asLong(-1) == ticketId) count++;
        }
        return count;
    }

    @Test void registrationNotifiesAdministratorsWithoutTrustingRequestedRole() throws Exception {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        Reply registered = send("POST", "/auth/register", null, Map.of(
                "nombre", "Cliente nuevo", "telefono", "6445551212",
                "correo", "notify-" + unique + "@example.test", "password", "Audit-only-123!"));
        assertEquals(201, registered.status, registered.body);
        assertEquals("USUARIO", registered.json().path("cliente").path("rol").asText());
        JsonNode adminItems = notifications(app.adminToken);
        assertTrue(countType(adminItems, "USUARIO_REGISTRADO", -1) >= 1);
        assertEquals(0, countType(notifications(app.otherToken), "USUARIO_REGISTRADO", -1));
    }

    @Test void statusChangeNotifiesAdminAndOwnerOnlyWhenStateActuallyChanges() throws Exception {
        long ticketId = createTicket(app.user.getId());
        assertEquals(200, send("PUT", "/resumenes/" + ticketId + "/estado", app.adminToken,
                Map.of("estado", "Entregado")).status);
        assertEquals(1, countType(notifications(app.adminToken), "ESTADO_TICKET_CAMBIADO", ticketId));
        assertEquals(1, countType(notifications(app.userToken), "ESTADO_TICKET_CAMBIADO", ticketId));
        assertEquals(0, countType(notifications(app.otherToken), "ESTADO_TICKET_CAMBIADO", ticketId));

        assertEquals(200, send("PUT", "/resumenes/" + ticketId + "/estado", app.adminToken,
                Map.of("estado", "Entregado")).status);
        assertEquals(1, countType(notifications(app.adminToken), "ESTADO_TICKET_CAMBIADO", ticketId));
        assertEquals(1, countType(notifications(app.userToken), "ESTADO_TICKET_CAMBIADO", ticketId));
    }

    @Test void reviewNotifiesAdminAndImageNotifiesOnlyTicketOwner() throws Exception {
        long ticketId = createTicket(app.user.getId());
        send("PUT", "/resumenes/" + ticketId + "/estado", app.adminToken, Map.of("estado", "Entregado"));
        Reply review = send("PUT", "/resumenes/" + ticketId + "/resena", app.userToken,
                Map.of("calificacion", 5, "resenaComentario", "Excelente"));
        assertEquals(200, review.status, review.body);
        assertEquals(1, countType(notifications(app.adminToken), "RESENA_CREADA", ticketId));
        assertEquals(0, countType(notifications(app.userToken), "RESENA_CREADA", ticketId));

        Reply image = send("POST", "/resumenes/" + ticketId + "/imagenes", app.adminToken,
                Map.of("dataBase64", "data:image/png;base64,iVBORw0KGgo=", "descripcion", "Equipo recibido"));
        assertEquals(201, image.status, image.body);
        assertEquals(1, countType(notifications(app.userToken), "IMAGEN_TICKET_AGREGADA", ticketId));
        assertEquals(0, countType(notifications(app.otherToken), "IMAGEN_TICKET_AGREGADA", ticketId));
        assertEquals(0, countType(notifications(app.adminToken), "IMAGEN_TICKET_AGREGADA", ticketId));
    }

    @Test void foreignNotificationCannotBeReadOrUsedToAccessForeignTicket() throws Exception {
        long ticketId = createTicket(app.user.getId());
        send("PUT", "/resumenes/" + ticketId + "/estado", app.adminToken, Map.of("estado", "Entregado"));
        JsonNode userItems = notifications(app.userToken);
        long notificationId = -1;
        for (JsonNode item : userItems) {
            if (item.path("ticketId").asLong(-1) == ticketId) { notificationId = item.path("id").asLong(); break; }
        }
        assertTrue(notificationId > 0);
        assertEquals(404, send("PATCH", "/notificaciones/" + notificationId + "/read", app.otherToken, null).status);
        assertEquals(403, send("GET", "/resumenes/" + ticketId, app.otherToken, null).status);
        assertEquals(200, send("PATCH", "/notificaciones/" + notificationId + "/read", app.userToken, null).status);
        JsonNode leidas = send("GET", "/notificaciones?unread=false&page=0&size=100", app.userToken, null).json();
        assertTrue(leidas.path("notificaciones").toString().contains("\"id\":" + notificationId));
        assertEquals(200, send("PATCH", "/notificaciones/read-all", app.userToken, null).status);
        assertEquals(0, send("GET", "/notificaciones/unread-count", app.userToken, null).json().path("count").asLong());
    }

    @Test void listValidationAndAuthenticationAreEnforced() throws Exception {
        assertEquals(401, send("GET", "/notificaciones", null, null).status);
        assertEquals(400, send("GET", "/notificaciones?page=-1", app.userToken, null).status);
        assertEquals(400, send("GET", "/notificaciones?page=2147483647", app.userToken, null).status);
        assertEquals(400, send("GET", "/notificaciones?size=101", app.userToken, null).status);
        assertEquals(400, send("GET", "/notificaciones?unread=talvez", app.userToken, null).status);
    }
}
