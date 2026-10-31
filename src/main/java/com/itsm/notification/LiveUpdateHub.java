package com.itsm.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Keeps open Server-Sent-Event connections per user and pushes events to browsers in
 * real time (toast notifications, live dashboard refresh). Thread-safe collections are
 * used because requests and the scheduler touch the map concurrently.
 */
@Component
public class LiveUpdateHub {

    private static final Logger log = LoggerFactory.getLogger(LiveUpdateHub.class);
    private static final long TIMEOUT_MS = 30 * 60 * 1000L;

    private final Map<Long, List<SseEmitter>> emittersByUser = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Long userId) {
        SseEmitter emitter = new SseEmitter(TIMEOUT_MS);
        List<SseEmitter> list = emittersByUser.computeIfAbsent(userId, id -> new CopyOnWriteArrayList<>());
        list.add(emitter);
        Runnable remove = () -> list.remove(emitter);
        emitter.onCompletion(remove);
        emitter.onTimeout(remove);
        emitter.onError(e -> remove.run());
        send(emitter, "hello", Map.of("connected", true));
        return emitter;
    }

    public void sendToUser(Long userId, String eventName, Object payload) {
        emittersByUser.getOrDefault(userId, List.of()).forEach(e -> send(e, eventName, payload));
    }

    public void broadcast(String eventName, Object payload) {
        emittersByUser.values().forEach(list -> list.forEach(e -> send(e, eventName, payload)));
    }

    public int connectionCount() {
        return emittersByUser.values().stream().mapToInt(List::size).sum();
    }

    /** Keeps proxies from closing idle connections. */
    @Scheduled(fixedDelay = 25_000)
    public void heartbeat() {
        broadcast("ping", Map.of());
    }

    private void send(SseEmitter emitter, String name, Object payload) {
        try {
            emitter.send(SseEmitter.event().name(name).data(payload));
        } catch (IOException | IllegalStateException e) {
            log.debug("Dropping closed SSE connection: {}", e.getMessage());
            emitter.complete();
        }
    }
}
