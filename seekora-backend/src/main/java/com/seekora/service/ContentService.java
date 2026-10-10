package com.seekora.service;

import com.seekora.dao.ContactDao;
import com.seekora.dao.ContentDao;
import com.seekora.dao.NewsletterDao;
import com.seekora.dao.SearchDao;
import com.seekora.model.ContactMessage;
import com.seekora.model.Faq;
import com.seekora.model.Feature;
import com.seekora.model.Testimonial;
import com.seekora.util.Validator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ContentService {

    private final ContentDao contentDao = new ContentDao();
    private final NewsletterDao newsletterDao = new NewsletterDao();
    private final ContactDao contactDao = new ContactDao();
    private final SearchDao searchDao = new SearchDao();

    public List<Faq> faqs() {
        List<Faq> items = new ArrayList<>();
        items.add(faq(1, "What does Seekora search?",
                "Seekora answers general questions with AI and can search the web for current information. Saved topics may use their stored summaries.", 1));
        items.add(faq(2, "Does Seekora show sources?",
                "AI answers include clickable web sources when the answer uses web search. Saved topics show their stored source links.", 2));
        items.add(faq(3, "Can I save my searches?",
                "Signed-in users can view and manage their search history. Guest searches are associated with the current browser session.", 3));
        items.add(faq(4, "Does Premium process a payment?",
                "No. Plan selection demonstrates subscription state only; no payment gateway is connected.", 4));
        return items;
    }

    public List<Testimonial> testimonials() {
        // Legacy demo testimonials are not verified user feedback.
        return Collections.emptyList();
    }

    public List<Feature> features() {
        List<Feature> items = new ArrayList<>();
        items.add(feature(1, "AI Answers", "Ask general questions and get a direct, conversational answer.", 1));
        items.add(feature(2, "Web Search", "Use web search for current questions and changing facts.", 2));
        items.add(feature(3, "Clickable Sources", "Follow source links supplied with web-grounded answers.", 3));
        items.add(feature(4, "Search History", "Signed-in users can review and manage previous searches.", 4));
        items.add(feature(5, "Voice Input", "Use browser speech recognition where the browser supports it.", 5));
        items.add(feature(6, "Account Profiles", "Create an account to keep search history and manage a profile.", 6));
        return items;
    }

    public Map<String, Object> liveStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalUsers", contentDao.countUsers());
        stats.put("totalSearches", searchDao.countAll());
        stats.put("newsletterSubscribers", newsletterDao.countSubscribed());
        // platform_stats is legacy marketing content and may contain claims
        // that are not backed by live measurements. Keep only measured totals.
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
        if (message.length() > 5000) {
            throw new ServiceException("Message must be 5,000 characters or fewer");
        }
        ContactMessage contact = new ContactMessage();
        contact.setName(Validator.truncate(name.trim(), 120));
        contact.setEmail(email.trim().toLowerCase());
        contact.setSubject(Validator.truncate(
                Validator.isBlank(subject) ? "General enquiry" : subject.trim(), 200));
        contact.setMessage(message.trim());
        return contactDao.save(contact);
    }

    private Faq faq(int id, String question, String answer, int sortOrder) {
        Faq faq = new Faq();
        faq.setId(id);
        faq.setQuestion(question);
        faq.setAnswer(answer);
        faq.setSortOrder(sortOrder);
        return faq;
    }

    private Feature feature(int id, String title, String description, int sortOrder) {
        Feature feature = new Feature();
        feature.setId(id);
        feature.setIcon("*");
        feature.setTitle(title);
        feature.setDescription(description);
        feature.setSortOrder(sortOrder);
        return feature;
    }
}
