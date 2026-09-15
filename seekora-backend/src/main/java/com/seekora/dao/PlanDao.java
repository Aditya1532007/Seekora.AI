package com.seekora.dao;

import com.seekora.config.DatabaseConfig;
import com.seekora.model.Plan;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class PlanDao {

    public List<Plan> findAllWithFeatures() {
        String sql = "SELECT p.id, p.code, p.name, p.price_monthly, p.price_yearly, p.daily_search_limit, " +
                "       p.is_featured, p.sort_order, f.feature " +
                "FROM plans p LEFT JOIN plan_features f ON f.plan_id = p.id " +
                "ORDER BY p.sort_order, f.sort_order";
        Map<Integer, Plan> plans = new LinkedHashMap<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int id = rs.getInt("id");
                Plan plan = plans.get(id);
                if (plan == null) {
                    plan = map(rs);
                    plans.put(id, plan);
                }
                String feature = rs.getString("feature");
                if (feature != null) {
                    plan.getFeatures().add(feature);
                }
            }
        } catch (SQLException e) {
            throw new DaoException("Unable to list plans", e);
        }
        return new ArrayList<>(plans.values());
    }

    public Optional<Plan> findByCode(String code) {
        String sql = "SELECT id, code, name, price_monthly, price_yearly, daily_search_limit, is_featured, sort_order " +
                "FROM plans WHERE code = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DaoException("Unable to load plan", e);
        }
    }

    private Plan map(ResultSet rs) throws SQLException {
        Plan plan = new Plan();
        plan.setId(rs.getInt("id"));
        plan.setCode(rs.getString("code"));
        plan.setName(rs.getString("name"));
        plan.setPriceMonthly(rs.getBigDecimal("price_monthly"));
        plan.setPriceYearly(rs.getBigDecimal("price_yearly"));
        plan.setDailySearchLimit(rs.getInt("daily_search_limit"));
        plan.setFeatured(rs.getBoolean("is_featured"));
        plan.setSortOrder(rs.getInt("sort_order"));
        return plan;
    }
}
