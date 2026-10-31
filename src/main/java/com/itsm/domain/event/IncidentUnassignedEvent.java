package com.itsm.domain.event;

import com.itsm.domain.incident.Incident;
import com.itsm.domain.notification.Severity;

import java.time.LocalDateTime;

/** Automatic assignment found nobody available; an administrator must step in. */
public class IncidentUnassignedEvent extends IncidentEvent {

    private final String strategyName;

    public IncidentUnassignedEvent(Incident incident, LocalDateTime at, String strategyName) {
        super(incident, null, at, incident.getStatus(), incident.getStatus());
        this.strategyName = strategyName;
    }

    @Override
    public String auditAction() {
        return "ASSIGNMENT_FAILED";
    }

    @Override
    public String describe() {
        return "No available technician found by " + strategyName + " strategy; manual assignment needed";
    }

    @Override
    public boolean notifyAdmins() {
        return true;
    }

    @Override
    public Severity severity() {
        return Severity.WARNING;
    }
}
