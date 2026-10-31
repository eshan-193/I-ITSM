package com.itsm.exception;

/** Thrown when no technician (or the chosen technician) can take an incident. */
public class TechnicianUnavailableException extends ItsmException {

    public TechnicianUnavailableException(String message) {
        super(message);
    }

    @Override
    public int httpStatus() {
        return 409;
    }
}
