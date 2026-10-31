package com.itsm.domain.incident;

import com.itsm.domain.user.User;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * One immutable line of an incident's history, e.g.
 * "10:07 – Assigned to Technician T205 (CLASSIFIED → ASSIGNED)".
 * <p>
 * There are no setters: audit records must never change after they are written.
 */
@Entity
@Table(name = "audit_entries")
public class AuditEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "incident_id")
    private Incident incident;

    /** Null when the action was performed by the system (scheduler, auto-assignment). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id")
    private User actor;

    /** Snapshot of the actor's name, so the log stays readable even if the user is renamed. */
    @Column(name = "actor_name", nullable = false, length = 120)
    private String actorName;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(nullable = false, length = 40)
    private String action;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 20)
    private IncidentStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", length = 20)
    private IncidentStatus toStatus;

    @Column(length = 1000)
    private String description;

    protected AuditEntry() {
        // for JPA
    }

    public AuditEntry(Incident incident, User actor, LocalDateTime timestamp, String action,
                      IncidentStatus fromStatus, IncidentStatus toStatus, String description) {
        this.incident = incident;
        this.actor = actor;
        this.actorName = actor == null ? "System" : actor.getName() + " (" + actor.getCode() + ")";
        this.timestamp = timestamp;
        this.action = action;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.description = description;
    }

    public boolean isStatusChange() {
        return fromStatus != null && toStatus != null && fromStatus != toStatus;
    }

    public Long getId() { return id; }
    public Incident getIncident() { return incident; }
    public User getActor() { return actor; }
    public String getActorName() { return actorName; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getAction() { return action; }
    public IncidentStatus getFromStatus() { return fromStatus; }
    public IncidentStatus getToStatus() { return toStatus; }
    public String getDescription() { return description; }
}
