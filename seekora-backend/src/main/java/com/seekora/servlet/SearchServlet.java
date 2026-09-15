package com.seekora.servlet;

import com.google.gson.JsonObject;
import com.seekora.model.SearchQuery;
import com.seekora.service.SearchService;
import com.seekora.util.JsonUtil;
import com.seekora.util.ServletUtils;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * POST /api/search  {query}   - ask a question (works for guests too)
 * GET  /api/search?q=...      - same, convenient for the browser / curl
 */
@WebServlet("/api/search")
public class SearchServlet extends BaseServlet {

    private final SearchService searchService = new SearchService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        handle(request, response, () -> {
            JsonObject body = JsonUtil.readBody(request);
            respond(request, response, JsonUtil.string(body, "query"));
        });
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        handle(request, response, () -> respond(request, response, request.getParameter("q")));
    }

    private void respond(HttpServletRequest request, HttpServletResponse response, String query) throws IOException {
        Long userId = ServletUtils.currentUserId(request);
        SearchQuery result = searchService.search(query, userId,
                ServletUtils.sessionKey(request), ServletUtils.clientIp(request));

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("search", result);
        payload.put("searchesRemainingToday", searchService.remainingToday(userId));
        ServletUtils.ok(response, payload);
    }
}
