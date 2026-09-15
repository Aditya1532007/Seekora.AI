package com.seekora.dao;

import com.seekora.config.DatabaseConfig;
import com.seekora.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserDao {

    private static final String SELECT_BASE =
            "SELECT u.id, u.full_name, u.email, u.password_hash, u.role, u.plan_id, u.status, " +
            "       u.last_login_at, u.created_at, p.code AS plan_code, p.name AS plan_name " +
            "FROM users u JOIN plans p ON p.id = u.plan_id ";

    public Optional<User> findByEmail(String email) {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_BASE + "WHERE u.email = ?")) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DaoException("Unable to load user by email", e);
        }
    }

    public Optional<User> findById(long id) {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_BASE + "WHERE u.id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DaoException("Unable to load user by id", e);
        }
    }

    public boolean emailExists(String email) {
        return findByEmail(email).isPresent();
    }

    public long create(String fullName, String email, String passwordHash, int planId) {
        String sql = "INSERT INTO users (full_name, email, password_hash, plan_id) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, fullName);
            ps.setString(2, email);
            ps.setString(3, passwordHash);
            ps.setInt(4, planId);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
            throw new DaoException("User insert returned no generated key");
        } catch (SQLException e) {
            throw new DaoException("Unable to create user", e);
        }
    }

    public void touchLastLogin(long userId) {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE users SET last_login_at = NOW() WHERE id = ?")) {
            ps.setLong(1, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DaoException("Unable to update last login", e);
        }
    }

    public void updateProfile(long userId, String fullName) {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE users SET full_name = ? WHERE id = ?")) {
            ps.setString(1, fullName);
            ps.setLong(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DaoException("Unable to update profile", e);
        }
    }

    public void updatePassword(long userId, String passwordHash) {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE users SET password_hash = ? WHERE id = ?")) {
            ps.setString(1, passwordHash);
            ps.setLong(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DaoException("Unable to update password", e);
        }
    }

    public void updatePlan(long userId, int planId) {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE users SET plan_id = ? WHERE id = ?")) {
            ps.setInt(1, planId);
            ps.setLong(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DaoException("Unable to update plan", e);
        }
    }

    public List<User> findAll(int limit) {
        List<User> users = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_BASE + "ORDER BY u.created_at DESC LIMIT ?")) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    users.add(map(rs));
                }
            }
        } catch (SQLException e) {
            throw new DaoException("Unable to list users", e);
        }
        return users;
    }

    private User map(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getLong("id"));
        user.setFullName(rs.getString("full_name"));
        user.setEmail(rs.getString("email"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setRole(rs.getString("role"));
        user.setPlanId(rs.getInt("plan_id"));
        user.setPlanCode(rs.getString("plan_code"));
        user.setPlanName(rs.getString("plan_name"));
        user.setStatus(rs.getString("status"));
        Timestamp lastLogin = rs.getTimestamp("last_login_at");
        if (lastLogin != null) {
            user.setLastLoginAt(lastLogin.toLocalDateTime());
        }
        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) {
            user.setCreatedAt(created.toLocalDateTime());
        }
        return user;
    }
}
