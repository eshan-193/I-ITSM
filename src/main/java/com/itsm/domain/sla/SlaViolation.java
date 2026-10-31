package com.itsm.domain.sla;

import com.itsm.domain.incident.Incident;
import com.itsm.domain.priority.Priority;
import com.itsm.domain.user.Technician;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/** A permanent record of one SLA breach, used by the violations report. */
@Entity
@Table(name = "sla_violations")
public class SlaViolation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "incident_id")
    private Incident incident;

    @Enumerated(EnumType.STRING)
    @Column(name = "breach_type", nullable = false, length = 20)
    private SlaBreachType breachType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 5)
    private Priority priority;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "technician_id")
    private Technician technician;

    @Column(nullable = false)
    private LocalDateTime deadline;

    @Column(name = "detected_at", nullable = false)
    private LocalDateTime detectedAt;

    protected SlaViolation() {
        // for JPA
    }

    public SlaViolation(Incident incident, SlaBreachType breachType, LocalDateTime detectedAt) {
        this.incident = incident;
        this.breachType = breachType;
        this.priority = incident.getPriority();
        this.technician = incident.getAssignee();
        this.deadline = breachType == SlaBreachType.RESPONSE
                ? incident.getSla().getResponseDeadline()
                : incident.getSla().getResolutionDeadline();
        this.detectedAt = detectedAt;
    }

    public Long getId() { return id; }
    public Incident getIncident() { return incident; }
    public SlaBreachType getBreachType() { return breachType; }
    public Priority getPriority() { return priority; }
    public Technician getTechnician() { return technician; }
    public LocalDateTime getDeadline() { return deadline; }
    public LocalDateTime getDetectedAt() { return detectedAt; }
}
