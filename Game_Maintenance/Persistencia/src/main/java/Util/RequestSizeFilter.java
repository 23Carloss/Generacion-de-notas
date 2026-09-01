package Util;

import java.io.IOException;
import java.util.Map;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Rejects declared request bodies that exceed the configured API limit. */
public class RequestSizeFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) {
        // No initialization required.
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        if (req.getContentLengthLong() > AppConfig.maxRequestBytes()) {
            resp.setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
            resp.setContentType("application/json;charset=UTF-8");
            JsonUtil.MAPPER.writeValue(resp.getWriter(), Map.of("error", "El cuerpo de la petición excede el tamaño permitido."));
            return;
        }
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        // No resources to release.
    }
}
