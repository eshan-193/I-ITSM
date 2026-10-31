package com.itsm.domain.incident;

import com.itsm.domain.incident.state.*;

import java.util.EnumSet;
import java.util.Set;

/**
 * The lifecycle status as stored in the database.
 * <p>
 * The enum is the <em>data</em> (easy to save and search). The behaviour lives in the
 * matching {@link IncidentState} object returned by {@link #state()}.
 */
public enum IncidentStatus {
    NEW("New", "secondary"),
    CLASSIFIED("Classified", "secondary"),
    ASSIGNED("Assigned", "primary"),
    ACKNOWLEDGED("Acknowledged", "primary"),
    IN_PROGRESS("In progress", "info"),
    AWAITING_INFO("Awaiting info", "warning"),
    RESOLVED("Resolved", "success"),
    VERIFIED("Verified", "success"),
    CLOSED("Closed", "dark");

    /** Statuses in which a technician is actively responsible: these count towards workload. */
    public static final Set<IncidentStatus> ACTIVE_WORK = EnumSet.of(ASSIGNED, ACKNOWLEDGED, IN_PROGRESS, AWAITING_INFO);

    /** Statuses in which the problem is not yet fixed. */
    public static final Set<IncidentStatus> UNRESOLVED = EnumSet.of(NEW, CLASSIFIED, ASSIGNED, ACKNOWLEDGED, IN_PROGRESS, AWAITING_INFO);

    private final String label;
    private final String badgeColor;

    IncidentStatus(String label, String badgeColor) {
        this.label = label;
        this.badgeColor = badgeColor;
    }

    public String getLabel() {
        return label;
    }

    public String getBadgeColor() {
        return badgeColor;
    }

    public boolean isUnresolved() {
        return UNRESOLVED.contains(this);
    }

    public boolean isActiveWork() {
        return ACTIVE_WORK.contains(this);
    }

    /** The State-pattern object holding the rules for this status. */
    public IncidentState state() {
        return switch (this) {
            case NEW -> NewState.INSTANCE;
            case CLASSIFIED -> ClassifiedState.INSTANCE;
            case ASSIGNED -> AssignedState.INSTANCE;
            case ACKNOWLEDGED -> AcknowledgedState.INSTANCE;
            case IN_PROGRESS -> InProgressState.INSTANCE;
            case AWAITING_INFO -> AwaitingInfoState.INSTANCE;
            case RESOLVED -> ResolvedState.INSTANCE;
            case VERIFIED -> VerifiedState.INSTANCE;
            case CLOSED -> ClosedState.INSTANCE;
        };
    }
}
