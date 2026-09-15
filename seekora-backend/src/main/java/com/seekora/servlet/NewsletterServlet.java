package com.seekora.servlet;

import com.google.gson.JsonObject;
import com.seekora.service.ContentService;
import com.seekora.util.JsonUtil;
import com.seekora.util.ServletUtils;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * POST   /api/newsletter {email} - subscribe from the footer form
 * DELETE /api/newsletter?email=  - unsubscribe
 */
@WebServlet("/api/newsletter")
public class NewsletterServlet extends BaseServlet {

    private final ContentService contentService = new ContentService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        handle(request, response, () -> {
            JsonObject body = JsonUtil.readBody(request);
            contentService.subscribeNewsletter(JsonUtil.string(body, "email"));
            ServletUtils.ok(response, "Subscribed! Welcome to the newsletter.", null);
        });
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws IOException {
        handle(request, response, () -> {
            contentService.unsubscribeNewsletter(request.getParameter("email"));
            ServletUtils.ok(response, "Unsubscribed", null);
        });
    }
}
