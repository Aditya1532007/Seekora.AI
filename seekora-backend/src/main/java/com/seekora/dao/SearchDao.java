package com.seekora.dao;

import com.seekora.config.DatabaseConfig;
import com.seekora.model.SearchQuery;

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

public class SearchDao {

    public long save(SearchQuery query) {
        String sql = "INSERT INTO search_queries (user_id, session_key, query_text, answer_text, knowledge_id, " +
                "accuracy, response_time_ms, source_count, ip_address) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            if (query.getUserId() == null) {
                ps.setNull(1, Types.BIGINT);
            } else {
                ps.setLong(1, query.getUserId());
            }
            ps.setString(2, query.getSessionKey());
            ps.setString(3, query.getQueryText());
            ps.setString(4, query.getAnswerText());
            if (query.getKnowledgeId() == null) {
                ps.setNull(5, Types.BIGINT);
            } else {
                ps.setLong(5, query.getKnowledgeId());
            }
            ps.setInt(6, query.getAccuracy());
            ps.setInt(7, query.getResponseTimeMs());
            ps.setInt(8, query.getSourceCount());
            ps.setString(9, query.getIpAddress());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getLong(1) : 0L;
            }
        } catch (SQLException e) {
            throw new DaoException("Unable to save the search", e);
        }
    }

    public int countToday(long userId) {
        String sql = "SELECT COUNT(*) FROM search_queries WHERE user_id = ? AND DATE(created_at) = CURDATE()";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new DaoException("Unable to count today's searches", e);
        }
    }

    public int countTodayForSession(String sessionKey) {
        String sql = "SELECT COUNT(*) FROM search_queries WHERE user_id IS NULL " +
                "AND session_key = ? AND DATE(created_at) = CURDATE()";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, sessionKey);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new DaoException("Unable to count today's guest searches", e);
        }
    }

    public List<SearchQuery> findHistory(long userId, int limit, int offset) {
        String sql = "SELECT id, user_id, session_key, query_text, answer_text, knowledge_id, accuracy, " +
                "response_time_ms, source_count, created_at FROM search_queries " +
                "WHERE user_id = ? ORDER BY created_at DESC LIMIT ? OFFSET ?";
        List<SearchQuery> history = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setInt(2, limit);
            ps.setInt(3, offset);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    history.add(map(rs));
                }
            }
        } catch (SQLException e) {
            throw new DaoException("Unable to load search history", e);
        }
        return history;
    }

    public boolean deleteForUser(long id, long userId) {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "DELETE FROM search_queries WHERE id = ? AND user_id = ?")) {
            ps.setLong(1, id);
            ps.setLong(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DaoException("Unable to delete the search", e);
        }
    }

    public int clearHistory(long userId) {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM search_queries WHERE user_id = ?")) {
            ps.setLong(1, userId);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new DaoException("Unable to clear history", e);
        }
    }

    /** Most searched queries - used for the trending suggestions. */
    public Map<String, Integer> findTrending(int limit) {
        String sql = "SELECT query_text, COUNT(*) AS hits FROM search_queries " +
                "GROUP BY query_text ORDER BY hits DESC, MAX(created_at) DESC LIMIT ?";
        Map<String, Integer> trending = new LinkedHashMap<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    trending.put(rs.getString("query_text"), rs.getInt("hits"));
                }
            }
        } catch (SQLException e) {
            throw new DaoException("Unable to load trending searches", e);
        }
        return trending;
    }

    public long countAll() {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM search_queries");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getLong(1) : 0L;
        } catch (SQLException e) {
            throw new DaoException("Unable to count searches", e);
        }
    }

    private SearchQuery map(ResultSet rs) throws SQLException {
        SearchQuery q = new SearchQuery();
        q.setId(rs.getLong("id"));
        long userId = rs.getLong("user_id");
        q.setUserId(rs.wasNull() ? null : userId);
        q.setSessionKey(rs.getString("session_key"));
        q.setQueryText(rs.getString("query_text"));
        q.setAnswerText(rs.getString("answer_text"));
        long knowledgeId = rs.getLong("knowledge_id");
        q.setKnowledgeId(rs.wasNull() ? null : knowledgeId);
        q.setAccuracy(rs.getInt("accuracy"));
        q.setResponseTimeMs(rs.getInt("response_time_ms"));
        q.setSourceCount(rs.getInt("source_count"));
        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) {
            q.setCreatedAt(created.toLocalDateTime());
        }
        return q;
    }
}
