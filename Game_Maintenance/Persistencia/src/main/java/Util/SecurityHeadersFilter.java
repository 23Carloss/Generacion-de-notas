package Util;

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

/** Adds browser hardening headers to every API response. */
public class SecurityHeadersFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) {
        // No initialization required.
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletResponse resp = (HttpServletResponse) response;
        resp.setHeader("Content-Security-Policy",
                "default-src 'none'; base-uri 'none'; frame-ancestors 'none'; form-action 'none'");
        resp.setHeader("X-Content-Type-Options", "nosniff");
        resp.setHeader("Referrer-Policy", "no-referrer");
        resp.setHeader("Permissions-Policy", "camera=(), geolocation=(), microphone=()");
        resp.setHeader("X-Frame-Options", "DENY");
        resp.setHeader("Cache-Control", "no-store");
        if (AppConfig.hstsEnabled()) {
            resp.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
        }
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        // No resources to release.
    }
}
