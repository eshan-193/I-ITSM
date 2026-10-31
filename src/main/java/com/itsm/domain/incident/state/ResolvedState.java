package com.itsm.domain.incident.state;

import com.itsm.domain.incident.IncidentAction;
import com.itsm.domain.incident.IncidentStatus;

import java.util.EnumSet;
import java.util.Set;

import static com.itsm.domain.incident.IncidentAction.*;

/** A fix has been provided; the reporter confirms it or reopens the incident. */
public final class ResolvedState implements IncidentState {

    public static final ResolvedState INSTANCE = new ResolvedState();

    private static final Set<IncidentAction> ALLOWED = EnumSet.of(VERIFY, REOPEN);

    private ResolvedState() {
    }

    @Override
    public IncidentStatus status() {
        return IncidentStatus.RESOLVED;
    }

    @Override
    public Set<IncidentAction> allowedActions() {
        return ALLOWED;
    }

    @Override
    public IncidentState verify() {
        return VerifiedState.INSTANCE;
    }

    /** The reporter rejected the fix, so work resumes. */
    @Override
    public IncidentState reopen() {
        return InProgressState.INSTANCE;
    }
}
