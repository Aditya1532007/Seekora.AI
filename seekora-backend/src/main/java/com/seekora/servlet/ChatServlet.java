package com.seekora.servlet;

import com.google.gson.JsonObject;
import com.seekora.service.ChatService;
import com.seekora.util.JsonUtil;
import com.seekora.util.ServletUtils;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * POST /api/chat  {message} - talk to the assistant widget
 * GET  /api/chat            - the conversation of the current browser session
 */
@WebServlet("/api/chat")
public class ChatServlet extends BaseServlet {

    private final ChatService chatService = new ChatService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        handle(request, response, () -> {
            JsonObject body = JsonUtil.readBody(request);
            ServletUtils.ok(response, chatService.reply(JsonUtil.string(body, "message"),
                    ServletUtils.sessionKey(request), ServletUtils.currentUserId(request)));
        });
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        handle(request, response, () -> ServletUtils.ok(response,
                chatService.history(ServletUtils.sessionKey(request), ServletUtils.currentUserId(request))));
    }
}
