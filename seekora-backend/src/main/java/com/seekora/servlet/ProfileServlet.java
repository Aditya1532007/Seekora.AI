package com.seekora.servlet;

import com.google.gson.JsonObject;
import com.seekora.service.AuthService;
import com.seekora.service.SearchService;
import com.seekora.service.ServiceException;
import com.seekora.util.JsonUtil;
import com.seekora.util.ServletUtils;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * GET  /api/profile                 - the signed-in user + usage info
 * PUT  /api/profile                 - {fullName}
 * POST /api/profile/password        - {currentPassword, newPassword}
 */
@WebServlet("/api/profile/*")
public class ProfileServlet extends BaseServlet {

    private final AuthService authService = new AuthService();
    private final SearchService searchService = new SearchService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        handle(request, response, () -> {
            long userId = requireUser(request);
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("user", authService.profile(userId));
            payload.put("searchesRemainingToday", searchService.remainingToday(userId));
            ServletUtils.ok(response, payload);
        });
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException {
        handle(request, response, () -> {
            long userId = requireUser(request);
            JsonObject body = JsonUtil.readBody(request);
            ServletUtils.ok(response, "Profile updated",
                    authService.updateProfile(userId, JsonUtil.string(body, "fullName")));
        });
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        handle(request, response, () -> {
            long userId = requireUser(request);
            if (!"password".equals(pathSegment(request))) {
                throw new ServiceException("Unknown profile action", 404);
            }
            JsonObject body = JsonUtil.readBody(request);
            authService.changePassword(userId, JsonUtil.string(body, "currentPassword"),
                    JsonUtil.string(body, "newPassword"));
            ServletUtils.ok(response, "Password changed", null);
        });
    }

    private long requireUser(HttpServletRequest request) {
        Long userId = ServletUtils.currentUserId(request);
        if (userId == null) {
            throw new ServiceException("Please log in to continue", 401);
        }
        return userId;
    }
}
