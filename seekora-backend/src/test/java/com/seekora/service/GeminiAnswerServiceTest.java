package com.seekora.service;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.seekora.model.Source;
import org.junit.Test;

import java.net.http.HttpRequest;
import java.net.http.HttpTimeoutException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertThrows;

public class GeminiAnswerServiceTest {
    private static final String GROUNDED_RESPONSE = "{\"status\":\"completed\",\"steps\":["
            + "{\"type\":\"google_search_call\",\"arguments\":{\"queries\":[\"sample query\"]}},"
            + "{\"type\":\"model_output\",\"content\":[{\"type\":\"text\","
            + "\"text\":\"Grounded answer.\",\"annotations\":[{\"type\":\"url_citation\","
            + "\"url\":\"https://example.org/source\",\"title\":\"Example source\"}]}]}]}";

    @Test
    public void sendsQuestionToGeminiAndReturnsOnlyGroundedCitations() {
        AtomicReference<HttpRequest> captured = new AtomicReference<>();
        GeminiAnswerService service = service("test-key", request -> {
            captured.set(request);
            return new GeminiAnswerService.HttpResponseData(200, GROUNDED_RESPONSE);
        });

        GeminiAnswerService.Answer answer = service.answer("What is the answer?", "Saved topic context");

        assertEquals("Grounded answer.", answer.getText());
        List<Source> sources = answer.getSources();
        assertEquals(1, sources.size());
        assertEquals("https://example.org/source", sources.get(0).getUrl());
        assertEquals("Example source", sources.get(0).getTitle());

        HttpRequest request = captured.get();
        assertEquals("https://generativelanguage.googleapis.com/v1beta/interactions",
                request.uri().toString());
        assertEquals("test-key", request.headers().firstValue("x-goog-api-key").orElse(null));
        assertFalse(request.uri().toString().contains("test-key"));
        JsonObject payload = JsonParser.parseString(body(request)).getAsJsonObject();
        assertEquals("gemini-3.8-flash", payload.get("model").getAsString());
        assertFalse(payload.get("store").getAsBoolean());
        assertTrue(payload.get("input").getAsString().contains("What is the answer?"));
        assertTrue(payload.get("input").getAsString().contains("Saved topic context"));
        assertEquals("google_search",
                payload.getAsJsonArray("tools").get(0).getAsJsonObject().get("type").getAsString());
    }

    @Test
    public void rejectsEmptyQuestionBeforeCallingProvider() {
        GeminiAnswerService service = service("", request -> {
            throw new AssertionError("Transport must not be called");
        });

        ServiceException error = assertThrows(ServiceException.class, () -> service.answer("  "));

        assertEquals(400, error.getStatusCode());
        assertEquals("Please enter a question.", error.getMessage());
    }

    @Test
    public void reportsMissingApiKeyAsJsonReadyServiceError() {
        GeminiAnswerService service = service("", request -> {
            throw new AssertionError("Transport must not be called");
        });

        ServiceException error = assertThrows(ServiceException.class, () -> service.answer("A question"));

        assertEquals(503, error.getStatusCode());
        assertTrue(error.getMessage().contains("GEMINI_API_KEY"));
    }

    @Test
    public void mapsInvalidKeyAndRateLimitResponsesWithoutLeakingProviderBody() {
        GeminiAnswerService invalidKey = service("bad-key", request ->
                new GeminiAnswerService.HttpResponseData(400,
                        "{\"error\":{\"message\":\"API key not valid. Please pass a valid API key.\"}}"));
        ServiceException keyError = assertThrows(ServiceException.class, () -> invalidKey.answer("A question"));
        assertEquals(503, keyError.getStatusCode());
        assertTrue(keyError.getMessage().contains("GEMINI_API_KEY"));
        assertFalse(keyError.getMessage().contains("bad-key"));

        GeminiAnswerService rateLimited = service("test-key", request ->
                new GeminiAnswerService.HttpResponseData(429, "{\"error\":{\"message\":\"quota detail\"}}"));
        ServiceException rateError = assertThrows(ServiceException.class, () -> rateLimited.answer("A question"));
        assertEquals(429, rateError.getStatusCode());
        assertFalse(rateError.getMessage().contains("quota detail"));
    }

