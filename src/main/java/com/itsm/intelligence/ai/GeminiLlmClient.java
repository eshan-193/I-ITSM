package com.itsm.intelligence.ai;

import com.itsm.exception.ExternalServiceException;
import com.itsm.service.SettingKey;
import com.itsm.service.SettingsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for Google's Gemini REST API ({@code models/{model}:generateContent}).
 * Converts our simple {@link LlmClient} call into Gemini's request/response shape.
 * <p>
 * Free-tier models are often overloaded, so each call tries the main model and then the
 * fallback models. If all of them fail, a circuit breaker skips AI for a cooldown period:
 * callers see {@link #isAvailable()} as false and use the offline algorithms straight away
 * instead of waiting on a provider that is down. When a fallback model answers, it is
 * tried first for the same cooldown period so later calls do not wait on the slow main model.
 */
@Component
public class GeminiLlmClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(GeminiLlmClient.class);

    private final GeminiProperties properties;
    private final SettingsService settings;
    private final Clock clock;
    private final JsonMapper json = JsonMapper.builder().build();
    private final HttpClient http;

    private static final Duration MIN_ATTEMPT = Duration.ofSeconds(2);

    private volatile Instant skipUntil = Instant.MIN;
    private volatile String lastWorkingModel;
    private volatile Instant preferLastWorkingUntil = Instant.MIN;

    public GeminiLlmClient(GeminiProperties properties, SettingsService settings, Clock clock) {
        this.properties = properties;
        this.settings = settings;
        this.clock = clock;
        this.http = HttpClient.newBuilder().connectTimeout(properties.timeout()).build();
    }

    @Override
    public boolean isAvailable() {
        return properties.hasKey() && settings.getBoolean(SettingKey.AI_ENABLED)
                && !clock.instant().isBefore(skipUntil);
    }

    @Override
    public String providerName() {
        return "Google Gemini (" + (lastWorkingModel != null ? lastWorkingModel : properties.model()) + ")";
    }

    @Override
    public String generateJson(String prompt) {
        if (!clock.instant().isBefore(skipUntil)) {
            ExternalServiceException last = null;
            Instant deadline = clock.instant().plus(properties.totalTimeout());
            for (String model : modelsToTry()) {
                Duration left = Duration.between(clock.instant(), deadline);
                if (left.compareTo(MIN_ATTEMPT) < 0) {
                    break;
                }
                Duration timeout = left.compareTo(properties.timeout()) < 0 ? left : properties.timeout();
                try {
                    String reply = call(model, prompt, properties.thinkingLevel(), timeout);
                    if (!model.equals(properties.model()) && !model.equals(lastWorkingModel)) {
                        preferLastWorkingUntil = clock.instant().plus(properties.cooldown());
                    }
                    lastWorkingModel = model;
                    return reply;
                } catch (ExternalServiceException e) {
                    log.warn("Gemini model {} failed: {}", model, e.getMessage());
                    last = e;
                }
            }
            skipUntil = clock.instant().plus(properties.cooldown());
            log.warn("All Gemini models failed; using offline algorithms for {}", properties.cooldown());
            throw last != null ? last : new ExternalServiceException("AI provider timed out", null);
        }
        throw new ExternalServiceException("AI provider temporarily skipped after recent failures", null);
    }

    private List<String> modelsToTry() {
        List<String> models = new ArrayList<>(properties.modelsInOrder());
        String preferred = lastWorkingModel;
        if (preferred != null && clock.instant().isBefore(preferLastWorkingUntil) && models.remove(preferred)) {
            models.addFirst(preferred);
        }
        return models;
    }

    private String call(String model, String prompt, String thinkingLevel, Duration timeout) {
        ObjectNode body = json.createObjectNode();
        ArrayNode contents = body.putArray("contents");
        contents.addObject().putArray("parts").addObject().put("text", prompt);
        ObjectNode config = body.putObject("generationConfig");
        config.put("responseMimeType", "application/json");
        config.put("temperature", 0.2);
        if (!thinkingLevel.isBlank()) {
            config.putObject("thinkingConfig").put("thinkingLevel", thinkingLevel);
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(properties.baseUrl() + "/models/" + model + ":generateContent"))
                .timeout(timeout)
                .header("Content-Type", "application/json")
                .header("x-goog-api-key", properties.apiKey())
                .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)))
                .build();
        try {
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 400 && !thinkingLevel.isBlank() && response.body().contains("hinking")) {
                return call(model, prompt, "", timeout); // model does not support thinking levels
            }
            if (response.statusCode() != 200) {
                throw new ExternalServiceException("HTTP " + response.statusCode() + " " + abbreviate(response.body()), null);
            }
            // Thinking models may return several parts; keep the answer text, skip "thought" parts.
            StringBuilder text = new StringBuilder();
            for (JsonNode part : json.readTree(response.body())
                    .path("candidates").path(0).path("content").path("parts")) {
                if (!part.path("thought").asBoolean(false) && part.has("text")) {
                    text.append(part.path("text").asString());
                }
            }
            if (text.isEmpty()) {
                throw new ExternalServiceException("AI provider returned no content", null);
            }
            return stripCodeFence(text.toString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ExternalServiceException("AI request interrupted", e);
        } catch (ExternalServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ExternalServiceException("AI provider unreachable: " + e, e);
        }
    }

    static String stripCodeFence(String text) {
        String t = text.trim();
        if (t.startsWith("```")) {
            t = t.replaceFirst("^```(json)?", "").replaceFirst("```$", "").trim();
        }
        return t;
    }

    private static String abbreviate(String s) {
        return s == null ? "" : s.substring(0, Math.min(200, s.length())).replaceAll("\\s+", " ");
    }
}
