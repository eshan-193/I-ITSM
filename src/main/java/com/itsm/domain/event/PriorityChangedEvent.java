package com.itsm.domain.event;

import com.itsm.domain.incident.Incident;
import com.itsm.domain.notification.Severity;
import com.itsm.domain.priority.Priority;
import com.itsm.domain.user.User;

import java.time.LocalDateTime;
import java.util.Set;

public class PriorityChangedEvent extends IncidentEvent {

    private final Priority previous;

    public PriorityChangedEvent(Incident incident, User actor, LocalDateTime at, Priority previous) {
        super(incident, actor, at, incident.getStatus(), incident.getStatus());
        this.previous = previous;
    }

    @Override
    public String auditAction() {
        return previous == null ? "PRIORITY_SET" : "PRIORITY_CHANGED";
    }

    @Override
    public String describe() {
        Incident i = getIncident();
        String change = previous == null ? "Priority calculated as " + i.getPriority()
                : "Priority changed from " + previous + " to " + i.getPriority();
        return change + " [" + i.getPriorityRationale() + "]";
    }

    /** Only an actual change is worth telling the technician about. */
    @Override
    public Set<User> recipients() {
        return previous == null ? Set.of() : assigneeOnly();
    }

    @Override
    public boolean notifyAdmins() {
        return previous != null && getIncident().getPriority() == Priority.P1;
    }

    @Override
    public Severity severity() {
        return getIncident().getPriority() == Priority.P1 ? Severity.WARNING : Severity.INFO;
    }

    public Priority getPrevious() {
        return previous;
    }
}
