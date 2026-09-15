package com.seekora.service;

import com.seekora.dao.ContactDao;
import com.seekora.dao.ContentDao;
import com.seekora.dao.NewsletterDao;
import com.seekora.dao.SearchDao;
import com.seekora.model.ContactMessage;
import com.seekora.model.Faq;
import com.seekora.model.Feature;
import com.seekora.model.PlatformStat;
import com.seekora.model.Testimonial;
import com.seekora.util.Validator;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ContentService {

    private final ContentDao contentDao = new ContentDao();
    private final NewsletterDao newsletterDao = new NewsletterDao();
    private final ContactDao contactDao = new ContactDao();
    private final SearchDao searchDao = new SearchDao();

    public List<Faq> faqs() {
        return contentDao.findFaqs();
    }

    public List<Testimonial> testimonials() {
        return contentDao.findTestimonials();
    }

    public List<Feature> features() {
        return contentDao.findFeatures();
    }

    public List<PlatformStat> stats() {
        return contentDao.findStats();
    }

    public Map<String, Object> liveStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalUsers", contentDao.countUsers());
        stats.put("totalSearches", searchDao.countAll());
        stats.put("newsletterSubscribers", newsletterDao.countSubscribed());
        stats.put("headlineStats", contentDao.findStats());
        return stats;
    }

    public void subscribeNewsletter(String email) {
        if (!Validator.isEmail(email)) {
            throw new ServiceException("Please enter a valid email address");
        }
        newsletterDao.subscribe(email.trim().toLowerCase());
    }

    public void unsubscribeNewsletter(String email) {
        if (!Validator.isEmail(email)) {
            throw new ServiceException("Please enter a valid email address");
        }
        newsletterDao.unsubscribe(email.trim().toLowerCase());
    }

    public long saveContactMessage(String name, String email, String subject, String message) {
        if (Validator.isBlank(name)) {
            throw new ServiceException("Name is required");
        }
        if (!Validator.isEmail(email)) {
            throw new ServiceException("A valid email address is required");
        }
        if (Validator.isBlank(message)) {
            throw new ServiceException("Message is required");
        }
        ContactMessage contact = new ContactMessage();
        contact.setName(Validator.truncate(name.trim(), 120));
        contact.setEmail(email.trim().toLowerCase());
        contact.setSubject(Validator.truncate(
                Validator.isBlank(subject) ? "General enquiry" : subject.trim(), 200));
        contact.setMessage(message.trim());
        return contactDao.save(contact);
    }
}
