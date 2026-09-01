/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package Servlets;

import Exceptions.PersistenciaException;
import Exceptions.CredencialesInvalidasException;
import DTOs.LoginRequest;
import DTOs.RegisterRequest;
import Service.AuthService;
import Service.LoginRateLimiter;
import Service.RegistrationRateLimiter;
import Service.TokenService;
import Util.JsonUtil;
import java.io.IOException;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author $Luis Carlos Manjarrez Gonzalez
 */
public class AuthServlet extends HttpServlet{
    
    private final AuthService authService = new AuthService();
 
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json;charset=UTF-8");
 
        String ruta = req.getPathInfo() == null
                ? ""
                : req.getPathInfo().replaceAll("^/+", "").replaceAll("/+$", "");
 
        try {
            switch (ruta) {
                case "login":
                    manejarLogin(req, resp);
                    break;
                case "register":
                    manejarRegister(req, resp);
                    break;
                case "logout":
                    manejarLogout(req, resp);
                    break;
                default:
                    enviarError(resp, 404, "ruta no soportada");
            }
        } catch (IllegalArgumentException e) {
            enviarError(resp, 400, e.getMessage());
        } catch (PersistenciaException e) {
            enviarError(resp, 500, "Ocurrió un error interno.");
        }
    }
 
    private void manejarLogin(HttpServletRequest req, HttpServletResponse resp)
            throws IOException, PersistenciaException {
        LoginRequest body = JsonUtil.MAPPER.readValue(req.getInputStream(), LoginRequest.class);
        String limiterKey = req.getRemoteAddr() + "|" + normalizarCuenta(body.getCorreo());
        long retryAfter = LoginRateLimiter.retryAfterSeconds(limiterKey);
        if (retryAfter > 0) {
            responderLimitado(resp, retryAfter);
            return;
        }
        AuthService.LoginResult resultado;
        try {
            resultado = authService.login(body.getCorreo(), body.getPassword());
        } catch (CredencialesInvalidasException e) {
            long bloqueo = LoginRateLimiter.registerFailure(limiterKey);
            if (bloqueo > 0) {
                responderLimitado(resp, bloqueo);
            } else {
                enviarError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Credenciales incorrectas.");
            }
            return;
        }
        LoginRateLimiter.registerSuccess(limiterKey);
        JsonUtil.MAPPER.writeValue(resp.getWriter(), Map.of(
                "token", resultado.token,
                "cliente", resultado.cliente
        ));
    }
 
    private void manejarRegister(HttpServletRequest req, HttpServletResponse resp)
            throws IOException, PersistenciaException {
        long retryAfter = RegistrationRateLimiter.tryAcquire(req.getRemoteAddr());
        if (retryAfter > 0) {
            responderLimitado(resp, retryAfter);
            return;
        }
        RegisterRequest body = JsonUtil.MAPPER.readValue(req.getInputStream(), RegisterRequest.class);
        AuthService.LoginResult resultado = authService.registrar(
                body.getNombre(),
                body.getTelefono(),
                body.getCorreo(),
                body.getPassword());
        resp.setStatus(201);
        JsonUtil.MAPPER.writeValue(resp.getWriter(), Map.of(
                "token", resultado.token,
                "cliente", resultado.cliente
        ));
    }
 
    private void manejarLogout(HttpServletRequest req, HttpServletResponse resp) {
        String header = req.getHeader("Authorization");
        String token = (header != null && header.startsWith("Bearer ")) ? header.substring(7) : null;
        TokenService.invalidar(token);
        resp.setStatus(204);
    }
 
    private String normalizarCuenta(String correo) {
        return correo == null ? "" : correo.trim().toLowerCase();
    }

    private void responderLimitado(HttpServletResponse resp, long retryAfter) throws IOException {
        resp.setHeader("Retry-After", Long.toString(Math.max(1, retryAfter)));
        enviarError(resp, 429, "Demasiados intentos. Intenta de nuevo más tarde.");
    }
 
    private void enviarError(HttpServletResponse resp, int status, String mensaje) throws IOException {
        resp.setStatus(status);
        resp.setContentType("application/json;charset=UTF-8");
        JsonUtil.MAPPER.writeValue(resp.getWriter(), Map.of("error", mensaje));
    }

}
