package Audit;

import DAOs.ClienteDAO;
import Util.JsonUtil;
import hp.models.Cliente;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/** Functional checks plus characterization of security findings, not fixes. */
class ApiAuditTest {
    static AuditFixture app;
    record Reply(int status, String headers, String body) {
        JsonNode json() throws Exception { return JsonUtil.MAPPER.readTree(body); }
    }
    @BeforeAll static void start() throws Exception { app = new AuditFixture(); app.server.start(); }
    @AfterAll static void stop() throws Exception { if (app != null) app.close(); }

    static Reply send(String method, String path, String token, Object body) throws Exception {
        String data = body == null ? "" : JsonUtil.MAPPER.writeValueAsString(body);
        return raw(method, path, token, data, false, "");
    }
    static Reply raw(String method, String path, String token, String data, boolean chunked, String extra) throws Exception {
        String framing = chunked ? "Transfer-Encoding: chunked\r\n" : "Content-Length: " + data.getBytes(StandardCharsets.UTF_8).length + "\r\n";
        String payload = chunked ? Integer.toHexString(data.getBytes(StandardCharsets.UTF_8).length) + "\r\n" + data + "\r\n0\r\n\r\n" : data;
        String req = method + " /GameMaintenance/api" + path + " HTTP/1.1\r\nHost: localhost\r\nConnection: close\r\nContent-Type: application/json\r\n"
                + (token == null ? "" : "Authorization: Bearer " + token + "\r\n") + extra + framing + "\r\n" + payload;
        String response = app.connector.getResponse(req);
        int split = response.indexOf("\r\n\r\n");
        return new Reply(Integer.parseInt(response.split(" ")[1]), response.substring(0, split), response.substring(split + 4));
    }
    static Map<String, Object> contact(String name) { return Map.of("nombre", name, "telefono", "+526441234567"); }
    static ObjectNode ticket() throws Exception {
        return (ObjectNode) JsonUtil.MAPPER.readTree("""
          {"cliente":{"id":%d},"descripcionProblema":"No enciende","comentariosCliente":"Prueba",
           "listaDispositivos":[{"modeloDispositivo":"PS4","detallesDispositivo":"Cable","plataforma":"PLAYSTATION"}],
           "listaTrabajos":[{"tipoTrabajo":"REPARACION","nombrePieza":"HDMI","unidades":1,"precioUnitario":100,"precio":100}]}
          """.formatted(app.user.getId()));
    }
    static long createTicket(ObjectNode body) throws Exception {
        Reply r = send("POST", "/resumenes", app.adminToken, body);
        assertEquals(201, r.status, r.body); return r.json().get("id").asLong();
    }

