package com.itsm.domain.event;

import com.itsm.domain.incident.Incident;
import com.itsm.domain.notification.Severity;
import com.itsm.domain.user.User;

import java.time.LocalDateTime;
import java.util.Set;

/** An incident moved up the escalation chain (Technician → Team Lead → IT Manager). */
public class IncidentEscalatedEvent extends IncidentEvent {

    private final Set<User> escalatedTo;
    private final String reason;

    public IncidentEscalatedEvent(Incident incident, LocalDateTime at, Set<User> escalatedTo, String reason) {
        super(incident, null, at, incident.getStatus(), incident.getStatus());
        this.escalatedTo = Set.copyOf(escalatedTo);
        this.reason = reason;
    }

    @Override
    public String auditAction() {
        return "ESCALATED";
    }

    @Override
    public String describe() {
        return getIncident().getEscalationLabel() + ": " + reason;
    }

    @Override
    public Set<User> recipients() {
        return escalatedTo;
    }

    @Override
    public Severity severity() {
        return Severity.CRITICAL;
    }
}
