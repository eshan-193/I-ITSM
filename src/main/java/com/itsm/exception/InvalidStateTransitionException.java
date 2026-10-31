package com.itsm.exception;

import com.itsm.domain.incident.IncidentStatus;

/** Thrown when an action is not allowed from the incident's current lifecycle state. */
public class InvalidStateTransitionException extends ItsmException {

    private final IncidentStatus currentStatus;
    private final String action;

    public InvalidStateTransitionException(IncidentStatus currentStatus, String action) {
        super("Cannot " + action + " an incident that is " + currentStatus.getLabel());
        this.currentStatus = currentStatus;
        this.action = action;
    }

    public IncidentStatus getCurrentStatus() {
        return currentStatus;
    }

    public String getAction() {
        return action;
    }

    @Override
    public int httpStatus() {
        return 409;
    }
}
