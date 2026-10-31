package com.itsm.domain.event;

import com.itsm.domain.incident.Incident;
import com.itsm.domain.incident.IncidentAction;
import com.itsm.domain.incident.IncidentStatus;
import com.itsm.domain.notification.ChannelType;
import com.itsm.domain.notification.Severity;
import com.itsm.domain.user.User;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Set;

/** A lifecycle step such as acknowledge, start work, resolve, verify, reopen or close. */
public class IncidentStatusChangedEvent extends IncidentEvent {

    private final IncidentAction action;
    private final String note;

    public IncidentStatusChangedEvent(Incident incident, User actor, LocalDateTime at,
                                      IncidentStatus from, IncidentAction action, String note) {
        super(incident, actor, at, from, incident.getStatus());
        this.action = action;
        this.note = note;
    }

    @Override
    public String auditAction() {
        return action.name();
    }

    @Override
    public String describe() {
        String base = switch (action) {
            case ACKNOWLEDGE -> "Technician acknowledged the incident";
            case START_WORK -> "Investigation started";
            case REQUEST_INFO -> "Technician requested more information (SLA paused)";
            case PROVIDE_INFO -> "Reporter provided more information (SLA resumed)";
            case RESOLVE -> "Resolution provided";
            case VERIFY -> "Reporter confirmed the resolution (rating " + getIncident().getFeedbackRating() + "/5)";
            case REOPEN -> "Reporter rejected the resolution; incident reopened";
            case CLOSE -> "Incident closed";
            case CLOSE_AS_DUPLICATE -> "Closed as duplicate of " + getIncident().getDuplicateOf().getNumber();
            default -> "Status changed";
        };
        return note == null || note.isBlank() ? base : base + ": " + abbreviate(note);
    }

    @Override
    public Set<User> recipients() {
        return switch (action) {
            case ACKNOWLEDGE, START_WORK, REQUEST_INFO, RESOLVE, CLOSE, CLOSE_AS_DUPLICATE -> reporterAndWatchers();
            case PROVIDE_INFO, VERIFY, REOPEN -> assigneeOnly();
            default -> Set.of();
        };
    }

    @Override
    public Severity severity() {
        return switch (action) {
            case RESOLVE, VERIFY, CLOSE -> Severity.SUCCESS;
            case REOPEN -> Severity.WARNING;
            default -> Severity.INFO;
        };
    }

    /** The reporter gets an email when asked a question or when the problem is fixed. */
    @Override
    public Set<ChannelType> channels() {
        Set<ChannelType> channels = EnumSet.copyOf(super.channels());
        if (action == IncidentAction.REQUEST_INFO || action == IncidentAction.RESOLVE) {
            channels.add(ChannelType.EMAIL);
        }
        return channels;
    }

    public IncidentAction getAction() {
        return action;
    }

    private static String abbreviate(String text) {
        return text.length() > 160 ? text.substring(0, 157) + "..." : text;
    }
}
