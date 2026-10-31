package com.itsm.domain.notification;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * A record of an email or SMS the system sent (or simulated).
 * The admin "Outbox" page lists these, which makes simulated delivery visible in the demo.
 */
@Entity
@Table(name = "outbound_messages")
public class OutboundMessage {

    public enum DeliveryStatus { SIMULATED, SENT, FAILED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ChannelType channel;

    @Column(name = "recipient_address", nullable = false, length = 120)
    private String recipientAddress;

    @Column(length = 200)
    private String subject;

    @Column(nullable = false, length = 2000)
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private DeliveryStatus status;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected OutboundMessage() {
        // for JPA
    }

    public OutboundMessage(ChannelType channel, String recipientAddress, String subject, String body, LocalDateTime createdAt) {
        this.channel = channel;
        this.recipientAddress = recipientAddress;
        this.subject = subject;
        this.body = body;
        this.status = DeliveryStatus.SIMULATED;
        this.createdAt = createdAt;
    }

    public void markSent() {
        this.status = DeliveryStatus.SENT;
    }

    public void markFailed(String error) {
        this.status = DeliveryStatus.FAILED;
        this.errorMessage = error == null ? null : error.substring(0, Math.min(error.length(), 500));
    }

    public Long getId() { return id; }
    public ChannelType getChannel() { return channel; }
    public String getRecipientAddress() { return recipientAddress; }
    public String getSubject() { return subject; }
    public String getBody() { return body; }
    public DeliveryStatus getStatus() { return status; }
    public String getErrorMessage() { return errorMessage; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
