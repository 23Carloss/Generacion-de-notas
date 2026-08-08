package Util;

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
 * Habilita CORS para que el frontend (Vite, típicamente http://localhost:5173)
 * pueda llamar a esta API aunque corran en puertos distintos. También resuelve
 * las peticiones OPTIONS de preflight que dispara el navegador porque api.js
 * manda "Content-Type: application/json" en cada request.
 */
public class CorsFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) {
        // sin inicialización necesaria
    }
 
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
 
        resp.setHeader("Access-Control-Allow-Origin", "*");
        resp.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        // Se agrega "Authorization" para el token de sesión (Etapa 5); antes
        // solo se permitía "Content-Type".
        resp.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
 
        if ("OPTIONS".equalsIgnoreCase(req.getMethod())) {
            resp.setStatus(HttpServletResponse.SC_OK);
            return;
        }
 
        chain.doFilter(request, response);
    }
 
    @Override
    public void destroy() {
        // sin recursos que liberar
    }
}