package com.seekora.servlet;

import com.seekora.config.DatabaseConfig;
import com.seekora.util.ServletUtils;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

/** GET /api/health - verifies the servlet container and the MySQL connection. */
@WebServlet("/api/health")
public class HealthServlet extends BaseServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("application", "Seekora backend");
        try (Connection conn = DatabaseConfig.getConnection()) {
            status.put("database", conn.isValid(2) ? "UP" : "DOWN");
            status.put("catalog", conn.getCatalog());
        } catch (SQLException | RuntimeException e) {
            status.put("database", "DOWN");
            status.put("error", e.getMessage());
            ServletUtils.error(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Database unavailable");
            return;
        }
        ServletUtils.ok(response, status);
    }
}
