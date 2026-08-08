/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package Servlets;

import Exceptions.PersistenciaException;
import Service.AuthService;
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
            enviarError(resp, 500, e.getMessage());
        }
    }
 
    private void manejarLogin(HttpServletRequest req, HttpServletResponse resp)
            throws IOException, PersistenciaException {
        Map<?, ?> body = JsonUtil.MAPPER.readValue(req.getInputStream(), Map.class);
        AuthService.LoginResult resultado = authService.login(texto(body.get("correo")), texto(body.get("password")));
        JsonUtil.MAPPER.writeValue(resp.getWriter(), Map.of(
                "token", resultado.token,
                "cliente", resultado.cliente
        ));
    }
 
    private void manejarRegister(HttpServletRequest req, HttpServletResponse resp)
            throws IOException, PersistenciaException {
        Map<?, ?> body = JsonUtil.MAPPER.readValue(req.getInputStream(), Map.class);
        AuthService.LoginResult resultado = authService.registrar(
                texto(body.get("nombre")),
                texto(body.get("telefono")),
                texto(body.get("correo")),
                texto(body.get("password")));
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
 
    private String texto(Object valor) {
        return valor == null ? null : valor.toString();
    }
 
    private void enviarError(HttpServletResponse resp, int status, String mensaje) throws IOException {
        resp.setStatus(status);
        resp.setContentType("application/json;charset=UTF-8");
        JsonUtil.MAPPER.writeValue(resp.getWriter(), Map.of("error", mensaje));
    }

}
