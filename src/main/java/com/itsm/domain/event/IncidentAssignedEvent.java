package com.itsm.domain.event;

import com.itsm.domain.incident.Incident;
import com.itsm.domain.incident.IncidentStatus;
import com.itsm.domain.notification.ChannelType;
import com.itsm.domain.notification.Severity;
import com.itsm.domain.priority.Priority;
import com.itsm.domain.user.User;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Set;

public class IncidentAssignedEvent extends IncidentEvent {

    private final String strategyName;
    private final String reason;

    public IncidentAssignedEvent(Incident incident, User actor, LocalDateTime at, IncidentStatus from,
                                 String strategyName, String reason) {
        super(incident, actor, at, from, incident.getStatus());
        this.strategyName = strategyName;
        this.reason = reason;
    }

    @Override
    public String auditAction() {
        return "ASSIGNED";
    }

    @Override
    public String describe() {
        String how = strategyName == null ? "manually" : "by " + strategyName + " strategy";
        return "Assigned to " + getIncident().getAssignee() + " " + how + (reason == null ? "" : " (" + reason + ")");
    }

    @Override
    public Set<User> recipients() {
        Set<User> users = new LinkedHashSet<>(assigneeOnly());
        users.add(getIncident().getReporter());
        return users;
    }

    @Override
    public Severity severity() {
        return getIncident().getPriority() == Priority.P1 ? Severity.CRITICAL : Severity.INFO;
    }

    /** The technician always gets an email for a new assignment. */
    @Override
    public Set<ChannelType> channels() {
        Set<ChannelType> channels = EnumSet.copyOf(super.channels());
        channels.add(ChannelType.EMAIL);
        return channels;
    }

    @Override
    public String notificationMessage() {
        return getIncident().getNumber() + " [" + getIncident().getPriority() + "] assigned to "
                + getIncident().getAssignee().getName() + ": " + getIncident().getTitle();
    }
}
