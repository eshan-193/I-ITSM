package com.itsm.domain.incident.state;

import com.itsm.domain.incident.IncidentAction;
import com.itsm.domain.incident.IncidentStatus;

import java.util.EnumSet;
import java.util.Set;

import static com.itsm.domain.incident.IncidentAction.*;

/** Just reported: waiting to be classified (or closed as a duplicate). */
public final class NewState implements IncidentState {

    public static final NewState INSTANCE = new NewState();

    private static final Set<IncidentAction> ALLOWED = EnumSet.of(CLASSIFY, CLOSE_AS_DUPLICATE);

    private NewState() {
    }

    @Override
    public IncidentStatus status() {
        return IncidentStatus.NEW;
    }

    @Override
    public Set<IncidentAction> allowedActions() {
        return ALLOWED;
    }

    @Override
    public IncidentState classify() {
        return ClassifiedState.INSTANCE;
    }

    @Override
    public IncidentState closeAsDuplicate() {
        return ClosedState.INSTANCE;
    }
}
