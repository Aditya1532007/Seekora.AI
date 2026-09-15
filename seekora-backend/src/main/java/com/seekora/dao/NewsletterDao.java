package com.seekora.dao;

import com.seekora.config.DatabaseConfig;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class NewsletterDao {

    /** Idempotent subscribe - re-subscribes an address that had opted out. */
    public boolean subscribe(String email) {
        String sql = "INSERT INTO newsletter_subscribers (email) VALUES (?) " +
                "ON DUPLICATE KEY UPDATE status = 'SUBSCRIBED', subscribed_at = NOW()";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DaoException("Unable to subscribe", e);
        }
    }

    public boolean unsubscribe(String email) {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE newsletter_subscribers SET status = 'UNSUBSCRIBED' WHERE email = ?")) {
            ps.setString(1, email);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DaoException("Unable to unsubscribe", e);
        }
    }

    public long countSubscribed() {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT COUNT(*) FROM newsletter_subscribers WHERE status = 'SUBSCRIBED'");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getLong(1) : 0L;
        } catch (SQLException e) {
            throw new DaoException("Unable to count subscribers", e);
        }
    }
}
