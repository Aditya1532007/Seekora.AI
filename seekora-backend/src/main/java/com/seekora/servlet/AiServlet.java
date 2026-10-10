package com.seekora.servlet;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.seekora.service.AiConversationService;
import com.seekora.service.ServiceException;
import com.seekora.util.JsonUtil;
import com.seekora.util.ServletUtils;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** POST /api/ai {query, mode, imageData?, conversationId?, history?, save?} */
@WebServlet("/api/ai")
public class AiServlet extends BaseServlet {
    private final AiConversationService aiService = new AiConversationService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        handle(request, response, () -> {
            JsonObject body = JsonUtil.readBody(request);
            String query = JsonUtil.string(body, "query");
            String mode = JsonUtil.string(body, "mode");
            String imageData = JsonUtil.string(body, "imageData");
            String conversationId = JsonUtil.string(body, "conversationId");
            boolean save = true;
            if (body.has("save") && !body.get("save").isJsonNull()) {
                if (!body.get("save").isJsonPrimitive()
                        || !body.getAsJsonPrimitive("save").isBoolean()) {
                    throw new ServiceException("The save option must be a boolean.");
                }
                save = body.getAsJsonPrimitive("save").getAsBoolean();
            }
            ServletUtils.ok(response, aiService.answer(query, mode, imageData, conversationId,
                    historyContext(body), ServletUtils.currentUserId(request), save));
        });
    }

    private String historyContext(JsonObject body) {
        if (!body.has("history") || !body.get("history").isJsonArray()) return "";
        JsonArray history = body.getAsJsonArray("history");
        List<String> lines = new ArrayList<>();
        int start = Math.max(0, history.size() - 12);
        for (int i = start; i < history.size(); i++) {
            JsonElement item = history.get(i);
            if (!item.isJsonObject()) continue;
            JsonObject message = item.getAsJsonObject();
            String role = JsonUtil.string(message, "role");
            String content = JsonUtil.string(message, "content");
            if (content == null || content.trim().isEmpty()) continue;
            String speaker = "user".equals(role) ? "User" : "assistant".equals(role) ? "Assistant" : null;
            if (speaker != null) {
                lines.add(speaker + ": " + content.substring(0, Math.min(content.length(), 4000)));
            }
        }
        return String.join("\n", lines);
    }
}