    @Test
    public void retainsRedactedProviderDetailsForServerDiagnosticsOnly() {
        GeminiAnswerService service = service("test-key", request ->
                new GeminiAnswerService.HttpResponseData(400,
                        "{\"error\":{\"message\":\"Invalid model for test-key\"}}"));

        ServiceException error = assertThrows(ServiceException.class, () -> service.answer("A question"));

        assertEquals(502, error.getStatusCode());
        assertFalse(error.getMessage().contains("Invalid model"));
        assertTrue(error.getDiagnosticMessage().contains("HTTP 400"));
        assertTrue(error.getDiagnosticMessage().contains("[REDACTED]"));
        assertFalse(error.getDiagnosticMessage().contains("test-key"));
    }

    @Test
    public void includesSafeMetadataWhenProviderErrorBodyHasNoMessage() {
        GeminiAnswerService service = service("test-key", request ->
                new GeminiAnswerService.HttpResponseData(400, "", "text/plain"));

        ServiceException error = assertThrows(ServiceException.class, () -> service.answer("A question"));

        assertTrue(error.getDiagnosticMessage().contains("content-type=text/plain"));
        assertTrue(error.getDiagnosticMessage().contains("body-chars=0"));
    }

    @Test
    public void reportsOnlyArrayShapeForUnexpectedProviderErrorBody() {
        GeminiAnswerService service = service("test-key", request ->
                new GeminiAnswerService.HttpResponseData(400,
                        "[{\"reason\":\"validation\",\"secret\":\"must not be logged\"}]",
                        "application/json"));

        ServiceException error = assertThrows(ServiceException.class, () -> service.answer("A question"));

        assertTrue(error.getDiagnosticMessage().contains("JSON array length=1"));
        assertTrue(error.getDiagnosticMessage().contains("fields=[reason, secret]"));
        assertFalse(error.getDiagnosticMessage().contains("must not be logged"));
    }

    @Test
    public void extractsProviderMessageFromWrappedArrayError() {
        GeminiAnswerService service = service("test-key", request ->
                new GeminiAnswerService.HttpResponseData(400,
                        "[{\"error\":{\"message\":\"Unknown field in request\"}}]",
                        "application/json"));

        ServiceException error = assertThrows(ServiceException.class, () -> service.answer("A question"));

        assertTrue(error.getDiagnosticMessage().contains("Unknown field in request"));
    }

    @Test
    public void reportsTimeoutAndMalformedResponseAsGatewayErrors() {
        GeminiAnswerService timeout = service("test-key", request -> {
            throw new HttpTimeoutException("timeout");
        });
        assertEquals(504, assertThrows(ServiceException.class, () -> timeout.answer("A question")).getStatusCode());

        GeminiAnswerService malformed = service("test-key", request ->
                new GeminiAnswerService.HttpResponseData(200, "<html>not JSON</html>"));
        assertEquals(502, assertThrows(ServiceException.class, () -> malformed.answer("A question")).getStatusCode());
    }

    @Test
    public void sendsImageToGeminiWithoutEnablingWebSearch() {
        AtomicReference<HttpRequest> captured = new AtomicReference<>();
        GeminiAnswerService service = service("test-key", request -> {
            captured.set(request);
            return new GeminiAnswerService.HttpResponseData(200, GROUNDED_RESPONSE);
        });

        service.summarizeImage(new byte[] { 1, 2 }, "image/png", "Describe this");

        JsonObject payload = JsonParser.parseString(body(captured.get())).getAsJsonObject();
        assertFalse(payload.has("tools"));
        assertTrue(payload.getAsJsonArray("input").get(1).getAsJsonObject()
                .get("data").getAsString().equals("AQI="));
        assertEquals("image/png", payload.getAsJsonArray("input").get(1)
                .getAsJsonObject().get("mime_type").getAsString());
    }

    private GeminiAnswerService service(String apiKey, GeminiAnswerService.Transport transport) {
        return new GeminiAnswerService(apiKey, null, transport);
    }

    private String body(HttpRequest request) {
        StringBuilder result = new StringBuilder();
        CompletableFuture<String> completed = new CompletableFuture<>();
        request.bodyPublisher().orElseThrow(AssertionError::new).subscribe(new Flow.Subscriber<ByteBuffer>() {
            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                subscription.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(ByteBuffer buffer) {
                byte[] bytes = new byte[buffer.remaining()];
                buffer.get(bytes);
                result.append(new String(bytes, StandardCharsets.UTF_8));
            }

            @Override
            public void onError(Throwable error) {
                completed.completeExceptionally(error);
            }

            @Override
            public void onComplete() {
                completed.complete(result.toString());
            }
        });
        return completed.join();
    }
}
