package com.seekora.util;

import com.seekora.model.ApiResponse;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;

public final class ServletUtils {

    public static final String SESSION_USER_ID = "userId";
    public static final String SESSION_USER_ROLE = "userRole";

    private ServletUtils() {
    }

    public static void writeJson(HttpServletResponse response, int status, Object payload) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        try (PrintWriter out = response.getWriter()) {
            out.write(JsonUtil.toJson(payload));
        }
    }

    public static void ok(HttpServletResponse response, Object data) throws IOException {
        writeJson(response, HttpServletResponse.SC_OK, ApiResponse.ok(data));
    }

    public static void ok(HttpServletResponse response, String message, Object data) throws IOException {
        writeJson(response, HttpServletResponse.SC_OK, ApiResponse.ok(message, data));
    }

    public static void error(HttpServletResponse response, int status, String message) throws IOException {
        writeJson(response, status, ApiResponse.error(message));
    }

    public static Long currentUserId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        Object value = session.getAttribute(SESSION_USER_ID);
        return value instanceof Long ? (Long) value : null;
    }

    public static String sessionKey(HttpServletRequest request) {
        return request.getSession(true).getId();
    }

    public static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isEmpty()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
