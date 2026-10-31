package com.itsm.exception;

/** Thrown when a reporter submits an incident identical to one they already have open. */
public class DuplicateIncidentException extends ItsmException {

    private final String existingIncidentNumber;

    public DuplicateIncidentException(String existingIncidentNumber) {
        super("You already reported this problem as " + existingIncidentNumber);
        this.existingIncidentNumber = existingIncidentNumber;
    }

    public String getExistingIncidentNumber() {
        return existingIncidentNumber;
    }

    @Override
    public int httpStatus() {
        return 409;
    }
}
