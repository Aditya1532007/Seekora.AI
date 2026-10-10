package com.seekora.dao;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.seekora.config.DatabaseConfig;
import com.seekora.model.Source;
import com.seekora.service.ServiceException;
import com.seekora.util.JsonUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ConversationDao {

    public List<Map<String, Object>> findConversations(long userId) {
        String sql = "SELECT id, title, created_at, updated_at FROM conversations "
                + "WHERE user_id = ? ORDER BY updated_at DESC, id DESC";
        List<Map<String, Object>> conversations = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> conversation = new LinkedHashMap<>();
                    conversation.put("id", String.valueOf(rs.getLong("id")));
                    conversation.put("title", rs.getString("title"));
                    conversation.put("created_at", timestamp(rs.getTimestamp("created_at")));
                    conversation.put("updated_at", timestamp(rs.getTimestamp("updated_at")));
                    conversations.add(conversation);
                }
            }
            return conversations;
        } catch (SQLException e) {
            throw new DaoException("Unable to load conversations", e);
        }
    }

    public List<Map<String, Object>> findMessages(long conversationId, long userId) {
        String sql = "SELECT m.id, m.role, m.content, m.sources, m.mode, m.created_at "
                + "FROM messages m JOIN conversations c ON c.id = m.conversation_id "
                + "WHERE c.id = ? AND c.user_id = ? ORDER BY m.created_at ASC, m.id ASC LIMIT 500";
        List<Map<String, Object>> messages = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, conversationId);
            ps.setLong(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> message = new LinkedHashMap<>();
                    message.put("id", String.valueOf(rs.getLong("id")));
                    message.put("conversation_id", String.valueOf(conversationId));
                    message.put("role", "USER".equals(rs.getString("role")) ? "user" : "assistant");
                    message.put("content", rs.getString("content"));
                    message.put("sources", parseSources(rs.getString("sources")));
                    message.put("mode", rs.getString("mode"));
                    message.put("created_at", timestamp(rs.getTimestamp("created_at")));
                    messages.add(message);
                }
            }
            return messages;
        } catch (SQLException e) {
            throw new DaoException("Unable to load conversation messages", e);
        }
    }

    public String findRecentContext(long conversationId, long userId, int limit) {
        String sql = "SELECT m.role, m.content FROM messages m "
                + "JOIN conversations c ON c.id = m.conversation_id "
                + "WHERE c.id = ? AND c.user_id = ? ORDER BY m.created_at DESC, m.id DESC LIMIT ?";
        List<String> lines = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, conversationId);
            ps.setLong(2, userId);
            ps.setInt(3, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String role = "USER".equals(rs.getString("role")) ? "User" : "Assistant";
                    lines.add(role + ": " + rs.getString("content"));
                }
            }
            Collections.reverse(lines);
            return String.join("\n", lines);
        } catch (SQLException e) {
            throw new DaoException("Unable to load conversation context", e);
        }
    }

    public boolean belongsToUser(long conversationId, long userId) {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT 1 FROM conversations WHERE id = ? AND user_id = ?")) {
            ps.setLong(1, conversationId);
            ps.setLong(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new DaoException("Unable to verify conversation ownership", e);
        }
    }

    public boolean deleteConversation(long conversationId, long userId) {
        String sql = "DELETE FROM conversations WHERE id = ? AND user_id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, conversationId);
            ps.setLong(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DaoException("Unable to delete conversation", e);
        }
    }

    public String saveTurn(Long conversationId, long userId, String query, String answer,
                           List<Source> sources, String mode) {
        String conversationTitle = query.length() > 60 ? query.substring(0, 60) : query;
        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false);
            long id;
            if (conversationId == null) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO conversations (user_id, title) VALUES (?, ?)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    ps.setLong(1, userId);
                    ps.setString(2, conversationTitle);
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (!keys.next()) throw new SQLException("Conversation insert returned no key");
                        id = keys.getLong(1);
                    }
                }
            } else {
                id = conversationId;
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT id FROM conversations WHERE id = ? AND user_id = ? FOR UPDATE")) {
                    ps.setLong(1, id);
                    ps.setLong(2, userId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) throw new ServiceException("Conversation not found", 404);
                    }
                }
            }

            insertMessage(conn, id, "USER", query, Collections.emptyList(), mode);
            insertMessage(conn, id, "ASSISTANT", answer, sources, mode);
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE conversations SET updated_at = CURRENT_TIMESTAMP WHERE id = ?")) {
                ps.setLong(1, id);
                ps.executeUpdate();
            }
            conn.commit();
            return String.valueOf(id);
        } catch (SQLException e) {
            throw new DaoException("Unable to save conversation messages", e);
        }
    }

    private void insertMessage(Connection conn, long conversationId, String role, String content,
                               List<Source> sources, String mode) throws SQLException {
        String sql = "INSERT INTO messages (conversation_id, role, content, sources, mode) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, conversationId);
            ps.setString(2, role);
            ps.setString(3, content);
            ps.setString(4, JsonUtil.toJson(sources));
            ps.setString(5, mode);
            ps.executeUpdate();
        }
    }

    private JsonElement parseSources(String value) {
        if (value == null || value.trim().isEmpty()) return new JsonArray();
        try {
            return JsonParser.parseString(value);
        } catch (RuntimeException e) {
            throw new DaoException("Stored conversation sources contain invalid JSON", e);
        }
    }

    private String timestamp(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime().toString();
    }
}
