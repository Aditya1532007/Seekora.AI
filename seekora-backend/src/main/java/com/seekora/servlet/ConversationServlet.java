package com.seekora.servlet;

import com.seekora.service.AiConversationService;
import com.seekora.service.ServiceException;
import com.seekora.util.ServletUtils;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/** Lists and manages the signed-in user's saved AI conversations. */
@WebServlet("/api/conversations/*")
public class ConversationServlet extends BaseServlet {
    private final AiConversationService conversationService = new AiConversationService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        handle(request, response, () -> {
            long userId = requireUser(request);
            String segment = pathSegment(request);
            if (segment == null) {
                ServletUtils.ok(response, conversationService.listConversations(userId));
                return;
            }
            String[] parts = segment.split("/");
            if (parts.length != 2 || !"messages".equals(parts[1])) {
                throw new ServiceException("Unknown conversation route", 404);
            }
            ServletUtils.ok(response, conversationService.listMessages(parts[0], userId));
        });
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws IOException {
        handle(request, response, () -> {
            long userId = requireUser(request);
            String segment = pathSegment(request);
            if (segment == null || segment.contains("/")) {
                throw new ServiceException("A conversation ID is required.");
            }
            conversationService.deleteConversation(segment, userId);
            ServletUtils.ok(response, "Conversation deleted", null);
        });
    }

    private long requireUser(HttpServletRequest request) {
        Long userId = ServletUtils.currentUserId(request);
        if (userId == null) throw new ServiceException("Please sign in to continue.", 401);
        return userId;
    }
}
