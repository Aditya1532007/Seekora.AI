package com.seekora.dao;

import com.seekora.config.DatabaseConfig;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

public class ActivityLogDao {

    public void log(Long userId, String action, String details, String ip) {
        String sql = "INSERT INTO activity_log (user_id, action, details, ip_address) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (userId == null) {
                ps.setNull(1, Types.BIGINT);
            } else {
                ps.setLong(1, userId);
            }
            ps.setString(2, action);
            ps.setString(3, details);
            ps.setString(4, ip);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DaoException("Unable to write the activity log", e);
        }
    }
}
