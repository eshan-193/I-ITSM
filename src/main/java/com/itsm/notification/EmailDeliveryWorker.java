package com.itsm.notification;

import com.itsm.domain.notification.OutboundMessage;
import com.itsm.repository.OutboundMessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Sends queued emails on a background thread so a slow mail server never blocks a request. */
@Component
public class EmailDeliveryWorker {

    private static final Logger log = LoggerFactory.getLogger(EmailDeliveryWorker.class);

    private final OutboundMessageRepository outbox;
    private final EmailGateway gateway;

    public EmailDeliveryWorker(OutboundMessageRepository outbox, EmailGateway gateway) {
        this.outbox = outbox;
        this.gateway = gateway;
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deliver(Long messageId) {
        OutboundMessage message = outbox.findById(messageId).orElse(null);
        if (message == null) {
            return;
        }
        try {
            gateway.send(message.getRecipientAddress(), message.getSubject(), message.getBody());
            message.markSent();
        } catch (RuntimeException e) {
            log.warn("Email to {} failed: {}", message.getRecipientAddress(), e.getMessage());
            message.markFailed(e.getMessage());
        }
    }
}
