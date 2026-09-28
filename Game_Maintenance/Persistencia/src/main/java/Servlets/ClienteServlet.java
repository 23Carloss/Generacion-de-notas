package Servlets;

import DAOs.ClienteDAO;
import DTOs.ClienteDTO;
import Exceptions.PersistenciaException;
import Util.JsonUtil;
import Service.Mappers;
import Service.LoginRateLimiter;
import Service.TokenService;
import hp.models.Cliente;
import Util.PasswordUtil;
import Util.ValidationUtil;

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
 *   PUT    /api/clientes/{id}    -> edita contacto (administrador o propio perfil)
 *   DELETE /api/clientes/{id}    -> elimina cliente; si es administrador requiere su contraseña
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
            enviarError(resp, 500, "Ocurrió un error interno.");
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
            Cliente cliente = new Cliente();
            cliente.setNombre(ValidationUtil.requiredText(entrada.getNombre(), "nombre", 100));
            cliente.setTelefono(ValidationUtil.phone(entrada.getTelefono()));
            // Cliente creado por el admin desde "nuevo ticket": todavía sin
            // cuenta propia (sin correo/password), así que no puede iniciar
            // sesión hasta que alguien lo registre con ese mismo teléfono.
            cliente.setRol(Cliente.ROL.USUARIO);
            clienteDAO.insertar(cliente);
 
            resp.setStatus(201);
            JsonUtil.MAPPER.writeValue(resp.getWriter(), Mappers.toDTO(cliente));
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
 
        Long id = idDesdePath(req.getPathInfo());
        if (id == null) {
            enviarError(resp, 400, "id invalido");
            return;
        }
 
        Long callerId = (Long) req.getAttribute("clienteId");
        boolean esAdmin = req.getAttribute("rol") == Cliente.ROL.ADMINISTRADOR;
        if (callerId == null || (!esAdmin && !callerId.equals(id))) {
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
 
            String nombreSeguro = ValidationUtil.requiredText(nombre, "nombre", 100);
            String telefonoSeguro = ValidationUtil.phone(telefono);
            // Omitir correo conserva el actual. Los contactos sin cuenta pueden
            // dejarlo vacío; una cuenta existente debe conservar un correo válido.
            String correoNormalizado = cliente.getCorreo();
            if (body.containsKey("correo")) {
                boolean sinCorreo = correo == null || correo.isBlank();
                boolean tieneCuenta = cliente.getPasswordHash() != null && !cliente.getPasswordHash().isBlank();
                correoNormalizado = sinCorreo && esAdmin && !tieneCuenta ? null : ValidationUtil.email(correo);
            }
            if (correoNormalizado != null && !correoNormalizado.equals(cliente.getCorreo())) {
                Cliente existente = clienteDAO.buscarPorCorreo(correoNormalizado);
                if (existente != null && !existente.getId().equals(id)) {
                    enviarError(resp, 400, "ese correo ya está en uso");
                    return;
                }
            }
 
            cliente.setNombre(nombreSeguro);
            cliente.setTelefono(telefonoSeguro);
            cliente.setCorreo(correoNormalizado);
            clienteDAO.actualizar(cliente);
 
            JsonUtil.MAPPER.writeValue(resp.getWriter(), Mappers.toDTO(cliente));
        } catch (IllegalArgumentException e) {
            enviarError(resp, 400, e.getMessage());
        } catch (PersistenciaException e) {
            enviarError(resp, 500, "Ocurrió un error interno.");
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
            Cliente cliente = clienteDAO.buscarPorId(id);
            if (cliente == null) {
                enviarError(resp, 404, "cliente no encontrado");
                return;
            }

            if (cliente.getRol() == Cliente.ROL.ADMINISTRADOR) {
                String limiterKey = "delete-admin|" + req.getRemoteAddr() + "|" + id;
                long retryAfter = LoginRateLimiter.retryAfterSeconds(limiterKey);
                if (retryAfter > 0) {
                    resp.setHeader("Retry-After", Long.toString(retryAfter));
                    enviarError(resp, 429, "Demasiados intentos. Intenta de nuevo más tarde.");
                    return;
                }
                String password = passwordEliminacion(req);
                boolean passwordValido = PasswordUtil.verificar(password, cliente.getPasswordHash())
                        || PasswordUtil.verificarLegado(password, cliente.getPasswordSalt(), cliente.getPasswordHash());
                if (!passwordValido) {
                    long bloqueo = LoginRateLimiter.registerFailure(limiterKey);
                    if (bloqueo > 0) {
                        resp.setHeader("Retry-After", Long.toString(bloqueo));
                        enviarError(resp, 429, "Demasiados intentos. Intenta de nuevo más tarde.");
                    } else {
                        enviarError(resp, 403, "La contraseña del administrador a eliminar es incorrecta.");
                    }
                    return;
                }
                LoginRateLimiter.registerSuccess(limiterKey);
            }

            clienteDAO.eliminar(id);
            TokenService.invalidarCliente(id);
            resp.setStatus(204);
        } catch (PersistenciaException e) {
            // lo más común: el cliente tiene tickets asociados (FK)
            enviarError(resp, 409, "No se puede eliminar el cliente porque tiene datos relacionados.");
        }
    }

    private String passwordEliminacion(HttpServletRequest req) throws IOException {
        byte[] contenido = req.getInputStream().readAllBytes();
        if (contenido.length == 0) return null;
        Map<?, ?> body = JsonUtil.MAPPER.readValue(contenido, Map.class);
        return body == null ? null : texto(body.get("password"));
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
