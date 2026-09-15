package com.seekora.dao;

import com.seekora.config.DatabaseConfig;
import com.seekora.model.Subscription;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class SubscriptionDao {

    /**
     * Cancels any running subscription and stores the new one, then points the
     * user record at the new plan - all inside one transaction.
     */
    public long subscribe(long userId, int planId, String billingCycle, BigDecimal amount) {
        String cancel = "UPDATE subscriptions SET status = 'CANCELLED' WHERE user_id = ? AND status = 'ACTIVE'";
        String insert = "INSERT INTO subscriptions (user_id, plan_id, billing_cycle, amount, expires_at) " +
                "VALUES (?, ?, ?, ?, DATE_ADD(NOW(), INTERVAL ? MONTH))";
        String updateUser = "UPDATE users SET plan_id = ? WHERE id = ?";
        int months = "YEARLY".equalsIgnoreCase(billingCycle) ? 12 : 1;

        Connection conn = null;
        try {
            conn = DatabaseConfig.getConnection();
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(cancel)) {
                ps.setLong(1, userId);
                ps.executeUpdate();
            }
            long id;
            try (PreparedStatement ps = conn.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS)) {
                ps.setLong(1, userId);
                ps.setInt(2, planId);
                ps.setString(3, billingCycle.toUpperCase());
                ps.setBigDecimal(4, amount);
                ps.setInt(5, months);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    id = keys.next() ? keys.getLong(1) : 0L;
                }
            }
            try (PreparedStatement ps = conn.prepareStatement(updateUser)) {
                ps.setInt(1, planId);
                ps.setLong(2, userId);
                ps.executeUpdate();
            }
            conn.commit();
            return id;
        } catch (SQLException e) {
            rollback(conn);
            throw new DaoException("Unable to create subscription", e);
        } finally {
            close(conn);
        }
    }

    public List<Subscription> findByUser(long userId) {
        String sql = "SELECT s.id, s.user_id, s.plan_id, s.billing_cycle, s.amount, s.status, " +
                "       s.started_at, s.expires_at, p.code AS plan_code " +
                "FROM subscriptions s JOIN plans p ON p.id = s.plan_id " +
                "WHERE s.user_id = ? ORDER BY s.started_at DESC";
        List<Subscription> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Subscription s = new Subscription();
                    s.setId(rs.getLong("id"));
                    s.setUserId(rs.getLong("user_id"));
                    s.setPlanId(rs.getInt("plan_id"));
                    s.setPlanCode(rs.getString("plan_code"));
                    s.setBillingCycle(rs.getString("billing_cycle"));
                    s.setAmount(rs.getBigDecimal("amount"));
                    s.setStatus(rs.getString("status"));
                    Timestamp started = rs.getTimestamp("started_at");
                    if (started != null) {
                        s.setStartedAt(started.toLocalDateTime());
                    }
                    Timestamp expires = rs.getTimestamp("expires_at");
                    if (expires != null) {
                        s.setExpiresAt(expires.toLocalDateTime());
                    }
                    list.add(s);
                }
            }
        } catch (SQLException e) {
            throw new DaoException("Unable to list subscriptions", e);
        }
        return list;
    }

    private void rollback(Connection conn) {
        if (conn != null) {
            try {
                conn.rollback();
            } catch (SQLException ignored) {
                // nothing else we can do
            }
        }
    }

    private void close(Connection conn) {
        if (conn != null) {
            try {
                conn.setAutoCommit(true);
                conn.close();
            } catch (SQLException ignored) {
                // nothing else we can do
            }
        }
    }
}
