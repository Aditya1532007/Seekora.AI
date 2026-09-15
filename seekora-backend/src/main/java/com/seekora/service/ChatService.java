package com.seekora.service;

import com.seekora.dao.ChatDao;
import com.seekora.model.ChatMessage;
import com.seekora.util.Validator;

import java.util.List;
import java.util.Map;

/** Keyword driven assistant backed by the chat_intents table. */
public class ChatService {

    private static final String DEFAULT_REPLY =
            "I can help with features, pricing, the developer API or your account. Try asking \"what is premium?\".";

    private final ChatDao chatDao = new ChatDao();

    public ChatMessage reply(String message, String sessionKey, Long userId) {
        if (Validator.isBlank(message)) {
            throw new ServiceException("Message cannot be empty");
        }
        long chatSessionId = chatDao.findOrCreateSession(sessionKey, userId);
        chatDao.saveMessage(chatSessionId, "USER", Validator.truncate(message.trim(), 1000));

        String reply = resolveReply(message.toLowerCase());
        long id = chatDao.saveMessage(chatSessionId, "BOT", reply);

        ChatMessage response = new ChatMessage();
        response.setId(id);
        response.setChatSessionId(chatSessionId);
        response.setSender("BOT");
        response.setMessage(reply);
        return response;
    }

    public List<ChatMessage> history(String sessionKey, Long userId) {
        long chatSessionId = chatDao.findOrCreateSession(sessionKey, userId);
        return chatDao.findMessages(chatSessionId, 100);
    }

    private String resolveReply(String normalizedMessage) {
        Map<String, String> intents = chatDao.findIntents();
        for (Map.Entry<String, String> intent : intents.entrySet()) {
            for (String keyword : intent.getKey().toLowerCase().split(",")) {
                String key = keyword.trim();
                if (!key.isEmpty() && normalizedMessage.contains(key)) {
                    return intent.getValue();
                }
            }
        }
        return DEFAULT_REPLY;
    }
}
