package com.itsm.domain.incident.state;

import com.itsm.domain.incident.IncidentAction;
import com.itsm.domain.incident.IncidentStatus;

import java.util.EnumSet;
import java.util.Set;

import static com.itsm.domain.incident.IncidentAction.*;

/** Category and priority known; waiting for a technician. */
public final class ClassifiedState implements IncidentState {

    public static final ClassifiedState INSTANCE = new ClassifiedState();

    private static final Set<IncidentAction> ALLOWED = EnumSet.of(CLASSIFY, ASSIGN, CLOSE_AS_DUPLICATE);

    private ClassifiedState() {
    }

    @Override
    public IncidentStatus status() {
        return IncidentStatus.CLASSIFIED;
    }

    @Override
    public Set<IncidentAction> allowedActions() {
        return ALLOWED;
    }

    /** Re-classification keeps the incident in this state. */
    @Override
    public IncidentState classify() {
        return this;
    }

    @Override
    public IncidentState assign() {
        return AssignedState.INSTANCE;
    }

    @Override
    public IncidentState closeAsDuplicate() {
        return ClosedState.INSTANCE;
    }
}
