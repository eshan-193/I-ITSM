package com.itsm.notification;

import com.itsm.domain.notification.ChannelType;
import com.itsm.domain.notification.OutboundMessage;
import com.itsm.repository.OutboundMessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Simulated SMS for critical events. A real provider (e.g. Twilio) would be plugged in
 * through an adapter exactly like {@link SmtpEmailGateway}.
 */
@Component
public class SmsNotificationChannel implements NotificationChannel {

    private static final Logger log = LoggerFactory.getLogger(SmsNotificationChannel.class);
    private static final int SMS_LENGTH = 160;

    private final OutboundMessageRepository outbox;
    private final Clock clock;

    public SmsNotificationChannel(OutboundMessageRepository outbox, Clock clock) {
        this.outbox = outbox;
        this.clock = clock;
    }

    @Override
    public ChannelType type() {
        return ChannelType.SMS;
    }

    @Override
    public void send(NotificationMessage message) {
        String phone = message.recipient().getPhone();
        if (phone == null || phone.isBlank()) {
            return; // no number on file
        }
        String text = "[I-ITSM] " + message.body();
        if (text.length() > SMS_LENGTH) {
            text = text.substring(0, SMS_LENGTH - 3) + "...";
        }
        outbox.save(new OutboundMessage(ChannelType.SMS, phone, null, text, LocalDateTime.now(clock)));
        log.info("[SIMULATED SMS] to={} text=\"{}\"", phone, text);
    }
}
