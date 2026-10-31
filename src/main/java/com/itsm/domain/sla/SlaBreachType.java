package com.itsm.domain.sla;

/** The two SLA targets that can be violated. */
public enum SlaBreachType {
    RESPONSE("Response"),
    RESOLUTION("Resolution");

    private final String label;

    SlaBreachType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
