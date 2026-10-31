package com.itsm.domain.incident.state;

import com.itsm.domain.incident.IncidentAction;
import com.itsm.domain.incident.IncidentStatus;

import java.util.EnumSet;
import java.util.Set;

import static com.itsm.domain.incident.IncidentAction.*;

/** The reporter confirmed the fix; ready to be closed. */
public final class VerifiedState implements IncidentState {

    public static final VerifiedState INSTANCE = new VerifiedState();

    private static final Set<IncidentAction> ALLOWED = EnumSet.of(CLOSE);

    private VerifiedState() {
    }

    @Override
    public IncidentStatus status() {
        return IncidentStatus.VERIFIED;
    }

    @Override
    public Set<IncidentAction> allowedActions() {
        return ALLOWED;
    }

    @Override
    public IncidentState close() {
        return ClosedState.INSTANCE;
    }
}
