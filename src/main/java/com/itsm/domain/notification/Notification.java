package com.itsm.domain.notification;

import com.itsm.domain.incident.Incident;
import com.itsm.domain.user.User;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/** An in-app notification shown under the bell icon. */
@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_id")
    private User recipient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_id")
    private Incident incident;

    @Column(nullable = false, length = 500)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Severity severity;

    @Column(name = "is_read", nullable = false)
    private boolean read;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected Notification() {
        // for JPA
    }

    public Notification(User recipient, Incident incident, String message, Severity severity, LocalDateTime createdAt) {
        this.recipient = recipient;
        this.incident = incident;
        this.message = message;
        this.severity = severity;
        this.createdAt = createdAt;
    }

    public void markRead() {
        this.read = true;
    }

    public Long getId() { return id; }
    public User getRecipient() { return recipient; }
    public Incident getIncident() { return incident; }
    public String getMessage() { return message; }
    public Severity getSeverity() { return severity; }
    public boolean isRead() { return read; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
