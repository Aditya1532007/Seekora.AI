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

/** Guards the endpoints that require a signed-in user. */
@WebFilter(urlPatterns = {"/api/history/*", "/api/profile/*", "/api/subscriptions/*"})
public class AuthFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        if ("OPTIONS".equalsIgnoreCase(httpRequest.getMethod())) {
            chain.doFilter(request, response);
            return;
        }
        if (ServletUtils.currentUserId(httpRequest) == null) {
            ServletUtils.error(httpResponse, HttpServletResponse.SC_UNAUTHORIZED, "Please log in to continue");
            return;
        }
        chain.doFilter(request, response);
    }
}
