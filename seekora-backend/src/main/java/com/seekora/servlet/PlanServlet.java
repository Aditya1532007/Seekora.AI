package com.seekora.servlet;

import com.seekora.service.SubscriptionService;
import com.seekora.util.ServletUtils;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/** GET /api/plans - pricing cards with their feature lists. */
@WebServlet("/api/plans")
public class PlanServlet extends BaseServlet {

    private final SubscriptionService subscriptionService = new SubscriptionService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        handle(request, response, () -> ServletUtils.ok(response, subscriptionService.plans()));
    }
}
