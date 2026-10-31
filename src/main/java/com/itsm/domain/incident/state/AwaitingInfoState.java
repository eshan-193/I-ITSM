package com.itsm.domain.incident.state;

import com.itsm.domain.incident.IncidentAction;
import com.itsm.domain.incident.IncidentStatus;

import java.util.EnumSet;
import java.util.Set;

import static com.itsm.domain.incident.IncidentAction.*;

/** Waiting for the reporter to answer a question; the resolution SLA clock is paused. */
public final class AwaitingInfoState implements IncidentState {

    public static final AwaitingInfoState INSTANCE = new AwaitingInfoState();

    private static final Set<IncidentAction> ALLOWED = EnumSet.of(PROVIDE_INFO);

    private AwaitingInfoState() {
    }

    @Override
    public IncidentStatus status() {
        return IncidentStatus.AWAITING_INFO;
    }

    @Override
    public Set<IncidentAction> allowedActions() {
        return ALLOWED;
    }

    @Override
    public IncidentState provideInfo() {
        return InProgressState.INSTANCE;
    }
}
