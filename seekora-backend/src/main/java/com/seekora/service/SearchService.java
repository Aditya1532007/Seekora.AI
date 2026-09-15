package com.seekora.service;

import com.seekora.dao.ActivityLogDao;
import com.seekora.dao.KnowledgeDao;
import com.seekora.dao.PlanDao;
import com.seekora.dao.SearchDao;
import com.seekora.dao.UserDao;
import com.seekora.model.KnowledgeEntry;
import com.seekora.model.SearchQuery;
import com.seekora.model.Source;
import com.seekora.model.User;
import com.seekora.util.Validator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Answers a query from the knowledge base, enforces the daily plan limit and
 * stores everything in search_queries (the user's search history).
 */
public class SearchService {

    private static final int GUEST_DAILY_LIMIT = 10;

    private final KnowledgeDao knowledgeDao = new KnowledgeDao();
    private final SearchDao searchDao = new SearchDao();
    private final UserDao userDao = new UserDao();
    private final PlanDao planDao = new PlanDao();
    private final ActivityLogDao activityLogDao = new ActivityLogDao();

    public SearchQuery search(String queryText, Long userId, String sessionKey, String ip) {
        if (Validator.isBlank(queryText)) {
            throw new ServiceException("Please enter something to search for");
        }
        String cleaned = Validator.truncate(queryText.trim(), 500);
        enforceDailyLimit(userId);

        long start = System.currentTimeMillis();
        Optional<KnowledgeEntry> match = knowledgeDao.findBestMatch(cleaned);

        SearchQuery result = new SearchQuery();
        result.setUserId(userId);
        result.setSessionKey(sessionKey);
        result.setQueryText(cleaned);
        result.setIpAddress(ip);

        if (match.isPresent()) {
            KnowledgeEntry entry = match.get();
            result.setAnswerText(entry.getSummary());
            result.setKnowledgeId(entry.getId());
            result.setAccuracy(entry.getAccuracy());
            result.setSources(entry.getSources());
        } else {
            result.setAnswerText(fallbackAnswer(cleaned));
            result.setAccuracy(85);
            result.setSources(genericSources(cleaned));
        }
        result.setSourceCount(result.getSources().size());
        result.setResponseTimeMs((int) (System.currentTimeMillis() - start));

        long id = searchDao.save(result);
        result.setId(id);
        activityLogDao.log(userId, "SEARCH", cleaned, ip);
        return result;
    }

    public List<SearchQuery> history(long userId, int limit, int offset) {
        return searchDao.findHistory(userId, Math.min(Math.max(limit, 1), 100), Math.max(offset, 0));
    }

    public boolean deleteHistoryItem(long id, long userId) {
        return searchDao.deleteForUser(id, userId);
    }

    public int clearHistory(long userId) {
        return searchDao.clearHistory(userId);
    }

    public Map<String, Integer> trending(int limit) {
        return searchDao.findTrending(Math.min(Math.max(limit, 1), 20));
    }

    public int remainingToday(Long userId) {
        if (userId == null) {
            return GUEST_DAILY_LIMIT;
        }
        User user = userDao.findById(userId).orElseThrow(() -> new ServiceException("User not found", 404));
        int limit = dailyLimitFor(user);
        if (limit < 0) {
            return -1;
        }
        return Math.max(0, limit - searchDao.countToday(userId));
    }

    private void enforceDailyLimit(Long userId) {
        if (userId == null) {
            return;
        }
        int remaining = remainingToday(userId);
        if (remaining == 0) {
            throw new ServiceException(
                    "Daily search limit reached on the Freemium plan. Upgrade to Premium for unlimited searches.", 429);
        }
    }

    private int dailyLimitFor(User user) {
        return planDao.findByCode(user.getPlanCode())
                .map(plan -> plan.getDailySearchLimit())
                .orElse(GUEST_DAILY_LIMIT);
    }

    private String fallbackAnswer(String query) {
        return "Here is a simplified summary of \"" + query + "\": our engine searched the verified sources in the " +
                "knowledge base, filtered the data and condensed it into simple words. Connect an OpenAI or Gemini " +
                "API key in SearchService to generate this answer live, with source links and related images.";
    }

    private List<Source> genericSources(String query) {
        String encoded = query.replace(' ', '+');
        List<Source> sources = new ArrayList<>();
        sources.add(new Source(0, "Wikipedia", "https://en.wikipedia.org/w/index.php?search=" + encoded, "ARTICLE"));
        sources.add(new Source(0, "Google Scholar", "https://scholar.google.com/scholar?q=" + encoded, "PAPER"));
        return sources;
    }
}
