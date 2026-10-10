package com.seekora.dao;

import com.seekora.config.DatabaseConfig;
import com.seekora.model.ChatMessage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Collections;

public class ChatDao {

    /** Returns the id of the chat session for this browser session, creating it if needed. */
    public long findOrCreateSession(String sessionKey, Long userId) {
        try (Connection conn = DatabaseConfig.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT id FROM chat_sessions WHERE session_key = ?")) {
                ps.setString(1, sessionKey);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getLong(1);
                    }
                }
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO chat_sessions (session_key, user_id) VALUES (?, ?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, sessionKey);
                if (userId == null) {
                    ps.setNull(2, Types.BIGINT);
                } else {
                    ps.setLong(2, userId);
                }
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        return keys.getLong(1);
                    }
                }
            }
            throw new DaoException("Chat session insert returned no key");
        } catch (SQLException e) {
            throw new DaoException("Unable to open a chat session", e);
        }
    }

    public long saveMessage(long chatSessionId, String sender, String message) {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO chat_messages (chat_session_id, sender, message) VALUES (?, ?, ?)",
                     Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, chatSessionId);
            ps.setString(2, sender);
            ps.setString(3, message);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getLong(1) : 0L;
            }
        } catch (SQLException e) {
            throw new DaoException("Unable to save the chat message", e);
        }
    }

    public int countBotRepliesToday(long chatSessionId) {
        String sql = "SELECT COUNT(*) FROM chat_messages WHERE chat_session_id = ? "
                + "AND sender = 'BOT' AND DATE(created_at) = CURDATE()";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, chatSessionId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new DaoException("Unable to count today's chat replies", e);
        }
    }

    public List<ChatMessage> findMessages(long chatSessionId, int limit) {
        String sql = "SELECT id, chat_session_id, sender, message, created_at FROM chat_messages " +
                "WHERE chat_session_id = ? ORDER BY created_at ASC, id ASC LIMIT ?";
        List<ChatMessage> messages = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, chatSessionId);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ChatMessage m = new ChatMessage();
                    m.setId(rs.getLong("id"));
                    m.setChatSessionId(rs.getLong("chat_session_id"));
                    m.setSender(rs.getString("sender"));
                    m.setMessage(rs.getString("message"));
                    Timestamp created = rs.getTimestamp("created_at");
                    if (created != null) {
                        m.setCreatedAt(created.toLocalDateTime());
                    }
                    messages.add(m);
                }
            }
        } catch (SQLException e) {
            throw new DaoException("Unable to load chat messages", e);
        }
        return messages;
    }

    /** Loads the newest messages in chronological order for AI follow-up context. */
    public List<ChatMessage> findRecentMessages(long chatSessionId, int limit) {
        String sql = "SELECT id, chat_session_id, sender, message, created_at FROM chat_messages "
                + "WHERE chat_session_id = ? ORDER BY created_at DESC, id DESC LIMIT ?";
        List<ChatMessage> messages = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, chatSessionId);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ChatMessage m = new ChatMessage();
                    m.setId(rs.getLong("id"));
                    m.setChatSessionId(rs.getLong("chat_session_id"));
                    m.setSender(rs.getString("sender"));
                    m.setMessage(rs.getString("message"));
                    Timestamp created = rs.getTimestamp("created_at");
                    if (created != null) m.setCreatedAt(created.toLocalDateTime());
                    messages.add(m);
                }
            }
        } catch (SQLException e) {
            throw new DaoException("Unable to load recent chat messages", e);
        }
        Collections.reverse(messages);
        return messages;
    }

    /** intent keywords -> reply, ordered by sort_order. */
    public Map<String, String> findIntents() {
        Map<String, String> intents = new LinkedHashMap<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT keywords, reply FROM chat_intents ORDER BY sort_order");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                intents.put(rs.getString("keywords"), rs.getString("reply"));
            }
        } catch (SQLException e) {
            throw new DaoException("Unable to load chat intents", e);
        }
        return intents;
    }
}
