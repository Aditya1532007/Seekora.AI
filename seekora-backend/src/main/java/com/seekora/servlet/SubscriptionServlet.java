package com.seekora.servlet;

import com.google.gson.JsonObject;
import com.seekora.service.ServiceException;
import com.seekora.service.SubscriptionService;
import com.seekora.util.JsonUtil;
import com.seekora.util.ServletUtils;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * GET  /api/subscriptions            - the signed-in user's subscription history
 * POST /api/subscriptions {planCode, billingCycle} - upgrade / downgrade
 */
@WebServlet("/api/subscriptions/*")
public class SubscriptionServlet extends BaseServlet {

    private final SubscriptionService subscriptionService = new SubscriptionService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        handle(request, response, () ->
                ServletUtils.ok(response, subscriptionService.history(requireUser(request))));
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        handle(request, response, () -> {
            long userId = requireUser(request);
            JsonObject body = JsonUtil.readBody(request);
            ServletUtils.ok(response, "Subscription updated",
                    subscriptionService.subscribe(userId, JsonUtil.string(body, "planCode"),
                            JsonUtil.string(body, "billingCycle"), ServletUtils.clientIp(request)));
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
