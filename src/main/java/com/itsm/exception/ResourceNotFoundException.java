package com.itsm.exception;

/** Thrown when an incident, user, service or other record does not exist. */
public class ResourceNotFoundException extends ItsmException {

    public ResourceNotFoundException(String resource, Object id) {
        super(resource + " not found: " + id);
    }

    @Override
    public int httpStatus() {
        return 404;
    }
}
