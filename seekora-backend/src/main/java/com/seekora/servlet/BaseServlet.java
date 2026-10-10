package com.seekora.servlet;

import com.seekora.dao.DaoException;
import com.seekora.service.ServiceException;
import com.seekora.util.ServletUtils;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Turns service / DAO failures into a consistent JSON error response so no
 * servlet ever leaks a stack trace to the browser.
 */
public abstract class BaseServlet extends HttpServlet {

    protected interface Action {
        void run() throws IOException;
    }

    protected void handle(HttpServletRequest request, HttpServletResponse response, Action action)
            throws IOException {
        try {
            action.run();
        } catch (ServiceException e) {
            if (e.getDiagnosticMessage() != null) {
                log("Service error on " + request.getRequestURI() + ": " + e.getDiagnosticMessage());
            }
            ServletUtils.error(response, e.getStatusCode(), e.getMessage());
        } catch (DaoException e) {
            log("Database error on " + request.getRequestURI(), e);
            ServletUtils.error(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Database error - please try again");
        } catch (RuntimeException e) {
            log("Unexpected error on " + request.getRequestURI(), e);
            ServletUtils.error(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Unexpected server error");
        }
    }

    /** Returns the trailing path segment, e.g. /api/history/12 -> "12". */
    protected String pathSegment(HttpServletRequest request) {
        String info = request.getPathInfo();
        if (info == null || info.length() <= 1) {
            return null;
        }
        String trimmed = info.startsWith("/") ? info.substring(1) : info;
        return trimmed.isEmpty() ? null : trimmed;
    }

    protected int intParam(HttpServletRequest request, String name, int fallback) {
        String value = request.getParameter(name);
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
