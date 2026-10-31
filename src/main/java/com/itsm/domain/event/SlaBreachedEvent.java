package com.itsm.domain.event;

import com.itsm.domain.incident.Incident;
import com.itsm.domain.notification.Severity;
import com.itsm.domain.sla.SlaBreachType;
import com.itsm.domain.user.User;

import java.time.LocalDateTime;
import java.util.Set;

public class SlaBreachedEvent extends IncidentEvent {

    private final SlaBreachType breachType;

    public SlaBreachedEvent(Incident incident, LocalDateTime at, SlaBreachType breachType) {
        super(incident, null, at, incident.getStatus(), incident.getStatus());
        this.breachType = breachType;
    }

    @Override
    public String auditAction() {
        return "SLA_BREACHED";
    }

    @Override
    public String describe() {
        return breachType.getLabel() + " SLA violated for " + getIncident().getPriority() + " incident";
    }

    @Override
    public Set<User> recipients() {
        return assigneeOnly();
    }

    @Override
    public boolean notifyAdmins() {
        return true;
    }

    @Override
    public Severity severity() {
        return Severity.CRITICAL;
    }

    public SlaBreachType getBreachType() {
        return breachType;
    }
}
