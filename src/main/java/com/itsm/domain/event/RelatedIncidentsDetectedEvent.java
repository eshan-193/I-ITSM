package com.itsm.domain.event;

import com.itsm.domain.incident.Incident;
import com.itsm.domain.notification.Severity;
import com.itsm.domain.user.User;

import java.time.LocalDateTime;
import java.util.Set;

/** The correlation engine linked this incident to others (possible duplicate, shared root cause, or outage cluster). */
public class RelatedIncidentsDetectedEvent extends IncidentEvent {

    private final String finding;
    private final boolean majorIncident;

    public RelatedIncidentsDetectedEvent(Incident incident, LocalDateTime at, String finding, boolean majorIncident) {
        super(incident, null, at, incident.getStatus(), incident.getStatus());
        this.finding = finding;
        this.majorIncident = majorIncident;
    }

    @Override
    public String auditAction() {
        return majorIncident ? "MAJOR_INCIDENT_SUSPECTED" : "CORRELATED";
    }

    @Override
    public String describe() {
        return finding;
    }

    @Override
    public Set<User> recipients() {
        return assigneeOnly();
    }

    @Override
    public boolean notifyAdmins() {
        return majorIncident;
    }

    @Override
    public Severity severity() {
        return majorIncident ? Severity.CRITICAL : Severity.WARNING;
    }
}
