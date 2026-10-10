package com.seekora.service;

import com.seekora.model.Source;
import com.seekora.util.Validator;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Fetches public HTML pages with strict bounds, then summarizes their text with the configured AI provider. */
public class WebPageAnalysisService {
    private static final int MAX_HTML_BYTES = 1_500_000;
    private static final int MAX_TEXT_CHARS = 18_000;
    private static final Pattern TITLE = Pattern.compile("(?is)<title[^>]*>(.*?)</title>");
    private static final Pattern REMOVE_BLOCKS = Pattern.compile("(?is)<(script|style|noscript|svg|iframe|form)[^>]*>.*?</\\1>");
    private static final Pattern TAGS = Pattern.compile("(?s)<[^>]+>");
    private static final Pattern ENTITY = Pattern.compile("&(#x[0-9a-fA-F]+|#\\d+|amp|lt|gt|quot|apos|nbsp);?");
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8)).followRedirects(HttpClient.Redirect.NEVER).build();
    private final GeminiAnswerService ai = new GeminiAnswerService();

    public Result summarize(String inputUrl) {
        if (Validator.isBlank(inputUrl) || inputUrl.length() > 2048) {
            throw new ServiceException("Enter a valid webpage URL (up to 2048 characters).", 400);
        }
        URI uri = parseAndValidate(inputUrl.trim());
        validatePublicHost(uri.getHost());
        HttpRequest request = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(15))
                .header("User-Agent", "SeekoraResearch/1.0")
                .header("Accept", "text/html,application/xhtml+xml")
                .GET().build();
        try {
            HttpResponse<InputStream> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofInputStream());
            try (InputStream body = response.body()) {
                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    throw new ServiceException("The webpage returned HTTP " + response.statusCode() + ".", 502);
                }
                String type = response.headers().firstValue("Content-Type").orElse("").toLowerCase(Locale.ROOT);
                if (!type.contains("text/html") && !type.contains("application/xhtml+xml")) {
                    throw new ServiceException("That URL does not return an HTML webpage.", 415);
                }
                byte[] html = readLimited(body, MAX_HTML_BYTES);
                String page = new String(html, StandardCharsets.UTF_8);
                Matcher titleMatcher = TITLE.matcher(page);
                String title = titleMatcher.find() ? decodeEntities(TAGS.matcher(titleMatcher.group(1)).replaceAll(" ")).trim() : uri.getHost();
                String text = extractText(page);
                if (text.length() < 80) throw new ServiceException("The page did not contain enough readable text to summarize.", 422);
                GeminiAnswerService.Answer answer = ai.summarizeText(title, uri.toString(), text);
                List<Source> sources = new ArrayList<>(answer.getSources());
                sources.add(0, new Source(0, title, uri.toString(), "WEBPAGE"));
                return new Result(title, answer.getText(), sources);
            }
        } catch (ServiceException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServiceException("The webpage request was interrupted. Please try again.", 503);
        } catch (IOException e) {
            throw new ServiceException("Could not retrieve that webpage. Check the URL and try again.", 502);
        }
    }

    private URI parseAndValidate(String value) {
        try {
            URI uri = URI.create(value);
            if (!uri.isAbsolute() || uri.getHost() == null || uri.getUserInfo() != null
                    || uri.getFragment() != null || !("http".equalsIgnoreCase(uri.getScheme())
                    || "https".equalsIgnoreCase(uri.getScheme()))) {
                throw new IllegalArgumentException();
            }
            return uri;
        } catch (IllegalArgumentException e) {
            throw new ServiceException("Only public http or https webpage URLs are supported.", 400);
        }
    }

    private void validatePublicHost(String host) {
        try {
            if (host.startsWith("[") && host.endsWith("]")) host = host.substring(1, host.length() - 1);
            InetAddress[] addresses = InetAddress.getAllByName(host);
            if (addresses.length == 0) throw new IllegalArgumentException();
            for (InetAddress address : addresses) {
                if (!isPublicAddress(address)) throw new IllegalArgumentException();
            }
        } catch (IOException | IllegalArgumentException e) {
            throw new ServiceException("That URL does not point to a publicly reachable host.", 400);
        }
    }

    private boolean isPublicAddress(InetAddress address) {
        if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress()
                || address.isSiteLocalAddress() || address.isMulticastAddress()) return false;
        byte[] b = address.getAddress();
        if (b.length == 4) {
            int a = b[0] & 255, c = b[1] & 255, d = b[2] & 255;
            return a != 0 && a != 10 && a != 127 && a < 224
                    && !(a == 100 && c >= 64 && c <= 127)
                    && !(a == 169 && c == 254)
                    && !(a == 172 && c >= 16 && c <= 31)
                    && !(a == 192 && (c == 0 || c == 168))
                    && !(a == 198 && (c == 18 || c == 19 || c == 51 && d == 100))
                    && !(a == 203 && c == 0 && d == 113);
        }
        int first = b[0] & 255, second = b[1] & 255;
        return (first & 0xe0) == 0x20 && !(first == 0x20 && second == 0x01 && (b[2] & 255) == 0x0d && (b[3] & 255) == 0xb8);
    }

    private byte[] readLimited(InputStream input, int limit) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int count;
        while ((count = input.read(buffer)) != -1) {
            if (output.size() + count > limit) throw new ServiceException("That webpage is too large to analyze (1.5 MB maximum).", 413);
            output.write(buffer, 0, count);
        }
        return output.toByteArray();
    }

    private String extractText(String html) {
        String text = REMOVE_BLOCKS.matcher(html).replaceAll(" ");
        text = TAGS.matcher(text).replaceAll(" ");
        text = decodeEntities(text).replace('\u00a0', ' ').replaceAll("\\s+", " ").trim();
        return text.length() > MAX_TEXT_CHARS ? text.substring(0, MAX_TEXT_CHARS) : text;
    }

    private String decodeEntities(String input) {
        Matcher matcher = ENTITY.matcher(input);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String entity = matcher.group(1), value = matcher.group();
            try {
                if (entity.startsWith("#x")) value = new String(Character.toChars(Integer.parseInt(entity.substring(2), 16)));
                else if (entity.startsWith("#")) value = new String(Character.toChars(Integer.parseInt(entity.substring(1))));
                else if ("amp".equalsIgnoreCase(entity)) value = "&";
                else if ("lt".equalsIgnoreCase(entity)) value = "<";
                else if ("gt".equalsIgnoreCase(entity)) value = ">";
                else if ("quot".equalsIgnoreCase(entity)) value = "\"";
                else if ("apos".equalsIgnoreCase(entity)) value = "'";
                else if ("nbsp".equalsIgnoreCase(entity)) value = " ";
            } catch (IllegalArgumentException ignored) { value = " "; }
            matcher.appendReplacement(result, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    public static final class Result {
        private final String title;
        private final String summary;
        private final List<Source> sources;
        Result(String title, String summary, List<Source> sources) { this.title = title; this.summary = summary; this.sources = sources; }
        public String getTitle() { return title; }
        public String getSummary() { return summary; }
        public List<Source> getSources() { return sources; }
    }
}
