package com.itsm.domain.incident.state;

import com.itsm.domain.incident.IncidentAction;
import com.itsm.domain.incident.IncidentStatus;

import java.util.EnumSet;
import java.util.Set;

import static com.itsm.domain.incident.IncidentAction.*;

/** A technician is assigned but has not yet acknowledged; the response SLA clock is running. */
public final class AssignedState implements IncidentState {

    public static final AssignedState INSTANCE = new AssignedState();

    private static final Set<IncidentAction> ALLOWED = EnumSet.of(ASSIGN, ACKNOWLEDGE, CLOSE_AS_DUPLICATE);

    private AssignedState() {
    }

    @Override
    public IncidentStatus status() {
        return IncidentStatus.ASSIGNED;
    }

    @Override
    public Set<IncidentAction> allowedActions() {
        return ALLOWED;
    }

    /** Re-assignment to a different technician is allowed. */
    @Override
    public IncidentState assign() {
        return this;
    }

    @Override
    public IncidentState acknowledge() {
        return AcknowledgedState.INSTANCE;
    }

    @Override
    public IncidentState closeAsDuplicate() {
        return ClosedState.INSTANCE;
    }
}
