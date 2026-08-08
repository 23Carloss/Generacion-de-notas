/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package Util;

import Service.TokenService;
import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author $Luis Carlos Manjarrez Gonzalez
 */

public class AuthFilter implements Filter {
 
    @Override
    public void init(FilterConfig filterConfig) {
        // sin inicialización necesaria
    }
 
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
 
        if ("OPTIONS".equalsIgnoreCase(req.getMethod())) {
            chain.doFilter(request, response);
            return;
        }
 
        String header = req.getHeader("Authorization");
        String token = (header != null && header.startsWith("Bearer "))
                ? header.substring("Bearer ".length())
                : null;
 
        TokenService.SesionInfo sesion = TokenService.validar(token);
        if (sesion == null) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            resp.setContentType("application/json;charset=UTF-8");
            resp.getWriter().write("{\"error\":\"no autenticado\"}");
            return;
        }
 
        req.setAttribute("clienteId", sesion.clienteId);
        req.setAttribute("rol", sesion.rol);
        chain.doFilter(request, response);
    }
 
    @Override
    public void destroy() {
        // sin recursos que liberar
    }
}
