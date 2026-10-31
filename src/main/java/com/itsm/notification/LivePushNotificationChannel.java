package com.itsm.notification;

import com.itsm.domain.notification.ChannelType;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/** Pushes a toast to the recipient's open browser tabs, after the transaction commits. */
@Component
public class LivePushNotificationChannel implements NotificationChannel {

    private final LiveUpdateHub hub;

    public LivePushNotificationChannel(LiveUpdateHub hub) {
        this.hub = hub;
    }

    @Override
    public ChannelType type() {
        return ChannelType.LIVE;
    }

    @Override
    public void send(NotificationMessage message) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("message", message.body());
        payload.put("severity", message.severity().getColor());
        if (message.incident() != null) {
            payload.put("incidentId", message.incident().getId());
            payload.put("incidentNumber", message.incident().getNumber());
        }
        Long userId = message.recipient().getId();
        AfterCommit.run(() -> hub.sendToUser(userId, "notification", payload));
    }
}
