package com.itsm.domain.event;

import com.itsm.domain.incident.Incident;
import com.itsm.domain.user.User;

import java.time.LocalDateTime;
import java.util.Set;

/** Another employee said "this affects me too" (duplicate report folded into the original). */
public class WatcherAddedEvent extends IncidentEvent {

    public WatcherAddedEvent(Incident incident, User actor, LocalDateTime at) {
        super(incident, actor, at, incident.getStatus(), incident.getStatus());
    }

    @Override
    public String auditAction() {
        return "AFFECTED_USER_ADDED";
    }

    @Override
    public String describe() {
        return getActor().getName() + " reported the same problem; " + getIncident().getAffectedUsers() + " users now affected";
    }

    @Override
    public Set<User> recipients() {
        return assigneeOnly();
    }
}
