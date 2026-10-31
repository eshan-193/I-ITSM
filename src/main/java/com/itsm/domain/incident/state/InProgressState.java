package com.itsm.domain.incident.state;

import com.itsm.domain.incident.IncidentAction;
import com.itsm.domain.incident.IncidentStatus;

import java.util.EnumSet;
import java.util.Set;

import static com.itsm.domain.incident.IncidentAction.*;

/** Under investigation. */
public final class InProgressState implements IncidentState {

    public static final InProgressState INSTANCE = new InProgressState();

    private static final Set<IncidentAction> ALLOWED = EnumSet.of(ASSIGN, REQUEST_INFO, RESOLVE);

    private InProgressState() {
    }

    @Override
    public IncidentStatus status() {
        return IncidentStatus.IN_PROGRESS;
    }

    @Override
    public Set<IncidentAction> allowedActions() {
        return ALLOWED;
    }

    @Override
    public IncidentState assign() {
        return AssignedState.INSTANCE;
    }

    @Override
    public IncidentState requestInfo() {
        return AwaitingInfoState.INSTANCE;
    }

    @Override
    public IncidentState resolve() {
        return ResolvedState.INSTANCE;
    }
}
