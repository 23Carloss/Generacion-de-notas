package Servlets;

import DTOs.ResumenDTO;
import Exceptions.PersistenciaException;
import Util.JsonUtil;
import Service.ResumenService;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Expone /api/resumenes
 *   GET    /api/resumenes             -> lista de tickets (más recientes primero)
 *   GET    /api/resumenes/{id}        -> detalle de un ticket
 *   POST   /api/resumenes             -> crea ticket (body: ResumenDTO)
 *   PUT    /api/resumenes/{id}/estado -> actualiza estado (body: {estado: "..."})
 *   DELETE /api/resumenes/{id}        -> elimina ticket
 */
public class ResumenServlet extends HttpServlet {

    private final ResumenService service = new ResumenService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json;charset=UTF-8");
        try {
            Long id = idDesdePath(req.getPathInfo());
            if (id == null) {
                List<ResumenDTO> lista = service.listarResumenesDTO();
                JsonUtil.MAPPER.writeValue(resp.getWriter(), lista);
            } else {
                ResumenDTO dto = service.obtenerResumenDTO(id);
                if (dto == null) {
                    enviarError(resp, 404, "ticket no encontrado");
                } else {
                    JsonUtil.MAPPER.writeValue(resp.getWriter(), dto);
                }
            }
        } catch (PersistenciaException e) {
            enviarError(resp, 500, e.getMessage());
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json;charset=UTF-8");
        try {
            ResumenDTO entrada = JsonUtil.MAPPER.readValue(req.getInputStream(), ResumenDTO.class);
            if (entrada.getCliente() == null || entrada.getCliente().getId() == null) {
                enviarError(resp, 400, "cliente.id es obligatorio");
                return;
            }
            if (entrada.getDescripcionProblema() == null || entrada.getDescripcionProblema().isBlank()) {
                enviarError(resp, 400, "descripcionProblema es obligatoria");
                return;
            }
            ResumenDTO creado = service.crearResumenCompleto(entrada);
            resp.setStatus(201);
            JsonUtil.MAPPER.writeValue(resp.getWriter(), creado);
        } catch (IllegalArgumentException e) {
            enviarError(resp, 400, e.getMessage());
        } catch (PersistenciaException e) {
            enviarError(resp, 500, e.getMessage());
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json;charset=UTF-8");

        String pathInfo = req.getPathInfo();
        if (pathInfo == null || !pathInfo.endsWith("/estado")) {
            enviarError(resp, 404, "ruta no soportada");
            return;
        }
        String limpio = pathInfo.replaceAll("^/+", "");
        String[] partes = limpio.split("/");
        Long id;
        try {
            id = Long.valueOf(partes[0]);
        } catch (Exception e) {
            enviarError(resp, 400, "id invalido");
            return;
        }

        try {
            Map<?, ?> body = JsonUtil.MAPPER.readValue(req.getInputStream(), Map.class);
            Object estado = body.get("estado");
            ResumenDTO actualizado = service.actualizarEstado(id, estado == null ? null : estado.toString());
            if (actualizado == null) {
                enviarError(resp, 404, "ticket no encontrado");
            } else {
                JsonUtil.MAPPER.writeValue(resp.getWriter(), actualizado);
            }
        } catch (IllegalArgumentException e) {
            enviarError(resp, 400, e.getMessage());
        } catch (PersistenciaException e) {
            enviarError(resp, 500, e.getMessage());
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Long id = idDesdePath(req.getPathInfo());
        if (id == null) {
            enviarError(resp, 400, "id invalido");
            return;
        }
        try {
            boolean eliminado = service.eliminarResumenCompleto(id);
            resp.setStatus(eliminado ? 204 : 404);
        } catch (PersistenciaException e) {
            enviarError(resp, 500, e.getMessage());
        }
    }

    private Long idDesdePath(String pathInfo) {
        if (pathInfo == null) return null;
        String limpio = pathInfo.replaceAll("^/+", "").replaceAll("/+$", "");
        if (limpio.isEmpty() || limpio.contains("/")) return null;
        try {
            return Long.valueOf(limpio);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void enviarError(HttpServletResponse resp, int status, String mensaje) throws IOException {
        resp.setStatus(status);
        resp.setContentType("application/json;charset=UTF-8");
        JsonUtil.MAPPER.writeValue(resp.getWriter(), Map.of("error", mensaje));
    }
}
