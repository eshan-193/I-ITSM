package com.itsm.domain.incident.state;

import com.itsm.domain.incident.IncidentAction;
import com.itsm.domain.incident.IncidentStatus;

import java.util.EnumSet;
import java.util.Set;

import static com.itsm.domain.incident.IncidentAction.*;

/** The technician has accepted the incident (response SLA met). */
public final class AcknowledgedState implements IncidentState {

    public static final AcknowledgedState INSTANCE = new AcknowledgedState();

    private static final Set<IncidentAction> ALLOWED = EnumSet.of(ASSIGN, START_WORK, CLOSE_AS_DUPLICATE);

    private AcknowledgedState() {
    }

    @Override
    public IncidentStatus status() {
        return IncidentStatus.ACKNOWLEDGED;
    }

    @Override
    public Set<IncidentAction> allowedActions() {
        return ALLOWED;
    }

    /** Handing over to another technician means they must acknowledge again. */
    @Override
    public IncidentState assign() {
        return AssignedState.INSTANCE;
    }

    @Override
    public IncidentState startWork() {
        return InProgressState.INSTANCE;
    }

    @Override
    public IncidentState closeAsDuplicate() {
        return ClosedState.INSTANCE;
    }
}
