package com.seekora.servlet;

import com.google.gson.JsonObject;
import com.seekora.model.User;
import com.seekora.service.AuthService;
import com.seekora.service.ServiceException;
import com.seekora.util.JsonUtil;
import com.seekora.util.ServletUtils;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

/**
 * POST /api/auth/signup  {fullName, email, password}
 * POST /api/auth/login   {email, password}
 * POST /api/auth/logout
 * GET  /api/auth/me
 */
@WebServlet("/api/auth/*")
public class AuthServlet extends BaseServlet {

    private final AuthService authService = new AuthService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        handle(request, response, () -> {
            String action = pathSegment(request);
            JsonObject body = JsonUtil.readBody(request);
            if ("signup".equals(action)) {
                User user = authService.signup(JsonUtil.string(body, "fullName"),
                        JsonUtil.string(body, "email"),
                        JsonUtil.string(body, "password"),
                        ServletUtils.clientIp(request));
                startSession(request, user);
                ServletUtils.ok(response, "Account created successfully", user);
            } else if ("login".equals(action)) {
                User user = authService.login(JsonUtil.string(body, "email"),
                        JsonUtil.string(body, "password"),
                        ServletUtils.clientIp(request));
                startSession(request, user);
                ServletUtils.ok(response, "Welcome back, " + user.getFullName(), user);
            } else if ("logout".equals(action)) {
                HttpSession session = request.getSession(false);
                if (session != null) {
                    session.invalidate();
                }
                ServletUtils.ok(response, "Logged out", null);
            } else {
                throw new ServiceException("Unknown auth action", 404);
            }
        });
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        handle(request, response, () -> {
            if (!"me".equals(pathSegment(request))) {
                throw new ServiceException("Unknown auth action", 404);
            }
            Long userId = ServletUtils.currentUserId(request);
            if (userId == null) {
                ServletUtils.ok(response, "Not signed in", null);
                return;
            }
            ServletUtils.ok(response, authService.profile(userId));
        });
    }

    private void startSession(HttpServletRequest request, User user) {
        HttpSession session = request.getSession(true);
        request.changeSessionId();
        session.setAttribute(ServletUtils.SESSION_USER_ID, user.getId());
        session.setAttribute(ServletUtils.SESSION_USER_ROLE, user.getRole());
        session.setMaxInactiveInterval(60 * 60 * 8);
    }
}
