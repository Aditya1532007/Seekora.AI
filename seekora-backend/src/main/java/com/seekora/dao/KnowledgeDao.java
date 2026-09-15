package com.seekora.dao;

import com.seekora.config.DatabaseConfig;
import com.seekora.model.KnowledgeEntry;
import com.seekora.model.Source;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class KnowledgeDao {

    /**
     * Finds the best knowledge entry for a query by matching any of the stored
     * keywords (comma separated) against the query text.
     */
    public Optional<KnowledgeEntry> findBestMatch(String query) {
        String sql = "SELECT id, topic, keywords, summary, accuracy FROM knowledge_base WHERE is_active = 1";
        String normalized = query.toLowerCase();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            KnowledgeEntry best = null;
            int bestScore = 0;
            while (rs.next()) {
                KnowledgeEntry entry = map(rs);
                int score = score(entry, normalized);
                if (score > bestScore) {
                    bestScore = score;
                    best = entry;
                }
            }
            if (best == null) {
                return Optional.empty();
            }
            best.setSources(findSources(best.getId()));
            return Optional.of(best);
        } catch (SQLException e) {
            throw new DaoException("Unable to search the knowledge base", e);
        }
    }

    public List<KnowledgeEntry> findAll() {
        String sql = "SELECT id, topic, keywords, summary, accuracy FROM knowledge_base " +
                "WHERE is_active = 1 ORDER BY topic";
        List<KnowledgeEntry> entries = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                entries.add(map(rs));
            }
        } catch (SQLException e) {
            throw new DaoException("Unable to list knowledge entries", e);
        }
        for (KnowledgeEntry entry : entries) {
            entry.setSources(findSources(entry.getId()));
        }
        return entries;
    }

    public List<Source> findSources(long knowledgeId) {
        String sql = "SELECT id, title, url, source_type FROM knowledge_sources WHERE knowledge_id = ?";
        List<Source> sources = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, knowledgeId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    sources.add(new Source(rs.getLong("id"), rs.getString("title"),
                            rs.getString("url"), rs.getString("source_type")));
                }
            }
        } catch (SQLException e) {
            throw new DaoException("Unable to load sources", e);
        }
        return sources;
    }

    private int score(KnowledgeEntry entry, String normalizedQuery) {
        int score = 0;
        for (String keyword : entry.getKeywords().toLowerCase().split(",")) {
            String key = keyword.trim();
            if (!key.isEmpty() && normalizedQuery.contains(key)) {
                score = Math.max(score, key.length());
            }
        }
        if (normalizedQuery.contains(entry.getTopic().toLowerCase())) {
            score += entry.getTopic().length();
        }
        return score;
    }

    private KnowledgeEntry map(ResultSet rs) throws SQLException {
        KnowledgeEntry entry = new KnowledgeEntry();
        entry.setId(rs.getLong("id"));
        entry.setTopic(rs.getString("topic"));
        entry.setKeywords(rs.getString("keywords"));
        entry.setSummary(rs.getString("summary"));
        entry.setAccuracy(rs.getInt("accuracy"));
        return entry;
    }
}
