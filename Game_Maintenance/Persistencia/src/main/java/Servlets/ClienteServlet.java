package Servlets;

import DAOs.ClienteDAO;
import DTOs.ClienteDTO;
import Exceptions.PersistenciaException;
import Util.JsonUtil;
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
 
        Cliente.ROL rol = (Cliente.ROL) req.getAttribute("rol");
        if (rol != Cliente.ROL.ADMINISTRADOR) {
            enviarError(resp, 403, "solo un administrador puede ver el listado de clientes");
            return;
        }
 
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
 
        Cliente.ROL rol = (Cliente.ROL) req.getAttribute("rol");
        if (rol != Cliente.ROL.ADMINISTRADOR) {
            enviarError(resp, 403, "solo un administrador puede crear clientes");
            return;
        }
 
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
            // Cliente creado por el admin desde "nuevo ticket": todavía sin
            // cuenta propia (sin correo/password), así que no puede iniciar
            // sesión hasta que alguien lo registre con ese mismo teléfono.
            cliente.setRol(Cliente.ROL.USUARIO);
            clienteDAO.insertar(cliente);
 
            resp.setStatus(201);
            JsonUtil.MAPPER.writeValue(resp.getWriter(), Mappers.toDTO(cliente));
        } catch (PersistenciaException e) {
            enviarError(resp, 500, e.getMessage());
        }
    }
 
    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json;charset=UTF-8");
 
        Long id = idDesdePath(req.getPathInfo());
        if (id == null) {
            enviarError(resp, 400, "id invalido");
            return;
        }
 
        Long callerId = (Long) req.getAttribute("clienteId");
        if (callerId == null || !callerId.equals(id)) {
            enviarError(resp, 403, "solo puedes editar tu propio perfil");
            return;
        }
 
        try {
            Cliente cliente = clienteDAO.buscarPorId(id);
            if (cliente == null) {
                enviarError(resp, 404, "cliente no encontrado");
                return;
            }
 
            Map<?, ?> body = JsonUtil.MAPPER.readValue(req.getInputStream(), Map.class);
            String nombre = texto(body.get("nombre"));
            String telefono = texto(body.get("telefono"));
            String correo = texto(body.get("correo"));
 
            if (nombre == null || nombre.isBlank()
                    || telefono == null || telefono.isBlank()
                    || correo == null || correo.isBlank()) {
                enviarError(resp, 400, "nombre, telefono y correo son obligatorios");
                return;
            }
 
            String correoNormalizado = correo.trim().toLowerCase();
            if (!correoNormalizado.equals(cliente.getCorreo())) {
                Cliente existente = clienteDAO.buscarPorCorreo(correoNormalizado);
                if (existente != null && !existente.getId().equals(id)) {
                    enviarError(resp, 400, "ese correo ya está en uso");
                    return;
                }
            }
 
            cliente.setNombre(nombre.trim());
            cliente.setTelefono(telefono.trim());
            cliente.setCorreo(correoNormalizado);
            clienteDAO.actualizar(cliente);
 
            JsonUtil.MAPPER.writeValue(resp.getWriter(), Mappers.toDTO(cliente));
        } catch (PersistenciaException e) {
            enviarError(resp, 500, e.getMessage());
        }
    }
 
    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Cliente.ROL rol = (Cliente.ROL) req.getAttribute("rol");
        if (rol != Cliente.ROL.ADMINISTRADOR) {
            enviarError(resp, 403, "solo un administrador puede eliminar clientes");
            return;
        }
 
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
 
    private String texto(Object valor) {
        return valor == null ? null : valor.toString();
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
