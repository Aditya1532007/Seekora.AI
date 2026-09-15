package com.seekora.dao;

import com.seekora.config.DatabaseConfig;
import com.seekora.model.ContactMessage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class ContactDao {

    public long save(ContactMessage message) {
        String sql = "INSERT INTO contact_messages (name, email, subject, message) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, message.getName());
            ps.setString(2, message.getEmail());
            ps.setString(3, message.getSubject());
            ps.setString(4, message.getMessage());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getLong(1) : 0L;
            }
        } catch (SQLException e) {
            throw new DaoException("Unable to save the contact message", e);
        }
    }

    public List<ContactMessage> findAll(int limit) {
        String sql = "SELECT id, name, email, subject, message, status, created_at " +
                "FROM contact_messages ORDER BY created_at DESC LIMIT ?";
        List<ContactMessage> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ContactMessage m = new ContactMessage();
                    m.setId(rs.getLong("id"));
                    m.setName(rs.getString("name"));
                    m.setEmail(rs.getString("email"));
                    m.setSubject(rs.getString("subject"));
                    m.setMessage(rs.getString("message"));
                    m.setStatus(rs.getString("status"));
                    Timestamp created = rs.getTimestamp("created_at");
                    if (created != null) {
                        m.setCreatedAt(created.toLocalDateTime());
                    }
                    list.add(m);
                }
            }
        } catch (SQLException e) {
            throw new DaoException("Unable to list contact messages", e);
        }
        return list;
    }
}
