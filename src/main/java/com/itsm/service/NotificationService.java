package com.itsm.service;

import com.itsm.domain.notification.Notification;
import com.itsm.domain.notification.OutboundMessage;
import com.itsm.domain.user.User;
import com.itsm.exception.ResourceNotFoundException;
import com.itsm.exception.UnauthorizedOperationException;
import com.itsm.repository.NotificationRepository;
import com.itsm.repository.OutboundMessageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Reading and acknowledging in-app notifications; viewing the email/SMS outbox. */
@Service
@Transactional
public class NotificationService {

    private final NotificationRepository notifications;
    private final OutboundMessageRepository outbox;

    public NotificationService(NotificationRepository notifications, OutboundMessageRepository outbox) {
        this.notifications = notifications;
        this.outbox = outbox;
    }

    @Transactional(readOnly = true)
    public List<Notification> latest(User user) {
        return notifications.findTop50ByRecipientOrderByCreatedAtDesc(user);
    }

    @Transactional(readOnly = true)
    public List<Notification> preview(User user) {
        return notifications.findTop8ByRecipientOrderByCreatedAtDesc(user);
    }

    @Transactional(readOnly = true)
    public long unreadCount(User user) {
        return notifications.countByRecipientAndReadFalse(user);
    }

    /** Marks one notification read and returns the incident it points to (if any). */
    public Long open(Long notificationId, User user) {
        Notification n = notifications.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", notificationId));
        if (!n.getRecipient().equals(user)) {
            throw new UnauthorizedOperationException("This notification belongs to someone else");
        }
        n.markRead();
        return n.getIncident() == null ? null : n.getIncident().getId();
    }

    public void markAllRead(User user) {
        notifications.markAllRead(user);
    }

    @Transactional(readOnly = true)
    public List<OutboundMessage> outbox() {
        return outbox.findTop200ByOrderByCreatedAtDesc();
    }
}
