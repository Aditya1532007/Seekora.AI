package com.seekora.servlet;

import com.seekora.service.ContentService;
import com.seekora.service.SearchService;
import com.seekora.service.ServiceException;
import com.seekora.util.ServletUtils;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * GET /api/content/faqs
 * GET /api/content/testimonials
 * GET /api/content/features
 * GET /api/content/stats
 * GET /api/content/trending
 */
@WebServlet("/api/content/*")
public class ContentServlet extends BaseServlet {

    private final ContentService contentService = new ContentService();
    private final SearchService searchService = new SearchService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        handle(request, response, () -> {
            String section = pathSegment(request);
            if (section == null) {
                throw new ServiceException("Specify a section: faqs, testimonials, features, stats, trending", 404);
            }
            switch (section) {
                case "faqs":
                    ServletUtils.ok(response, contentService.faqs());
                    break;
                case "testimonials":
                    ServletUtils.ok(response, contentService.testimonials());
                    break;
                case "features":
                    ServletUtils.ok(response, contentService.features());
                    break;
                case "stats":
                    ServletUtils.ok(response, contentService.liveStats());
                    break;
                case "trending":
                    ServletUtils.ok(response, searchService.trending(intParam(request, "limit", 5)));
                    break;
                default:
                    throw new ServiceException("Unknown content section: " + section, 404);
            }
        });
    }
}
