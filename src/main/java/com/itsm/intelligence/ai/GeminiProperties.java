package com.itsm.intelligence.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;
import java.util.stream.Stream;

/**
 * Settings under {@code itsm.ai.gemini.*}. The API key comes from the
 * {@code GEMINI_API_KEY} environment variable and is never committed.
 *
 * @param fallbackModels tried in order when the main model is overloaded or unavailable
 * @param totalTimeout   upper bound for one AI request across all models tried
 * @param thinkingLevel  "low" keeps thinking models fast; blank uses the model's default
 * @param cooldown       after every model fails, AI is skipped for this long (circuit breaker)
 */
@ConfigurationProperties(prefix = "itsm.ai.gemini")
public record GeminiProperties(String apiKey, String model, List<String> fallbackModels, String baseUrl,
                               Duration timeout, Duration totalTimeout, String thinkingLevel, Duration cooldown) {

    public GeminiProperties {
        if (model == null || model.isBlank()) model = "gemini-3.8-flash";
        String main = model;
        fallbackModels = fallbackModels == null ? List.of() : fallbackModels.stream()
                .map(String::trim).filter(m -> !m.isEmpty() && !m.equals(main)).toList();
        if (baseUrl == null || baseUrl.isBlank()) baseUrl = "https://generativelanguage.googleapis.com/v1beta";
        if (timeout == null) timeout = Duration.ofSeconds(15);
        if (totalTimeout == null) totalTimeout = Duration.ofSeconds(20);
        if (thinkingLevel == null) thinkingLevel = "low";
        if (cooldown == null) cooldown = Duration.ofMinutes(2);
    }

    public boolean hasKey() {
        return apiKey != null && !apiKey.isBlank();
    }

    /** Main model first, then the fallbacks. */
    public List<String> modelsInOrder() {
        return Stream.concat(Stream.of(model), fallbackModels.stream()).toList();
    }
}
