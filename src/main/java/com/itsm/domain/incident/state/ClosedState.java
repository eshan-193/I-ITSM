package com.itsm.domain.incident.state;

import com.itsm.domain.incident.IncidentAction;
import com.itsm.domain.incident.IncidentStatus;

import java.util.EnumSet;
import java.util.Set;



/** Terminal state: no further actions are possible. */
public final class ClosedState implements IncidentState {

    public static final ClosedState INSTANCE = new ClosedState();

    private static final Set<IncidentAction> ALLOWED = EnumSet.noneOf(IncidentAction.class);

    private ClosedState() {
    }

    @Override
    public IncidentStatus status() {
        return IncidentStatus.CLOSED;
    }

    @Override
    public Set<IncidentAction> allowedActions() {
        return ALLOWED;
    }
}
