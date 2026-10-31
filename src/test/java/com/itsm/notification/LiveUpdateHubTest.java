package com.itsm.notification;

import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class LiveUpdateHubTest {

    @Test
    void deadBrowserConnectionIsDroppedWithoutBreakingTheBroadcast() {
        LiveUpdateHub hub = new LiveUpdateHub();
        SseEmitter closedTab = hub.subscribe(1L);
        hub.subscribe(2L);
        closedTab.completeWithError(new IOException("Broken pipe"));

        assertThatCode(() -> hub.broadcast("dashboard", Map.of())).doesNotThrowAnyException();
        assertThat(hub.connectionCount()).isEqualTo(1);
    }
}
