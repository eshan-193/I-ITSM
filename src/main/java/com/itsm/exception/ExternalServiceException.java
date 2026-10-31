package com.itsm.exception;

/** Thrown when an external service (AI provider, mail server) fails or cannot be reached. */
public class ExternalServiceException extends ItsmException {

    public ExternalServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public int httpStatus() {
        return 503;
    }
}
