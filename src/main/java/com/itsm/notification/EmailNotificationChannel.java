package com.itsm.notification;

import com.itsm.domain.notification.ChannelType;
import com.itsm.domain.notification.OutboundMessage;
import com.itsm.repository.OutboundMessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Email notifications. Every email is recorded in the outbox; it is really sent only
 * when an SMTP gateway is configured, otherwise it stays "simulated" (visible on the
 * admin Outbox page and in the log).
 */
@Component
public class EmailNotificationChannel implements NotificationChannel {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationChannel.class);

    private final OutboundMessageRepository outbox;
    private final EmailGateway gateway;
    private final EmailDeliveryWorker worker;
    private final Clock clock;
    private final String baseUrl;

    public EmailNotificationChannel(OutboundMessageRepository outbox, EmailGateway gateway, EmailDeliveryWorker worker,
                                    Clock clock, @Value("${itsm.base-url:http://localhost:8080}") String baseUrl) {
        this.outbox = outbox;
        this.gateway = gateway;
        this.worker = worker;
        this.clock = clock;
        this.baseUrl = baseUrl;
    }

    @Override
    public ChannelType type() {
        return ChannelType.EMAIL;
    }

    @Override
    public void send(NotificationMessage message) {
        String link = message.incident() == null ? "" : "\n\nOpen: " + baseUrl + "/incidents/" + message.incident().getId();
        String body = "Hello " + message.recipient().getName() + ",\n\n" + message.body() + link + "\n\n— I-ITSM Service Desk";
        OutboundMessage saved = outbox.save(new OutboundMessage(ChannelType.EMAIL, message.recipient().getEmail(),
                message.subject(), body, LocalDateTime.now(clock)));
        if (gateway.isEnabled()) {
            Long id = saved.getId();
            AfterCommit.run(() -> worker.deliver(id));
        } else {
            log.info("[SIMULATED EMAIL] to={} subject=\"{}\"", message.recipient().getEmail(), message.subject());
        }
    }
}
