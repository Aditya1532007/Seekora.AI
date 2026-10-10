package com.seekora.service;

import com.seekora.dao.ActivityLogDao;
import com.seekora.dao.KnowledgeDao;
import com.seekora.dao.PlanDao;
import com.seekora.dao.SearchDao;
import com.seekora.dao.UserDao;
import com.seekora.model.KnowledgeEntry;
import com.seekora.model.SearchQuery;
import com.seekora.model.User;
import com.seekora.util.Validator;

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
    private final GeminiAnswerService aiAnswerService = new GeminiAnswerService();

    public SearchQuery search(String queryText, Long userId, String sessionKey, String ip) {
        if (Validator.isBlank(queryText)) {
            throw new ServiceException("Please enter something to search for");
        }
        String cleaned = Validator.truncate(queryText.trim(), 500);
        enforceDailyLimit(userId, sessionKey);

        long start = System.currentTimeMillis();
        Optional<KnowledgeEntry> match = knowledgeDao.findBestMatch(cleaned);
        SearchQuery result = new SearchQuery();
        result.setUserId(userId);
        result.setSessionKey(sessionKey);
        result.setQueryText(cleaned);
        result.setIpAddress(ip);
        String savedContext = match.map(KnowledgeEntry::getSummary).orElse(null);
        GeminiAnswerService.Answer answer = aiAnswerService.answer(cleaned, savedContext);
        result.setAnswerText(answer.getText());
        result.setSources(answer.getSources());
        match.ifPresent(entry -> result.setKnowledgeId(entry.getId()));
        result.setAccuracy(0);
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
        return remainingToday(userId, null);
    }

    public int remainingToday(Long userId, String sessionKey) {
        if (userId == null) {
            return Math.max(0, GUEST_DAILY_LIMIT - searchDao.countTodayForSession(sessionKey));
        }
        User user = userDao.findById(userId).orElseThrow(() -> new ServiceException("User not found", 404));
        int limit = dailyLimitFor(user);
        if (limit < 0) {
            return -1;
        }
        return Math.max(0, limit - searchDao.countToday(userId));
    }

    private void enforceDailyLimit(Long userId, String sessionKey) {
        int remaining = remainingToday(userId, sessionKey);
        if (remaining == 0) {
            throw new ServiceException("Your daily search limit has been reached. Please try again tomorrow.", 429);
        }
    }

    private int dailyLimitFor(User user) {
        return planDao.findByCode(user.getPlanCode())
                .map(plan -> plan.getDailySearchLimit())
                .orElse(GUEST_DAILY_LIMIT);
    }

}
