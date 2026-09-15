package com.seekora.servlet;

import com.seekora.service.SearchService;
import com.seekora.service.ServiceException;
import com.seekora.util.ServletUtils;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * GET    /api/history?limit=20&offset=0 - the signed-in user's search history
 * DELETE /api/history/{id}              - remove one entry
 * DELETE /api/history                   - clear the whole history
 */
@WebServlet("/api/history/*")
public class HistoryServlet extends BaseServlet {

    private final SearchService searchService = new SearchService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        handle(request, response, () -> {
            long userId = requireUser(request);
            ServletUtils.ok(response, searchService.history(userId,
                    intParam(request, "limit", 20), intParam(request, "offset", 0)));
        });
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws IOException {
        handle(request, response, () -> {
            long userId = requireUser(request);
            String segment = pathSegment(request);
            if (segment == null) {
                int removed = searchService.clearHistory(userId);
                ServletUtils.ok(response, "History cleared", removed);
                return;
            }
            long id;
            try {
                id = Long.parseLong(segment);
            } catch (NumberFormatException e) {
                throw new ServiceException("Invalid history id");
            }
            if (!searchService.deleteHistoryItem(id, userId)) {
                throw new ServiceException("History entry not found", 404);
            }
            ServletUtils.ok(response, "Entry deleted", id);
        });
    }

    private long requireUser(HttpServletRequest request) {
        Long userId = ServletUtils.currentUserId(request);
        if (userId == null) {
            throw new ServiceException("Please log in to continue", 401);
        }
        return userId;
    }
}
