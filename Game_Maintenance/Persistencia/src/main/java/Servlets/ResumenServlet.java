package Servlets;

import DTOs.ImagenDTO;
import DTOs.ResumenDTO;
import Exceptions.AutorizacionException;
import Exceptions.PersistenciaException;
import Util.JsonUtil;
import Service.ResumenService;
import hp.models.Cliente;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Expone /api/resumenes
 *   GET    /api/resumenes               -> lista de tickets (más recientes primero)
 *                                          ADMINISTRADOR: todos · USUARIO: solo los suyos
 *   GET    /api/resumenes/{id}          -> detalle de un ticket (dueño o ADMINISTRADOR)
 *   POST   /api/resumenes               -> crea ticket (body: ResumenDTO)               (solo ADMINISTRADOR)
 *   POST   /api/resumenes/{id}/imagenes -> sube una foto (body: {dataBase64, descripcion, tipo}) (solo ADMINISTRADOR)
 *   PUT    /api/resumenes/{id}          -> actualiza los datos editables (body: ResumenDTO) (solo ADMINISTRADOR)
 *   PUT    /api/resumenes/{id}/estado   -> actualiza estado (body: {estado: "..."})     (solo ADMINISTRADOR)
 *   PUT    /api/resumenes/{id}/resena   -> guarda reseña/calificación (body: {calificacion, resenaComentario})
 *                                          (solo el cliente dueño, y solo si el ticket está Entregado)
 *   DELETE /api/resumenes/{id}          -> elimina ticket                               (solo ADMINISTRADOR)
 *   DELETE /api/resumenes/{id}/imagenes/{imagenId} -> elimina foto                       (solo ADMINISTRADOR)
 *
 * El rol y el id del cliente que hace la petición ya vienen resueltos por
 * AuthFilter en los atributos "rol" y "clienteId" del request (ver
 * ServerMain, que registra ese filtro sobre /api/resumenes/*).
 */
public class ResumenServlet extends HttpServlet {

    private final ResumenService service = new ResumenService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json;charset=UTF-8");

        Cliente.ROL rol = (Cliente.ROL) req.getAttribute("rol");
        Long callerId = (Long) req.getAttribute("clienteId");

        try {
            Long id = idDesdePath(req.getPathInfo());
            if (id == null) {
                List<ResumenDTO> lista = (rol == Cliente.ROL.ADMINISTRADOR)
                        ? service.listarResumenesDTO()
                        : service.listarResumenesDTOPorCliente(callerId);
                JsonUtil.MAPPER.writeValue(resp.getWriter(), lista);
                return;
            }

            ResumenDTO dto = service.obtenerResumenDTO(id);
            if (dto == null) {
                enviarError(resp, 404, "ticket no encontrado");
                return;
            }
            boolean esDueno = dto.getCliente() != null && dto.getCliente().getId() != null
                    && dto.getCliente().getId().equals(callerId);
            if (rol != Cliente.ROL.ADMINISTRADOR && !esDueno) {
                enviarError(resp, 403, "no tienes acceso a este ticket");
                return;
            }
            JsonUtil.MAPPER.writeValue(resp.getWriter(), dto);
        } catch (PersistenciaException e) {
            enviarError(resp, 500, "Ocurrió un error interno.");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json;charset=UTF-8");

        String pathInfo = req.getPathInfo();
        if (pathInfo == null) {
            crearTicket(req, resp);
        } else {
            subirImagen(req, resp, pathInfo);
        }
    }

    private void crearTicket(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Cliente.ROL rol = (Cliente.ROL) req.getAttribute("rol");
        if (rol != Cliente.ROL.ADMINISTRADOR) {
            enviarError(resp, 403, "solo un administrador puede crear tickets");
            return;
        }
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
            enviarError(resp, 500, "Ocurrió un error interno.");
        }
    }

    private void subirImagen(HttpServletRequest req, HttpServletResponse resp, String pathInfo) throws IOException {
        String limpio = pathInfo.replaceAll("^/+", "").replaceAll("/+$", "");
        String[] partes = limpio.split("/");
        if (partes.length != 2 || !"imagenes".equals(partes[1])) {
            enviarError(resp, 404, "ruta no soportada");
            return;
        }

        Cliente.ROL rol = (Cliente.ROL) req.getAttribute("rol");
        if (rol != Cliente.ROL.ADMINISTRADOR) {
            enviarError(resp, 403, "solo un administrador puede subir fotos");
            return;
        }

        Long idResumen;
        try {
            idResumen = Long.valueOf(partes[0]);
        } catch (Exception e) {
            enviarError(resp, 400, "id invalido");
            return;
        }

        try {
            ImagenDTO entrada = JsonUtil.MAPPER.readValue(req.getInputStream(), ImagenDTO.class);
            ImagenDTO creada = service.subirImagen(
                    idResumen, entrada.getDataBase64(), entrada.getDescripcion(), entrada.getTipo());
            if (creada == null) {
                enviarError(resp, 404, "ticket no encontrado");
            } else {
                resp.setStatus(201);
                JsonUtil.MAPPER.writeValue(resp.getWriter(), creada);
            }
        } catch (IllegalArgumentException e) {
            enviarError(resp, 400, e.getMessage());
        } catch (PersistenciaException e) {
            enviarError(resp, 500, "Ocurrió un error interno.");
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json;charset=UTF-8");

        String pathInfo = req.getPathInfo();
        if (pathInfo == null) {
            enviarError(resp, 404, "ruta no soportada");
            return;
        }
        String limpio = pathInfo.replaceAll("^/+", "").replaceAll("/+$", "");
        String[] partes = limpio.split("/");
        if (partes.length == 1) {
            Long id = idDesdePath(pathInfo);
            if (id == null) {
                enviarError(resp, 400, "id invalido");
                return;
            }
            try {
                actualizarTicket(req, resp, id);
            } catch (IllegalArgumentException e) {
                enviarError(resp, 400, e.getMessage());
            } catch (PersistenciaException e) {
                enviarError(resp, 500, "Ocurrió un error interno.");
            }
            return;
        }
        if (partes.length != 2) {
            enviarError(resp, 404, "ruta no soportada");
            return;
        }

        Long id;
        try {
            id = Long.valueOf(partes[0]);
        } catch (Exception e) {
            enviarError(resp, 400, "id invalido");
            return;
        }
        String subruta = partes[1];

        try {
            if ("estado".equals(subruta)) {
                actualizarEstado(req, resp, id);
            } else if ("resena".equals(subruta)) {
                actualizarResena(req, resp, id);
            } else if ("resena-publica".equals(subruta)) {
                publicarResena(req, resp, id);
            } else {
                enviarError(resp, 404, "ruta no soportada");
            }
        } catch (AutorizacionException e) {
            enviarError(resp, 403, e.getMessage());
        } catch (IllegalArgumentException e) {
            enviarError(resp, 400, e.getMessage());
        } catch (PersistenciaException e) {
            enviarError(resp, 500, "Ocurrió un error interno.");
        }
    }

    public record PublicacionResena(String texto, String resenaOriginal, boolean confirmado) {}

    private void publicarResena(HttpServletRequest req, HttpServletResponse resp, Long id) throws IOException {
        if (req.getAttribute("rol") != Cliente.ROL.ADMINISTRADOR) {
            enviarError(resp, 403, "solo un administrador puede publicar reseñas");
            return;
        }
        PublicacionResena body = JsonUtil.MAPPER.readValue(req.getInputStream(), PublicacionResena.class);
        if (body == null) throw new IllegalArgumentException("Faltan los datos de publicación.");
        boolean existe = new Service.TrabajosResueltosService().publicar(
                id, body.texto(), body.resenaOriginal(), body.confirmado());
        if (existe) resp.setStatus(204);
        else enviarError(resp, 404, "ticket no encontrado");
    }

    private void actualizarTicket(HttpServletRequest req, HttpServletResponse resp, Long id)
            throws IOException, PersistenciaException {
        Cliente.ROL rol = (Cliente.ROL) req.getAttribute("rol");
        if (rol != Cliente.ROL.ADMINISTRADOR) {
            enviarError(resp, 403, "solo un administrador puede editar tickets");
            return;
        }
        ResumenDTO entrada = JsonUtil.MAPPER.readValue(req.getInputStream(), ResumenDTO.class);
        ResumenDTO actualizado = service.actualizarResumenCompleto(id, entrada);
        if (actualizado == null) {
            enviarError(resp, 404, "ticket no encontrado");
        } else {
            JsonUtil.MAPPER.writeValue(resp.getWriter(), actualizado);
        }
    }

    private void actualizarEstado(HttpServletRequest req, HttpServletResponse resp, Long id)
            throws IOException, PersistenciaException {
        Cliente.ROL rol = (Cliente.ROL) req.getAttribute("rol");
        if (rol != Cliente.ROL.ADMINISTRADOR) {
            enviarError(resp, 403, "solo un administrador puede actualizar el estado del ticket");
            return;
        }
        Map<?, ?> body = JsonUtil.MAPPER.readValue(req.getInputStream(), Map.class);
        Object estado = body.get("estado");
        ResumenDTO actualizado = service.actualizarEstado(id, estado == null ? null : estado.toString());
        if (actualizado == null) {
            enviarError(resp, 404, "ticket no encontrado");
        } else {
            JsonUtil.MAPPER.writeValue(resp.getWriter(), actualizado);
        }
    }

    private void actualizarResena(HttpServletRequest req, HttpServletResponse resp, Long id)
            throws IOException, PersistenciaException, AutorizacionException {
        Long callerId = (Long) req.getAttribute("clienteId");
        Map<?, ?> body = JsonUtil.MAPPER.readValue(req.getInputStream(), Map.class);
        Object calificacionObj = body.get("calificacion");
        Integer calificacion = calificacionObj == null ? null : Integer.valueOf(calificacionObj.toString());
        Object comentarioObj = body.get("resenaComentario");
        String comentario = comentarioObj == null ? null : comentarioObj.toString();

        ResumenDTO actualizado = service.actualizarResena(id, callerId, comentario, calificacion);
        if (actualizado == null) {
            enviarError(resp, 404, "ticket no encontrado");
        } else {
            JsonUtil.MAPPER.writeValue(resp.getWriter(), actualizado);
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Cliente.ROL rol = (Cliente.ROL) req.getAttribute("rol");
        if (rol != Cliente.ROL.ADMINISTRADOR) {
            enviarError(resp, 403, "solo un administrador puede eliminar tickets");
            return;
        }

        String limpio = req.getPathInfo() == null ? "" : req.getPathInfo()
                .replaceAll("^/+", "").replaceAll("/+$", "");
        String[] partes = limpio.isEmpty() ? new String[0] : limpio.split("/");
        if (partes.length == 3 && "imagenes".equals(partes[1])) {
            try {
                Long idResumen = Long.valueOf(partes[0]);
                Long idImagen = Long.valueOf(partes[2]);
                boolean eliminada = service.eliminarImagen(idResumen, idImagen);
                resp.setStatus(eliminada ? 204 : 404);
            } catch (NumberFormatException e) {
                enviarError(resp, 400, "id invalido");
            } catch (PersistenciaException e) {
                enviarError(resp, 500, "Ocurrió un error interno.");
            }
            return;
        }

        Long id = idDesdePath(req.getPathInfo());
        if (id == null) {
            enviarError(resp, 400, "id invalido");
            return;
        }
        try {
            boolean eliminado = service.eliminarResumenCompleto(id);
            resp.setStatus(eliminado ? 204 : 404);
        } catch (PersistenciaException e) {
            enviarError(resp, 500, "Ocurrió un error interno.");
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
