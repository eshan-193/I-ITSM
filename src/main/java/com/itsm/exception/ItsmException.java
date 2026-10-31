package com.itsm.exception;

/**
 * Root of the application's exception hierarchy.
 * <p>
 * Every business-rule violation extends this class so that one global handler can
 * turn any of them into a friendly message, while callers can still catch a
 * specific subtype when they need special handling. The exceptions are unchecked
 * because the caller usually cannot recover locally (see architecture.md §10).
 */
public abstract class ItsmException extends RuntimeException {

    protected ItsmException(String message) {
        super(message);
    }

    protected ItsmException(String message, Throwable cause) {
        super(message, cause);
    }

    /** HTTP status the REST layer should use for this kind of failure. */
    public abstract int httpStatus();
}
