package com.itsm.domain.event;

import com.itsm.domain.incident.Incident;
import com.itsm.domain.incident.IncidentStatus;
import com.itsm.domain.user.User;

import java.time.LocalDateTime;
import java.util.Set;

public class IncidentCreatedEvent extends IncidentEvent {

    public IncidentCreatedEvent(Incident incident, User actor, LocalDateTime at) {
        super(incident, actor, at, null, IncidentStatus.NEW);
    }

    @Override
    public String auditAction() {
        return "CREATED";
    }

    @Override
    public String describe() {
        return "Incident created by " + getActor().getName() + " for service " + getIncident().getService().getName();
    }

    @Override
    public Set<User> recipients() {
        return Set.of(getIncident().getReporter());
    }

    @Override
    public String notificationMessage() {
        return "Your incident " + getIncident().getNumber() + " \"" + getIncident().getTitle() + "\" was received";
    }
}
