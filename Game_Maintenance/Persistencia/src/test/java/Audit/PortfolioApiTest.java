package Audit;

import Util.JsonUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class PortfolioApiTest {
    static AuditFixture app;
    record Reply(int status, String body) {
        JsonNode json() throws Exception { return JsonUtil.MAPPER.readTree(body); }
    }
    @BeforeAll static void start() throws Exception { app = new AuditFixture(); app.server.start(); }
    @AfterAll static void stop() throws Exception { if (app != null) app.close(); }
    static Reply send(String method, String path, String token, Object body) throws Exception {
        // LocalConnector's String helper uses ISO-8859-1; escaped JSON preserves Unicode and framing.
        String data = body == null ? "" : JsonUtil.MAPPER.writer()
                .with(com.fasterxml.jackson.core.json.JsonWriteFeature.ESCAPE_NON_ASCII).writeValueAsString(body);
        String response = app.connector.getResponse(method + " /GameMaintenance/api" + path
                + " HTTP/1.1\r\nHost: localhost\r\nConnection: close\r\nContent-Type: application/json\r\n"
                + (token == null ? "" : "Authorization: Bearer " + token + "\r\n")
                + "Content-Length: " + data.getBytes(StandardCharsets.UTF_8).length + "\r\n\r\n" + data);
        return new Reply(Integer.parseInt(response.split(" ")[1]), response.substring(response.indexOf("\r\n\r\n") + 4));
    }
    static ObjectNode ticket() throws Exception {
        return (ObjectNode) JsonUtil.MAPPER.readTree("""
            {"cliente":{"id":%d},"descripcionProblema":"SECRETO direccion personal",
             "comentariosCliente":"SECRETO correo y telefono",
             "listaDispositivos":[{"modeloDispositivo":"SECRETO numero serie","detallesDispositivo":"SECRETO domicilio","plataforma":"PLAYSTATION"}],
             "listaTrabajos":[{"tipoTrabajo":"REPARACION","nombrePieza":"SECRETO pieza privada","unidades":1,"precioUnitario":789}]}
            """.formatted(app.user.getId()));
    }
    static long create() throws Exception {
        Reply r = send("POST", "/resumenes", app.adminToken, ticket());
        assertEquals(201, r.status, r.body); return r.json().path("id").asLong();
    }
    static void state(long id, String state) throws Exception {
        Reply r = send("PUT", "/resumenes/" + id + "/estado", app.adminToken, Map.of("estado", state));
        assertEquals(200, r.status, r.body);
    }
    static void review(long id, String text) throws Exception {
        Reply r = send("PUT", "/resumenes/" + id + "/resena", app.userToken, Map.of("resenaComentario", text, "calificacion", 4));
        assertEquals(200, r.status, r.body);
    }
    static Reply publish(long id, String text, String original, boolean confirmed) throws Exception {
        return send("PUT", "/resumenes/" + id + "/resena-publica", app.adminToken,
                Map.of("texto", text, "resenaOriginal", original, "confirmado", confirmed));
    }
    static JsonNode gallery() throws Exception {
        Reply r = send("GET", "/trabajos-resueltos?tamano=20", app.otherToken, null);
        assertEquals(200, r.status, r.body); return r.json();
    }
    static Set<String> fields(JsonNode n) { Set<String> keys = new TreeSet<>(); n.fieldNames().forEachRemaining(keys::add); return keys; }

    @Test void missingPortfolioColumnBreaksBothReadsAndAdditiveMigrationRestoresExistingTickets() throws Exception {
        long id = create(); state(id, "Entregado");
        // Only the disposable AuditPU database; never reads DB_URL or touches real records.
        try (var connection = java.sql.DriverManager.getConnection("jdbc:h2:mem:gm_audit;MODE=LEGACY", "sa", "");
                var sql = connection.createStatement()) {
            sql.execute("ALTER TABLE Resumen DROP COLUMN resenaPublica");
            try {
                assertEquals(500, send("GET", "/resumenes", app.adminToken, null).status);
                assertEquals(500, send("GET", "/trabajos-resueltos", app.userToken, null).status);
            } finally {
                // The shipped PostgreSQL migration is also accepted by H2 for this regression check.
                String migration = java.nio.file.Files.readString(java.nio.file.Path.of("../Deployment/portfolio-postgresql.sql"));
                for (int run = 0; run < 2; run++) {
                    for (String statement : migration.split(";")) if (!statement.isBlank()) sql.execute(statement);
                }
            }
            assertEquals(200, send("GET", "/resumenes", app.adminToken, null).status);
            assertEquals(200, send("GET", "/trabajos-resueltos", app.userToken, null).status);
            Reply restored = send("GET", "/resumenes/" + id, app.adminToken, null);
            assertEquals(200, restored.status);
            assertEquals("SECRETO direccion personal", restored.json().path("descripcionProblema").asText());
        }
    }

    @Test void authenticationAndPrivateTicketPermissionsRemainSeparate() throws Exception {
        assertEquals(401, send("GET", "/trabajos-resueltos", null, null).status);
        assertEquals(401, send("GET", "/trabajos-resueltos", "invalid", null).status);
        long id = create(); state(id, "Entregado");
        assertEquals(200, send("GET", "/trabajos-resueltos", app.userToken, null).status);
        assertEquals(403, send("GET", "/resumenes/" + id, app.otherToken, null).status);
        assertEquals(403, send("PUT", "/resumenes/" + id + "/resena-publica", app.userToken,
                Map.of("texto", "Buen trabajo", "confirmado", true)).status);
        assertEquals(404, send("GET", "/trabajos-resueltos/" + id, app.otherToken, null).status);
    }

    @Test void onlyDeliveredTicketsAppearAndNoPrivateFieldsAreSerialized() throws Exception {
        long before = gallery().path("total").asLong();
        long id = create(); state(id, "Listo para entrega");
        assertEquals(before, gallery().path("total").asLong());
        state(id, "Entregado");
        review(id, "SECRETO Usuario prueba audit-user@example.test 6441234567");
        JsonNode g = gallery(); assertEquals(before + 1, g.path("total").asLong());
        assertEquals(Set.of("trabajos", "total", "pagina", "tamano", "valoraciones", "promedio"), fields(g));
        for (JsonNode item : g.path("trabajos"))
            assertEquals(Set.of("plataformas", "servicios", "calificacion", "resena"), fields(item));
        assertFalse(g.toString().contains("SECRETO")); assertFalse(g.toString().contains(app.user.getCorreo()));
        JsonNode item = g.path("trabajos").get(0);
        assertTrue(item.path("resena").isNull()); assertEquals(4, item.path("calificacion").asInt());
        assertEquals("PLAYSTATION", item.path("plataformas").get(0).asText());
        assertEquals("REPARACION", item.path("servicios").get(0).asText());
    }

    @Test void publicationRequiresLiteralReviewedExcerptAndRejectsKnownPersonalData() throws Exception {
        long id = create();
        assertEquals(400, publish(id, "Bien", "Bien", true).status);
        state(id, "Entregado");
        String original = "Buen servicio. Usuario prueba. audit-user@example.test. 6441234567. https://example.test. <script>alert(1)</script>";
        review(id, original);
        assertEquals(400, publish(id, "Buen servicio.", original, false).status);
        assertEquals(400, publish(id, "Inventado", original, true).status);
        assertEquals(400, publish(id, "Buen servicio.", "Versión antigua", true).status);
        for (String unsafe : List.of("Usuario prueba", "audit-user@example.test", "6441234567", "https://example.test", "<script>alert(1)</script>"))
            assertEquals(400, publish(id, unsafe, original, true).status, unsafe);
        assertEquals(204, publish(id, "Buen servicio.", original, true).status);
        assertEquals("Buen servicio.", gallery().path("trabajos").get(0).path("resena").asText());
        assertFalse(gallery().toString().contains("audit-user"));
        assertEquals(204, send("PUT", "/resumenes/" + id + "/resena-publica", app.adminToken, Collections.singletonMap("texto", null)).status);
        assertTrue(gallery().path("trabajos").get(0).path("resena").isNull());
    }

    @Test void editingOrReopeningRevokesPublicationAndReassignmentClearsOldOwnersReview() throws Exception {
        long id = create(); state(id, "Entregado"); String text = "Excelente atención.";
        review(id, text); assertEquals(204, publish(id, text, text, true).status);
        review(id, text + " Mi teléfono es privado.");
        assertTrue(gallery().path("trabajos").get(0).path("resena").isNull());
        review(id, text); assertEquals(204, publish(id, text, text, true).status);
        state(id, "En reparación"); state(id, "Entregado");
        assertTrue(gallery().path("trabajos").get(0).path("resena").isNull());
        assertEquals(204, publish(id, text, text, true).status);
        assertEquals(200, send("PUT", "/resumenes/" + id, app.adminToken, ticket()).status);
        assertTrue(gallery().path("trabajos").get(0).path("resena").isNull());
        ObjectNode reassigned = ticket(); ((ObjectNode) reassigned.path("cliente")).put("id", app.other.getId());
        Reply r = send("PUT", "/resumenes/" + id, app.adminToken, reassigned);
        assertEquals(200, r.status, r.body);
        assertTrue(r.json().path("resenaComentario").isNull()); assertTrue(r.json().path("calificacion").isNull());
    }

    @Test void paginationIsBoundedAndHandlesEmptyPages() throws Exception {
        for (String query : List.of("pagina=0", "pagina=-1", "pagina=2147483647", "pagina=abc", "tamano=0", "tamano=21", "pagina=1%20OR%201=1"))
            assertEquals(400, send("GET", "/trabajos-resueltos?" + query, app.userToken, null).status);
        for (int i = 0; i < 3; i++) { long id = create(); state(id, "Entregado"); }
        JsonNode page = send("GET", "/trabajos-resueltos?pagina=2&tamano=2", app.otherToken, null).json();
        assertEquals(2, page.path("pagina").asInt()); assertTrue(page.path("trabajos").size() <= 2);
        JsonNode empty = send("GET", "/trabajos-resueltos?pagina=100000", app.userToken, null).json();
        assertEquals(0, empty.path("trabajos").size());
    }
}
