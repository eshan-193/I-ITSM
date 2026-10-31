package com.itsm.exception;

/** Thrown when assigning an incident would push a technician past their workload limit. */
public class WorkloadExceededException extends ItsmException {

    public WorkloadExceededException(String technicianName, int activeIncidents, int limit) {
        super(technicianName + " already has " + activeIncidents + " active incidents (limit " + limit + ")");
    }

    @Override
    public int httpStatus() {
        return 409;
    }
}
