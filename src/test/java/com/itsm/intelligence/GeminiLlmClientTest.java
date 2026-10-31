package com.itsm.intelligence;

import com.itsm.exception.ExternalServiceException;
import com.itsm.intelligence.ai.GeminiLlmClient;
import com.itsm.intelligence.ai.GeminiProperties;
import com.itsm.service.SettingKey;
import com.itsm.service.SettingsService;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Model fallback and circuit breaker, against a fake Gemini server on localhost. */
class GeminiLlmClientTest {

    private static final String OK = """
            {"candidates":[{"content":{"parts":[{"text":"thinking...","thought":true},{"text":"{\\"ok\\":true}"}]}}]}""";

    private HttpServer server;
    private final AtomicInteger calls = new AtomicInteger();
    private final SettingsService settings = mock(SettingsService.class);

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/models/", exchange -> {
            calls.incrementAndGet();
            String path = exchange.getRequestURI().getPath();
            boolean up = path.contains("healthy-model");
            byte[] body = (up ? OK : "{\"error\":{\"message\":\"high demand\"}}").getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(up ? 200 : 503, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        when(settings.getBoolean(SettingKey.AI_ENABLED)).thenReturn(true);
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    private GeminiLlmClient client(String model, List<String> fallbacks) {
        String url = "http://127.0.0.1:" + server.getAddress().getPort();
        GeminiProperties props = new GeminiProperties("test-key", model, fallbacks, url,
                Duration.ofSeconds(5), Duration.ofSeconds(20), "low", Duration.ofMinutes(2));
        return new GeminiLlmClient(props, settings, Clock.fixed(Instant.parse("2026-11-01T10:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void fallsBackToNextModelWhenMainModelIsOverloaded() {
        GeminiLlmClient ai = client("busy-model", List.of("healthy-model"));

        assertThat(ai.generateJson("hi")).isEqualTo("{\"ok\":true}");
        assertThat(ai.providerName()).contains("healthy-model");
        assertThat(ai.isAvailable()).isTrue();

        // the working fallback is now tried first, so the busy model is not called again
        calls.set(0);
        ai.generateJson("again");
        assertThat(calls).hasValue(1);
    }

    @Test
    void circuitBreakerSkipsAiAfterEveryModelFails() {
        GeminiLlmClient ai = client("busy-model", List.of("other-busy-model"));

        assertThatThrownBy(() -> ai.generateJson("hi")).isInstanceOf(ExternalServiceException.class);
        assertThat(calls).hasValue(2);
        assertThat(ai.isAvailable()).isFalse();

        // further calls fail fast without touching the network
        assertThatThrownBy(() -> ai.generateJson("hi")).isInstanceOf(ExternalServiceException.class);
        assertThat(calls).hasValue(2);
    }
}
