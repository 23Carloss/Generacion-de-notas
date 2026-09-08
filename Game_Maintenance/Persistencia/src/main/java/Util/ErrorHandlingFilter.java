package Util;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.IOException;
import java.util.Map;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

/** Prevents parser and internal exception details from reaching API clients. */
public class ErrorHandlingFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) {
        // No initialization required.
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletResponse resp = (HttpServletResponse) response;
        try {
            chain.doFilter(request, response);
        } catch (JsonProcessingException e) {
            writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "JSON inválido.");
        } catch (RuntimeException | ServletException e) {
            DatabaseDiagnostics.log("petición API", e);
            writeError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Ocurrió un error interno.");
        }
    }

    private void writeError(HttpServletResponse resp, int status, String message) throws IOException {
        if (resp.isCommitted()) {
            return;
        }
        resp.resetBuffer();
        resp.setStatus(status);
        resp.setContentType("application/json;charset=UTF-8");
        JsonUtil.MAPPER.writeValue(resp.getWriter(), Map.of("error", message));
    }

    @Override
    public void destroy() {
        // No resources to release.
    }
}
