package Servlets;

import DTOs.NotificacionDTO;
import DTOs.PaginaNotificacionesDTO;
import Exceptions.PersistenciaException;
import Service.NotificacionService;
import Util.JsonUtil;
import java.io.IOException;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** API del centro de notificaciones; el destinatario siempre proviene de AuthFilter. */
public class NotificacionServlet extends HttpServlet {
    private static final int MAX_TAMANO = 100;
    private final NotificacionService service = new NotificacionService();

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if ("PATCH".equalsIgnoreCase(req.getMethod())) {
            doPatch(req, resp);
            return;
        }
        super.service(req, resp);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        Long destinatarioId = (Long) req.getAttribute("clienteId");
        String ruta = ruta(req);
        try {
            if ("unread-count".equals(ruta)) {
                JsonUtil.MAPPER.writeValue(resp.getWriter(), Map.of("count", service.contarNoLeidas(destinatarioId)));
                return;
            }
            if (!ruta.isEmpty()) {
                error(resp, 404, "ruta no soportada");
                return;
            }
            int pagina = entero(req.getParameter("page"), 0, "page");
            int tamano = entero(req.getParameter("size"), 20, "size");
            if (pagina < 0 || tamano < 1 || tamano > MAX_TAMANO
                    || pagina > Integer.MAX_VALUE / tamano) {
                throw new IllegalArgumentException("page debe ser mayor o igual a 0 y size debe estar entre 1 y 100");
            }
            Boolean noLeidas = booleano(req.getParameter("unread"));
            PaginaNotificacionesDTO resultado = service.listar(destinatarioId, noLeidas, pagina, tamano);
            JsonUtil.MAPPER.writeValue(resp.getWriter(), resultado);
        } catch (IllegalArgumentException e) {
            error(resp, 400, e.getMessage());
        } catch (PersistenciaException e) {
            error(resp, 500, "Ocurrió un error interno.");
        }
    }

    protected void doPatch(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        Long destinatarioId = (Long) req.getAttribute("clienteId");
        String ruta = ruta(req);
        try {
            if ("read-all".equals(ruta)) {
                int actualizadas = service.marcarTodasLeidas(destinatarioId);
                JsonUtil.MAPPER.writeValue(resp.getWriter(), Map.of("updated", actualizadas));
                return;
            }
            String[] partes = ruta.split("/");
            if (partes.length != 2 || !"read".equals(partes[1])) {
                error(resp, 404, "ruta no soportada");
                return;
            }
            Long id = Long.valueOf(partes[0]);
            NotificacionDTO actualizada = service.marcarLeida(id, destinatarioId);
            if (actualizada == null) error(resp, 404, "notificación no encontrada");
            else JsonUtil.MAPPER.writeValue(resp.getWriter(), actualizada);
        } catch (NumberFormatException e) {
            error(resp, 400, "id inválido");
        } catch (PersistenciaException e) {
            error(resp, 500, "Ocurrió un error interno.");
        }
    }

    private String ruta(HttpServletRequest req) {
        return req.getPathInfo() == null ? "" : req.getPathInfo().replaceAll("^/+", "").replaceAll("/+$", "");
    }

    private int entero(String valor, int porDefecto, String nombre) {
        if (valor == null || valor.isBlank()) return porDefecto;
        try { return Integer.parseInt(valor); }
        catch (NumberFormatException e) { throw new IllegalArgumentException(nombre + " debe ser un entero"); }
    }

    private Boolean booleano(String valor) {
        if (valor == null || valor.isBlank()) return null;
        if ("true".equalsIgnoreCase(valor)) return true;
        if ("false".equalsIgnoreCase(valor)) return false;
        throw new IllegalArgumentException("unread debe ser true o false");
    }

    private void error(HttpServletResponse resp, int status, String mensaje) throws IOException {
        resp.setStatus(status);
        JsonUtil.MAPPER.writeValue(resp.getWriter(), Map.of("error", mensaje));
    }
}
