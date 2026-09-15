package com.seekora.dao;

import com.seekora.config.DatabaseConfig;
import com.seekora.model.Faq;
import com.seekora.model.Feature;
import com.seekora.model.PlatformStat;
import com.seekora.model.Testimonial;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Read-only content that powers the marketing sections of the page. */
public class ContentDao {

    public List<Faq> findFaqs() {
        List<Faq> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT id, question, answer, sort_order FROM faqs WHERE is_active = 1 ORDER BY sort_order");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Faq faq = new Faq();
                faq.setId(rs.getInt("id"));
                faq.setQuestion(rs.getString("question"));
                faq.setAnswer(rs.getString("answer"));
                faq.setSortOrder(rs.getInt("sort_order"));
                list.add(faq);
            }
        } catch (SQLException e) {
            throw new DaoException("Unable to list FAQs", e);
        }
        return list;
    }

    public List<Testimonial> findTestimonials() {
        List<Testimonial> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT id, author_name, author_title, initials, rating, content FROM testimonials " +
                             "WHERE is_approved = 1 ORDER BY created_at");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Testimonial t = new Testimonial();
                t.setId(rs.getInt("id"));
                t.setAuthorName(rs.getString("author_name"));
                t.setAuthorTitle(rs.getString("author_title"));
                t.setInitials(rs.getString("initials"));
                t.setRating(rs.getInt("rating"));
                t.setContent(rs.getString("content"));
                list.add(t);
            }
        } catch (SQLException e) {
            throw new DaoException("Unable to list testimonials", e);
        }
        return list;
    }

    public List<Feature> findFeatures() {
        List<Feature> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT id, icon, title, description, sort_order FROM features ORDER BY sort_order");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Feature f = new Feature();
                f.setId(rs.getInt("id"));
                f.setIcon(rs.getString("icon"));
                f.setTitle(rs.getString("title"));
                f.setDescription(rs.getString("description"));
                f.setSortOrder(rs.getInt("sort_order"));
                list.add(f);
            }
        } catch (SQLException e) {
            throw new DaoException("Unable to list features", e);
        }
        return list;
    }

    public List<PlatformStat> findStats() {
        List<PlatformStat> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT id, label, value, suffix FROM platform_stats ORDER BY sort_order");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                PlatformStat s = new PlatformStat();
                s.setId(rs.getInt("id"));
                s.setLabel(rs.getString("label"));
                s.setValue(rs.getInt("value"));
                s.setSuffix(rs.getString("suffix"));
                list.add(s);
            }
        } catch (SQLException e) {
            throw new DaoException("Unable to list stats", e);
        }
        return list;
    }

    public long countUsers() {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT COUNT(*) FROM users WHERE status = 'ACTIVE'");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getLong(1) : 0L;
        } catch (SQLException e) {
            throw new DaoException("Unable to count users", e);
        }
    }
}
