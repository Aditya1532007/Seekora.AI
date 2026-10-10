package com.seekora.service;

import com.seekora.dao.ConversationDao;
import com.seekora.model.Source;
import com.seekora.util.Validator;

import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AiConversationService {
    private static final int MAX_IMAGE_BYTES = 5 * 1024 * 1024;
    private static final Pattern IMAGE_DATA_URL =
            Pattern.compile("^data:(image/(?:jpeg|png|gif|webp));base64,([A-Za-z0-9+/=\\r\\n]+)$");

    private final ConversationDao conversationDao = new ConversationDao();
    private final GeminiAnswerService aiAnswerService = new GeminiAnswerService();
    private final WebPageAnalysisService webPageAnalysisService = new WebPageAnalysisService();

    public List<Map<String, Object>> listConversations(long userId) {
        return conversationDao.findConversations(userId);
    }

    public List<Map<String, Object>> listMessages(String conversationId, long userId) {
        long id = parseId(conversationId);
        if (!conversationDao.belongsToUser(id, userId)) throw new ServiceException("Conversation not found", 404);
        return conversationDao.findMessages(id, userId);
    }

    public boolean deleteConversation(String conversationId, long userId) {
        if (!conversationDao.deleteConversation(parseId(conversationId), userId)) {
            throw new ServiceException("Conversation not found", 404);
        }
        return true;
    }

    public Map<String, Object> answer(String query, String mode, String imageData,
                                      String conversationId, String history,
                                      Long userId, boolean save) {
        if (Validator.isBlank(query)) throw new ServiceException("Please enter a question.");
        String cleanedQuery = Validator.truncate(query.trim(), 5000);
        String selectedMode = mode == null || mode.trim().isEmpty() ? "ask" : mode.trim().toLowerCase();
        if (!isSupportedMode(selectedMode)) throw new ServiceException("Unsupported question mode.");

        Long existingId = null;
        String context = history;
        if (save) {
            if (userId == null) throw new ServiceException("Please sign in to save this conversation.", 401);
            if (conversationId != null && !conversationId.trim().isEmpty()) {
                existingId = parseId(conversationId);
                if (!conversationDao.belongsToUser(existingId, userId)) {
                    throw new ServiceException("Conversation not found", 404);
                }
                context = conversationDao.findRecentContext(existingId, userId, 12);
            }
        }

        GeminiAnswerService.Answer answer;
        if ("analyze_image".equals(selectedMode)) {
            ImagePayload image = decodeImage(imageData);
            answer = aiAnswerService.summarizeImage(image.bytes, image.mimeType, cleanedQuery);
        } else if ("analyze_url".equals(selectedMode)) {
            WebPageAnalysisService.Result result = webPageAnalysisService.summarize(cleanedQuery);
            answer = new GeminiAnswerService.Answer(result.getSummary(), result.getSources());
        } else {
            answer = aiAnswerService.answerWithConversation(cleanedQuery, context);
        }

        String savedConversationId = null;
        if (save) {
            savedConversationId = conversationDao.saveTurn(existingId, userId, cleanedQuery,
                    answer.getText(), answer.getSources(), selectedMode);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("answer", answer.getText());
        result.put("sources", answer.getSources());
        result.put("conversationId", savedConversationId);
        return result;
    }

    private boolean isSupportedMode(String mode) {
        return "ask".equals(mode) || "research".equals(mode) || "chat".equals(mode)
                || "analyze_url".equals(mode) || "analyze_image".equals(mode);
    }

    private long parseId(String value) {
        if (value == null || !value.matches("[1-9][0-9]{0,18}")) {
            throw new ServiceException("Invalid conversation ID.");
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new ServiceException("Invalid conversation ID.");
        }
    }

    private ImagePayload decodeImage(String dataUrl) {
        if (dataUrl == null) throw new ServiceException("Choose an image to analyze.");
        Matcher matcher = IMAGE_DATA_URL.matcher(dataUrl);
        if (!matcher.matches()) {
            throw new ServiceException("Use a JPEG, PNG, GIF, or WebP image.");
        }
        try {
            byte[] bytes = Base64.getDecoder().decode(matcher.group(2));
            if (bytes.length == 0 || bytes.length > MAX_IMAGE_BYTES) {
                throw new ServiceException("Images must be smaller than 5 MB.");
            }
            return new ImagePayload(matcher.group(1), bytes);
        } catch (IllegalArgumentException e) {
            throw new ServiceException("The uploaded image is not valid base64 data.");
        }
    }

    private static final class ImagePayload {
        private final String mimeType;
        private final byte[] bytes;

        private ImagePayload(String mimeType, byte[] bytes) {
            this.mimeType = mimeType;
            this.bytes = bytes;
        }
    }
}
