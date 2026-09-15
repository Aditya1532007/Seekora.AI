package com.seekora.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Gson helpers shared by every servlet. */
public final class JsonUtil {

    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class,
                    (com.google.gson.JsonSerializer<LocalDateTime>) (src, type, ctx) ->
                            src == null ? null : new com.google.gson.JsonPrimitive(
                                    src.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)))
            .create();

    private JsonUtil() {
    }

    public static Gson gson() {
        return GSON;
    }

    public static String toJson(Object value) {
        return GSON.toJson(value);
    }

    /**
     * Reads the request body as a JSON object. Falls back to normal form
     * parameters so the API also works with plain HTML form posts.
     */
    public static JsonObject readBody(HttpServletRequest request) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        String body = sb.toString().trim();
        if (body.isEmpty()) {
            JsonObject fromForm = new JsonObject();
            request.getParameterMap().forEach((key, values) -> {
                if (values.length > 0) {
                    fromForm.addProperty(key, values[0]);
                }
            });
            return fromForm;
        }
        try {
            return JsonParser.parseString(body).getAsJsonObject();
        } catch (JsonSyntaxException | IllegalStateException e) {
            return new JsonObject();
        }
    }

    public static String string(JsonObject json, String key) {
        return json.has(key) && !json.get(key).isJsonNull() ? json.get(key).getAsString().trim() : null;
    }

    public static int integer(JsonObject json, String key, int fallback) {
        try {
            return json.has(key) && !json.get(key).isJsonNull() ? json.get(key).getAsInt() : fallback;
        } catch (NumberFormatException | UnsupportedOperationException e) {
            return fallback;
        }
    }
}
