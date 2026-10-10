package com.seekora.filter;

import com.seekora.util.ServletUtils;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Allows explicitly configured frontend origins to call the API. Same-origin
 * requests need no CORS headers.
 */
@WebFilter(urlPatterns = "/api/*")
public class CorsFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String origin = httpRequest.getHeader("Origin");
        if (origin != null) {
            Set<String> allowedOrigins = allowedOrigins();
            if (!allowedOrigins.contains(origin) && !isSameOrigin(httpRequest, origin)) {
                ServletUtils.error(httpResponse, HttpServletResponse.SC_FORBIDDEN, "Origin is not allowed");
                return;
            }
            httpResponse.setHeader("Access-Control-Allow-Origin", origin);
            httpResponse.setHeader("Access-Control-Allow-Credentials", "true");
            httpResponse.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            httpResponse.setHeader("Access-Control-Allow-Headers", "Content-Type, Accept, X-Requested-With");
            httpResponse.setHeader("Vary", "Origin");
        }

        if ("OPTIONS".equalsIgnoreCase(httpRequest.getMethod())) {
            httpResponse.setStatus(HttpServletResponse.SC_NO_CONTENT);
            return;
        }
        chain.doFilter(request, response);
    }

    private Set<String> allowedOrigins() {
        String configured = System.getenv("SEEKORA_ALLOWED_ORIGINS");
        if (configured == null || configured.trim().isEmpty()) {
            return Collections.emptySet();
        }
        return Arrays.stream(configured.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .collect(Collectors.toSet());
    }

    private boolean isSameOrigin(HttpServletRequest request, String origin) {
        try {
            URI parsed = new URI(origin);
            if (parsed.getHost() == null || parsed.getUserInfo() != null
                    || (parsed.getPath() != null && !parsed.getPath().isEmpty())
                    || parsed.getQuery() != null || parsed.getFragment() != null) {
                return false;
            }
            int originPort = parsed.getPort() >= 0 ? parsed.getPort()
                    : ("https".equalsIgnoreCase(parsed.getScheme()) ? 443 : 80);
            int requestPort = request.getServerPort();
            return parsed.getScheme() != null
                    && parsed.getScheme().equalsIgnoreCase(request.getScheme())
                    && parsed.getHost().equalsIgnoreCase(request.getServerName())
                    && originPort == requestPort;
        } catch (URISyntaxException e) {
            return false;
        }
    }
}
