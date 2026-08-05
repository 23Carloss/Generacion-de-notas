package Servlets;

import DAOs.ClienteDAO;
import DTOs.ClienteDTO;
import Exceptions.PersistenciaException;
import persistencia.JsonUtil;
import Service.Mappers;
import hp.models.Cliente;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Expone /api/clientes
 *   GET    /api/clientes         -> lista de clientes
 *   POST   /api/clientes         -> crea cliente (body: {nombre, telefono})
 *   DELETE /api/clientes/{id}    -> elimina cliente
 */
public class ClienteServlet extends HttpServlet {

    private final ClienteDAO clienteDAO = new ClienteDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json;charset=UTF-8");
        try {
            List<Cliente> clientes = clienteDAO.listarTodos();
            List<ClienteDTO> dtos = clientes.stream().map(Mappers::toDTO).collect(Collectors.toList());
            JsonUtil.MAPPER.writeValue(resp.getWriter(), dtos);
        } catch (PersistenciaException e) {
            enviarError(resp, 500, e.getMessage());
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json;charset=UTF-8");
        try {
            ClienteDTO entrada = JsonUtil.MAPPER.readValue(req.getInputStream(), ClienteDTO.class);
            if (entrada.getNombre() == null || entrada.getNombre().isBlank()
                    || entrada.getTelefono() == null || entrada.getTelefono().isBlank()) {
                enviarError(resp, 400, "nombre y telefono son obligatorios");
                return;
            }
            Cliente cliente = new Cliente();
            cliente.setNombre(entrada.getNombre().trim());
            cliente.setTelefono(entrada.getTelefono().trim());
            clienteDAO.insertar(cliente);

            resp.setStatus(201);
            JsonUtil.MAPPER.writeValue(resp.getWriter(), Mappers.toDTO(cliente));
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
            clienteDAO.eliminar(id);
            resp.setStatus(204);
        } catch (PersistenciaException e) {
            // lo más común: el cliente tiene tickets asociados (FK)
            enviarError(resp, 409, e.getMessage());
        }
    }

    private Long idDesdePath(String pathInfo) {
        if (pathInfo == null) return null;
        String limpio = pathInfo.replaceAll("^/+", "").replaceAll("/+$", "");
        if (limpio.isEmpty()) return null;
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
