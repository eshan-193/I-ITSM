package com.itsm.exception;

import com.itsm.domain.priority.Priority;

/** Thrown when no SLA policy exists for a priority (neither service-specific nor default). */
public class SlaConfigurationMissingException extends ItsmException {

    public SlaConfigurationMissingException(Priority priority) {
        super("No SLA policy is configured for priority " + priority);
    }

    @Override
    public int httpStatus() {
        return 500;
    }
}
