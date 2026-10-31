package com.itsm.exception;

/** Thrown when the current user is not allowed to perform an operation. */
public class UnauthorizedOperationException extends ItsmException {

    public UnauthorizedOperationException(String message) {
        super(message);
    }

    @Override
    public int httpStatus() {
        return 403;
    }
}
