package com.seekora.servlet;

import com.google.gson.JsonObject;
import com.seekora.service.ContentService;
import com.seekora.util.JsonUtil;
import com.seekora.util.ServletUtils;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/** POST /api/contact {name, email, subject, message} */
@WebServlet("/api/contact")
public class ContactServlet extends BaseServlet {

    private final ContentService contentService = new ContentService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        handle(request, response, () -> {
            JsonObject body = JsonUtil.readBody(request);
            long id = contentService.saveContactMessage(JsonUtil.string(body, "name"),
                    JsonUtil.string(body, "email"),
                    JsonUtil.string(body, "subject"),
                    JsonUtil.string(body, "message"));
            ServletUtils.ok(response, "Your message was saved successfully.", id);
        });
    }
}
