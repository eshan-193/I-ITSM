package com.itsm.notification;

import com.itsm.domain.notification.ChannelType;
import com.itsm.domain.notification.Notification;
import com.itsm.repository.NotificationRepository;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;

/** Stores a notification for the bell icon. */
@Component
public class InAppNotificationChannel implements NotificationChannel {

    private final NotificationRepository repository;
    private final Clock clock;

    public InAppNotificationChannel(NotificationRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    public ChannelType type() {
        return ChannelType.IN_APP;
    }

    @Override
    public void send(NotificationMessage message) {
        repository.save(new Notification(message.recipient(), message.incident(), message.body(),
                message.severity(), LocalDateTime.now(clock)));
    }
}
