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
 
        String origin = req.getHeader("Origin");
        boolean allowed = origin != null && AppConfig.allowedOrigins().contains(origin);
        if (allowed) {
            resp.setHeader("Access-Control-Allow-Origin", origin);
            resp.setHeader("Vary", "Origin");
            resp.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, PATCH, DELETE, OPTIONS");
            resp.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
            resp.setHeader("Access-Control-Max-Age", "600");
        }

        if ("OPTIONS".equalsIgnoreCase(req.getMethod())) {
            resp.setStatus(allowed ? HttpServletResponse.SC_NO_CONTENT : HttpServletResponse.SC_FORBIDDEN);
            return;
        }
 
        chain.doFilter(request, response);
    }
 
    @Override
    public void destroy() {
        // sin recursos que liberar
    }
}
