package com.itsm.notification;

import com.itsm.domain.event.IncidentEvent;
import com.itsm.domain.notification.ChannelType;
import com.itsm.domain.user.User;
import com.itsm.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Observer: turns incident events into notifications. The event decides who and how
 * (polymorphism); this class only resolves recipients and asks the factory for each channel.
 */
@Component
public class NotificationListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationListener.class);

    private final NotificationChannelFactory channels;
    private final UserRepository users;

    public NotificationListener(NotificationChannelFactory channels, UserRepository users) {
        this.channels = channels;
        this.users = users;
    }

    @EventListener
    @Order(2)
    public void on(IncidentEvent event) {
        Set<User> recipients = new LinkedHashSet<>(event.recipients());
        if (event.notifyAdmins()) {
            recipients.addAll(users.findActiveAdministrators());
        }
        if (event.getActor() != null) {
            recipients.remove(event.getActor());
        }
        recipients.removeIf(u -> u == null || !u.isActive());
        if (recipients.isEmpty()) {
            return;
        }
        String subject = "[" + event.getIncident().getNumber() + "] " + event.auditAction().replace('_', ' ');
        for (User recipient : recipients) {
            NotificationMessage message = new NotificationMessage(recipient, event.getIncident(), subject,
                    event.notificationMessage(), event.severity());
            for (ChannelType type : event.channels()) {
                channels.get(type).ifPresent(channel -> deliver(channel, message));
            }
        }
    }

    /** One failing channel must not stop the others or roll back the business change. */
    private void deliver(NotificationChannel channel, NotificationMessage message) {
        try {
            channel.send(message);
        } catch (RuntimeException e) {
            log.warn("{} notification to {} failed: {}", channel.type(), message.recipient().getEmail(), e.getMessage());
        }
    }
}
