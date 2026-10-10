package com.seekora.service;

import com.seekora.dao.ChatDao;
import com.seekora.model.ChatMessage;
import com.seekora.util.Validator;

import java.util.List;
import java.util.stream.Collectors;

/** General AI chat with user and assistant messages persisted in chat_messages. */
public class ChatService {

    private static final int DAILY_CHAT_LIMIT = 20;

    private final ChatDao chatDao = new ChatDao();
    private final GeminiAnswerService aiAnswerService = new GeminiAnswerService();

    public ChatMessage reply(String message, String sessionKey, Long userId) {
        if (Validator.isBlank(message)) {
            throw new ServiceException("Message cannot be empty");
        }
        long chatSessionId = chatDao.findOrCreateSession(sessionKey, userId);
        if (chatDao.countBotRepliesToday(chatSessionId) >= DAILY_CHAT_LIMIT) {
            throw new ServiceException("Today's AI chat limit has been reached. Please try again tomorrow.", 429);
        }
        String cleanedMessage = Validator.truncate(message.trim(), 1000);
        String context = chatDao.findRecentMessages(chatSessionId, 12).stream()
                .map(item -> item.getSender() + ": " + item.getMessage())
                .collect(Collectors.joining("\n"));
        chatDao.saveMessage(chatSessionId, "USER", cleanedMessage);

        GeminiAnswerService.Answer answer = aiAnswerService.answerWithConversation(cleanedMessage, context);
        String reply = answer.getText();
        long id = chatDao.saveMessage(chatSessionId, "BOT", reply);

        ChatMessage response = new ChatMessage();
        response.setId(id);
        response.setChatSessionId(chatSessionId);
        response.setSender("BOT");
        response.setMessage(reply);
        response.setSources(answer.getSources());
        return response;
    }

    public List<ChatMessage> history(String sessionKey, Long userId) {
        long chatSessionId = chatDao.findOrCreateSession(sessionKey, userId);
        return chatDao.findMessages(chatSessionId, 100);
    }

}