    @Test void clientNamesAndPhonesEnforceLimitsOnApi() throws Exception {
        assertEquals(201, send("POST", "/clientes", app.adminToken, contact("x".repeat(100))).status);
        assertEquals(400, send("POST", "/clientes", app.adminToken, contact("x".repeat(101))).status);
        assertEquals(400, send("POST", "/clientes", app.adminToken, contact(" ")).status);
        assertEquals(400, send("POST", "/clientes", app.adminToken, Map.of("nombre", "Test", "telefono", "abc1234567")).status);
    }
    @Test void adminEditsRegisteredContactWithoutChangingRolePasswordOrTickets() throws Exception {
        ClienteDAO dao = new ClienteDAO();
        String hash = dao.buscarPorId(app.other.getId()).getPasswordHash();
        ObjectNode data = ticket(); ((ObjectNode) data.get("cliente")).put("id", app.other.getId());
        long ticketId = createTicket(data);
        Reply r = send("PUT", "/clientes/" + app.other.getId(), app.adminToken,
            Map.of("nombre", "Cliente editado", "telefono", "+14155552671", "correo", "changed@example.test", "rol", "ADMINISTRADOR", "passwordHash", "inventado"));
        assertEquals(200, r.status, r.body);
        Cliente edited = dao.buscarPorId(app.other.getId());
        assertEquals(Cliente.ROL.USUARIO, edited.getRol()); assertEquals(hash, edited.getPasswordHash());
        assertEquals("changed@example.test", edited.getCorreo());
        assertEquals("Cliente editado", send("GET", "/resumenes/" + ticketId, app.adminToken, null).json().path("cliente").path("nombre").asText());
        assertEquals(400, send("PUT", "/clientes/" + app.other.getId(), app.adminToken,
            Map.of("nombre", "x", "telefono", "6441234567", "correo", "")).status);
        assertEquals(400, send("PUT", "/clientes/" + app.other.getId(), app.adminToken,
            Map.of("nombre", "x", "telefono", "6441234567", "correo", app.admin.getCorreo())).status);
        assertEquals(400, send("PUT", "/clientes/" + app.other.getId(), app.adminToken, contact("x".repeat(101))).status);
    }
    @Test void guestContactCanBeEditedWithoutEmail() throws Exception {
        long id = send("POST", "/clientes", app.adminToken, contact("Sin cuenta")).json().get("id").asLong();
        Reply r = send("PUT", "/clientes/" + id, app.adminToken, Map.of("nombre", "Editado", "telefono", "6441234567", "correo", ""));
        assertEquals(200, r.status, r.body); assertTrue(r.json().path("correo").isNull());
    }
    @Test void userCanEditOnlyOwnProfile() throws Exception {
        assertEquals(200, send("PUT", "/clientes/" + app.user.getId(), app.userToken, contact("Usuario prueba")).status);
        assertEquals(403, send("PUT", "/clientes/" + app.admin.getId(), app.userToken, contact("Ataque")).status);
        assertEquals(401, send("PUT", "/clientes/" + app.user.getId(), null, contact("Ataque")).status);
        assertEquals(401, send("GET", "/clientes", "invalid-token", null).status);
    }
    @Test void ticketMaximumsAreAcceptedTogether() throws Exception {
        ObjectNode d = ticket();
        ((ObjectNode) d.at("/listaDispositivos/0")).put("modeloDispositivo", "m".repeat(50)).put("detallesDispositivo", "d".repeat(150));
        ((ObjectNode) d.at("/listaTrabajos/0")).put("nombrePieza", "p".repeat(75)).put("unidades", 50).put("precioUnitario", 50000);
        long id = createTicket(d);
        Reply updated = send("PUT", "/resumenes/" + id, app.adminToken, d);
        assertEquals(200, updated.status, updated.body);
        assertEquals(2500000, updated.json().at("/listaTrabajos/0/precio").asDouble());
    }
    @Test void invalidTicketValuesAreRejectedAndEditsRollBack() throws Exception {
        ObjectNode good = ticket(); long id = createTicket(good);
        for (String field : new String[]{"modeloDispositivo", "detallesDispositivo", "nombrePieza", "unidades", "precioUnitario"}) {
            ObjectNode bad = good.deepCopy();
            switch (field) {
                case "modeloDispositivo" -> ((ObjectNode) bad.at("/listaDispositivos/0")).put(field, "x".repeat(51));
                case "detallesDispositivo" -> ((ObjectNode) bad.at("/listaDispositivos/0")).put(field, "x".repeat(151));
                case "nombrePieza" -> ((ObjectNode) bad.at("/listaTrabajos/0")).put(field, "x".repeat(76));
                case "unidades" -> ((ObjectNode) bad.at("/listaTrabajos/0")).put(field, 51);
                default -> ((ObjectNode) bad.at("/listaTrabajos/0")).put(field, 50000.01);
            }
            assertEquals(400, send("POST", "/resumenes", app.adminToken, bad).status, field);
            assertEquals(400, send("PUT", "/resumenes/" + id, app.adminToken, bad).status, field);
        }
        for (String value : new String[]{"NaN", "Infinity", "-Infinity", "-1"}) {
            ObjectNode bad = good.deepCopy(); ((ObjectNode) bad.at("/listaTrabajos/0")).put("precioUnitario", value);
            assertEquals(400, send("POST", "/resumenes", app.adminToken, bad).status, value);
        }
        ObjectNode fraction = good.deepCopy();
        ((ObjectNode) fraction.at("/listaTrabajos/0")).put("unidades", 50.9);
        assertEquals(400, send("POST", "/resumenes", app.adminToken, fraction).status);
        assertEquals("PS4", send("GET", "/resumenes/" + id, app.adminToken, null).json().at("/listaDispositivos/0/modeloDispositivo").asText());
    }
    @Test void sqlPayloadsRemainDataAcrossTextFields() throws Exception {
        ClienteDAO dao = new ClienteDAO();
        for (String probe : new String[]{"' OR '1'='1", "'; DROP TABLE Cliente; --", "<img src=x onerror=alert(1)>"}) {
            Reply c = send("POST", "/clientes", app.adminToken, contact(probe));
            assertEquals(201, c.status, c.body); assertEquals(probe, c.json().path("nombre").asText());
            assertNull(dao.buscarPorCorreo(probe)); assertNull(dao.buscarPorTelefono(probe));
            ObjectNode d = ticket(); d.put("descripcionProblema", probe).put("comentariosCliente", probe);
            ((ObjectNode) d.at("/listaDispositivos/0")).put("modeloDispositivo", probe).put("detallesDispositivo", probe);
            ((ObjectNode) d.at("/listaTrabajos/0")).put("nombrePieza", probe);
            long id = createTicket(d);
            JsonNode stored = send("GET", "/resumenes/" + id, app.adminToken, null).json();
            for (String path : new String[]{"/descripcionProblema", "/comentariosCliente", "/listaDispositivos/0/modeloDispositivo", "/listaDispositivos/0/detallesDispositivo", "/listaTrabajos/0/nombrePieza"}) {
                assertEquals(probe, stored.at(path).asText(), path);
            }
            assertEquals(200, send("PUT", "/resumenes/" + id + "/estado", app.adminToken, Map.of("estado", "Entregado")).status);
            Reply review = send("PUT", "/resumenes/" + id + "/resena", app.userToken, Map.of("calificacion", 5, "resenaComentario", probe));
            assertEquals(200, review.status, review.body); assertEquals(probe, review.json().path("resenaComentario").asText());
            assertNotNull(dao.buscarPorId(app.admin.getId()));
        }
        assertEquals(401, send("POST", "/auth/login", null, Map.of("correo", app.admin.getCorreo(), "password", "' OR '1'='1")).status);
        assertEquals(401, send("POST", "/auth/login", null, Map.of("correo", "'or'1'='1@example.test", "password", "Audit-only-123!")).status);
    }
    @Test void roleAndOwnershipChecksBlockCrossUserRequests() throws Exception {
        long id = createTicket(ticket());
        assertEquals(403, send("GET", "/resumenes/" + id, app.otherToken, null).status);
        assertEquals(200, send("GET", "/resumenes/" + id, app.userToken, null).status);
        for (String method : new String[]{"POST", "PUT", "DELETE"}) {
            assertEquals(403, send(method, "/resumenes" + (method.equals("POST") ? "" : "/" + id), app.userToken, ticket()).status);
        }
        assertEquals(403, send("GET", "/clientes", app.userToken, null).status);
        assertEquals(403, send("POST", "/clientes", app.userToken, contact("Ataque")).status);
        assertEquals(403, send("PUT", "/resumenes/" + id + "/estado", app.userToken, Map.of("estado", "Entregado")).status);
        assertEquals(403, send("PUT", "/resumenes/" + id + "/resena", app.otherToken, Map.of("calificacion", 5, "resenaComentario", "Ataque")).status);
        assertEquals(400, send("POST", "/auth/register", null, Map.of("nombre", "Intruso", "telefono", "6441234567", "correo", "inject@example.test", "password", "Audit-only-123!", "rol", "ADMINISTRADOR")).status);
    }
    @Test void malformedAndDeepJsonAndKnownSizeAreBounded() throws Exception {
        assertEquals(400, raw("POST", "/auth/login", null, "{", false, "").status);
        assertEquals(400, raw("POST", "/auth/login", null, "[".repeat(25) + "0" + "]".repeat(25), false, "").status);
        String payload = "x".repeat(Util.AppConfig.maxRequestBytes() + 1);
        assertEquals(413, raw("POST", "/auth/logout", null, payload, false, "").status);
        assertEquals(403, raw("OPTIONS", "/clientes", null, "", false, "Origin: https://untrusted.example\r\n").status);
    }
    @Test void characterizeChunkedSizeBypass() throws Exception {
        String payload = " ".repeat(Util.AppConfig.maxRequestBytes()) + JsonUtil.MAPPER.writeValueAsString(contact("Chunked test"));
        assertEquals(413, raw("POST", "/clientes", app.adminToken, payload, false, "").status);
        Reply result = raw("POST", "/clientes", app.adminToken, payload, true, "");
        assertEquals(201, result.status, "Finding SEC-01 changed: review the report if fixed.");
        System.out.println("SEC-01 CONFIRMED: oversized chunked JSON parsed and accepted (201); same fixed-length body rejected (413).");
    }
    @Test void characterizeFakeImageAndCheckSvgAndOwnership() throws Exception {
        long id = createTicket(ticket());
        assertEquals(400, send("POST", "/resumenes/" + id + "/imagenes", app.adminToken,
            Map.of("dataBase64", "data:image/svg+xml;base64,PHN2Zz4=" )).status);
        Reply result = send("POST", "/resumenes/" + id + "/imagenes", app.adminToken,
            Map.of("dataBase64", "data:image/png;base64,bm90LWFuLWltYWdl", "descripcion", "'; DROP TABLE Imagen; --"));
        assertEquals(201, result.status, result.body);
        assertEquals("'; DROP TABLE Imagen; --", result.json().path("descripcion").asText());
        long imageId = result.json().path("id").asLong();
        long differentTicket = createTicket(ticket());
        assertEquals(404, send("DELETE", "/resumenes/" + differentTicket + "/imagenes/" + imageId, app.adminToken, null).status);
        assertEquals(403, send("DELETE", "/resumenes/" + id + "/imagenes/" + imageId, app.userToken, null).status);
        assertEquals(204, send("DELETE", "/resumenes/" + id + "/imagenes/" + imageId, app.adminToken, null).status);
        System.out.println("SEC-02 CONFIRMED: Base64 text accepted as PNG (201); SVG rejected (400).");
    }
    @Test void characterizeNullBodyAndMissingPassword() throws Exception {
        assertEquals(500, raw("POST", "/auth/login", null, "null", false, "").status);
        Reply response = send("POST", "/auth/register", null, Map.of("nombre", "Prueba", "telefono", "6441234567", "correo", "missing@example.test"));
        assertEquals(500, response.status); assertFalse(response.body.contains("Exception"));
        System.out.println("SEC-03 CONFIRMED: null login body and missing register password produce generic 500.");
    }
    @Test void loginLimiterBlocksRepeatedFailuresButAllowsRotatingAccounts() throws Exception {
        Reply result = null;
        for (int i = 0; i < 5; i++) result = send("POST", "/auth/login", null, Map.of("correo", "limited@example.test", "password", "wrong"));
        assertEquals(429, result.status); assertTrue(result.headers.contains("Retry-After"));
        for (int i = 0; i < 8; i++) assertEquals(401, send("POST", "/auth/login", null, Map.of("correo", "spray" + i + "@example.test", "password", "wrong")).status);
        System.out.println("SEC-04 CONFIRMED: per-account limit returns 429; rotating accounts remain 401 from same requester.");
    }
    @Test void characterizeDeletedAccountSession() throws Exception {
        Cliente c = new Cliente(); c.setNombre("Temporary admin"); c.setTelefono("6441112233");
        c.setRol(Cliente.ROL.ADMINISTRADOR); new ClienteDAO().insertar(c);
        String token = Service.TokenService.emitirToken(c);
        try {
            assertEquals(204, send("DELETE", "/clientes/" + c.getId(), app.adminToken, null).status);
            assertEquals(200, send("GET", "/clientes", token, null).status);
            System.out.println("SEC-05 CONFIRMED: deleted administrator's existing token still reads client directory (200).");
        } finally { Service.TokenService.invalidar(token); }
    }
    @Test void descriptionLimitAlreadySupportedByApi() throws Exception {
        ObjectNode d = ticket(); d.put("descripcionProblema", "x".repeat(2000));
        Reply result = send("POST", "/resumenes", app.adminToken, d);
        assertEquals(201, result.status);
        assertEquals(2000, result.json().path("descripcionProblema").asText().length());
        d.put("descripcionProblema", "x".repeat(2001));
        assertEquals(400, send("POST", "/resumenes", app.adminToken, d).status);
    }
}
