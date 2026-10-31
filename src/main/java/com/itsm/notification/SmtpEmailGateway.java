package com.itsm.notification;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * Adapter pattern: wraps Spring's {@link JavaMailSender} (a third-party API with its own
 * message types) behind our tiny {@link EmailGateway} interface. Enabled only when
 * {@code itsm.mail.enabled=true} and SMTP settings are provided (e.g. a Gmail app password).
 */
@Component
public class SmtpEmailGateway implements EmailGateway {

    private final ObjectProvider<JavaMailSender> mailSender;
    private final boolean enabled;
    private final String from;

    public SmtpEmailGateway(ObjectProvider<JavaMailSender> mailSender,
                            @Value("${itsm.mail.enabled:false}") boolean enabled,
                            @Value("${itsm.mail.from:itsm@localhost}") String from) {
        this.mailSender = mailSender;
        this.enabled = enabled;
        this.from = from;
    }

    @Override
    public boolean isEnabled() {
        return enabled && mailSender.getIfAvailable() != null;
    }

    @Override
    public void send(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        mailSender.getObject().send(message);
    }
}
