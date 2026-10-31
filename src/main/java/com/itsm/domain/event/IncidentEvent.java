package com.itsm.domain.event;

import com.itsm.domain.incident.Incident;
import com.itsm.domain.incident.IncidentStatus;
import com.itsm.domain.notification.ChannelType;
import com.itsm.domain.notification.Severity;
import com.itsm.domain.priority.Priority;
import com.itsm.domain.user.User;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Observer pattern: something noteworthy happened to an incident.
 * <p>
 * Services only <em>publish</em> events. Independent listeners react: one writes the
 * audit trail, one sends notifications, one pushes live dashboard updates. Adding a new
 * reaction (say, Slack) means adding a listener; the services do not change.
 * <p>
 * Each subclass decides polymorphically who should hear about it, how urgently, and
 * through which channels.
 */
public abstract class IncidentEvent {

    private final Incident incident;
    private final User actor;
    private final LocalDateTime occurredAt;
    private final IncidentStatus fromStatus;
    private final IncidentStatus toStatus;

    protected IncidentEvent(Incident incident, User actor, LocalDateTime occurredAt,
                            IncidentStatus fromStatus, IncidentStatus toStatus) {
        this.incident = Objects.requireNonNull(incident);
        this.actor = actor;
        this.occurredAt = occurredAt;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
    }

    /** Short machine-friendly action name for the audit log, e.g. "ASSIGNED". */
    public abstract String auditAction();

    /** One-line human description, used in the audit log and as the notification text. */
    public abstract String describe();

    /** People to notify directly. The actor is never notified about their own action. */
    public Set<User> recipients() {
        return Set.of();
    }

    /** Should every administrator also be notified? */
    public boolean notifyAdmins() {
        return false;
    }

    public Severity severity() {
        return Severity.INFO;
    }

    /** In-app and live by default; important events add email, critical ones SMS. */
    public Set<ChannelType> channels() {
        EnumSet<ChannelType> channels = EnumSet.of(ChannelType.IN_APP, ChannelType.LIVE);
        if (severity() == Severity.CRITICAL || severity() == Severity.WARNING) {
            channels.add(ChannelType.EMAIL);
        }
        if (severity() == Severity.CRITICAL && incident.getPriority() == Priority.P1) {
            channels.add(ChannelType.SMS);
        }
        return channels;
    }

    public String notificationMessage() {
        return incident.getNumber() + ": " + describe();
    }

    /** Helper for subclasses: reporter plus everyone who joined via "me too". */
    protected Set<User> reporterAndWatchers() {
        Set<User> users = new LinkedHashSet<>();
        users.add(incident.getReporter());
        users.addAll(incident.getWatchers());
        return users;
    }

    protected Set<User> assigneeOnly() {
        return incident.getAssignee() == null ? Set.of() : Set.of(incident.getAssignee());
    }

    public Incident getIncident() { return incident; }
    public User getActor() { return actor; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
    public IncidentStatus getFromStatus() { return fromStatus; }
    public IncidentStatus getToStatus() { return toStatus; }
}
