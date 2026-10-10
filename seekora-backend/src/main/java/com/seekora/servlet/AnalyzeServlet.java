package com.seekora.servlet;

import com.google.gson.JsonObject;
import com.seekora.model.Source;
import com.seekora.service.GeminiAnswerService;
import com.seekora.service.ServiceException;
import com.seekora.service.WebPageAnalysisService;
import com.seekora.util.JsonUtil;
import com.seekora.util.ServletUtils;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/** POST /api/analyze/url {url}; POST /api/analyze/image multipart (image, prompt). */
@WebServlet("/api/analyze/*")
@MultipartConfig(fileSizeThreshold = 1024 * 1024, maxFileSize = 5 * 1024 * 1024, maxRequestSize = 6 * 1024 * 1024)
public class AnalyzeServlet extends BaseServlet {
    private static final String IMAGE_COUNT = "seekoraImageAnalysesToday";
    private static final Set<String> IMAGE_TYPES = new HashSet<>(Arrays.asList("image/jpeg", "image/png", "image/gif", "image/webp"));
    private final WebPageAnalysisService webPageService = new WebPageAnalysisService();
    private final GeminiAnswerService ai = new GeminiAnswerService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        handle(request, response, () -> {
            String action = pathSegment(request);
            if ("url".equals(action)) {
                JsonObject body = JsonUtil.readBody(request);
                ServletUtils.ok(response, webPageService.summarize(JsonUtil.string(body, "url")));
            } else if ("image".equals(action)) {
                analyzeImage(request, response);
            } else {
                throw new ServiceException("Unknown analysis type", 404);
            }
        });
    }

    private void analyzeImage(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Integer count = (Integer) request.getSession(true).getAttribute(IMAGE_COUNT);
        if (count != null && count >= 10) throw new ServiceException("Today's image analysis limit has been reached.", 429);
        Part imagePart;
        try {
            imagePart = request.getPart("image");
        } catch (ServletException e) {
            throw new ServiceException("Could not read the uploaded image. Check the file and try again.", 400);
        } catch (IllegalStateException e) {
            throw new ServiceException("The upload is too large. Images must be 5 MB or smaller.", 413);
        }
        if (imagePart == null || imagePart.getSize() == 0) throw new ServiceException("Choose an image to analyze.", 400);
        if (imagePart.getSize() > 5L * 1024 * 1024) throw new ServiceException("Images must be 5 MB or smaller.", 413);
        String contentType = imagePart.getContentType();
        if (contentType == null || !IMAGE_TYPES.contains(contentType.toLowerCase())) {
            throw new ServiceException("Use a JPEG, PNG, GIF, or WebP image.", 415);
        }
        byte[] bytes = readAll(imagePart.getInputStream(), 5 * 1024 * 1024);
        if (!matchesImageType(bytes, contentType.toLowerCase())) {
            throw new ServiceException("The image contents do not match the selected file type.", 415);
        }
        String prompt = request.getParameter("prompt");
        if (prompt != null && prompt.length() > 500) throw new ServiceException("Image questions must be 500 characters or fewer.", 400);
        GeminiAnswerService.Answer answer = ai.summarizeImage(bytes, contentType.toLowerCase(), prompt);
        request.getSession().setAttribute(IMAGE_COUNT, count == null ? 1 : count + 1);
        ServletUtils.ok(response, "Image analyzed", new ImageResult(answer.getText(), answer.getSources()));
    }

    private boolean matchesImageType(byte[] bytes, String type) {
        if ("image/jpeg".equals(type)) return bytes.length >= 3 && (bytes[0] & 255) == 0xff && (bytes[1] & 255) == 0xd8 && (bytes[2] & 255) == 0xff;
        if ("image/png".equals(type)) return bytes.length >= 8 && (bytes[0] & 255) == 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4e && bytes[3] == 0x47;
        if ("image/gif".equals(type)) return bytes.length >= 6 && bytes[0] == 'G' && bytes[1] == 'I' && bytes[2] == 'F';
        return "image/webp".equals(type) && bytes.length >= 12 && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F' && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P';
    }

    private byte[] readAll(InputStream input, int limit) throws IOException {
        try (InputStream in = input; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int n;
            while ((n = in.read(buffer)) != -1) {
                if (out.size() + n > limit) throw new ServiceException("Images must be 5 MB or smaller.", 413);
                out.write(buffer, 0, n);
            }
            return out.toByteArray();
        }
    }

    private static final class ImageResult {
        private final String summary;
        private final java.util.List<Source> sources;
        ImageResult(String summary, java.util.List<Source> sources) { this.summary = summary; this.sources = sources; }
        public String getSummary() { return summary; }
        public java.util.List<Source> getSources() { return sources; }
    }
}
