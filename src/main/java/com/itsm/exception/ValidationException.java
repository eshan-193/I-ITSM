package com.itsm.exception;

/** Thrown for invalid user input that is not caught by form-level bean validation. */
public class ValidationException extends ItsmException {

    public ValidationException(String message) {
        super(message);
    }

    @Override
    public int httpStatus() {
        return 400;
    }
}
