package com.itsm.domain.event;

import com.itsm.domain.incident.Incident;
import com.itsm.domain.notification.Severity;
import com.itsm.domain.user.User;

import java.time.LocalDateTime;
import java.util.Set;

/** The resolution deadline is approaching (configurable threshold, 75% by default). */
public class SlaWarningEvent extends IncidentEvent {

    private final long minutesLeft;

    public SlaWarningEvent(Incident incident, LocalDateTime at, long minutesLeft) {
        super(incident, null, at, incident.getStatus(), incident.getStatus());
        this.minutesLeft = minutesLeft;
    }

    @Override
    public String auditAction() {
        return "SLA_WARNING";
    }

    @Override
    public String describe() {
        return "SLA warning: resolution due in " + minutesLeft + " min";
    }

    @Override
    public Set<User> recipients() {
        return assigneeOnly();
    }

    @Override
    public Severity severity() {
        return Severity.WARNING;
    }
}
