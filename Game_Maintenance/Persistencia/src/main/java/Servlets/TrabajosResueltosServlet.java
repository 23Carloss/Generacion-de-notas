package Servlets;

import Service.TrabajosResueltosService;
import Util.JsonUtil;
import java.io.IOException;
import java.util.Map;
import javax.servlet.http.*;

/** Read-only portfolio for authenticated users, with no access to private ticket details. */
public final class TrabajosResueltosServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        if (req.getPathInfo() != null && !"/".equals(req.getPathInfo())) {
            resp.setStatus(404);
            JsonUtil.MAPPER.writeValue(resp.getWriter(), Map.of("error", "ruta no soportada"));
            return;
        }
        try {
            int pagina = Integer.parseInt(req.getParameter("pagina") == null ? "1" : req.getParameter("pagina"));
            int tamano = Integer.parseInt(req.getParameter("tamano") == null ? "12" : req.getParameter("tamano"));
            JsonUtil.MAPPER.writeValue(resp.getWriter(), new TrabajosResueltosService().listar(pagina, tamano));
        } catch (IllegalArgumentException e) {
            resp.setStatus(400);
            JsonUtil.MAPPER.writeValue(resp.getWriter(), Map.of("error", "Paginación inválida."));
        }
    }
}
