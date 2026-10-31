package com.itsm.domain.notification;

/** How important an event is; drives the channels used and the colour in the UI. */
public enum Severity {
    INFO("primary"),
    SUCCESS("success"),
    WARNING("warning"),
    CRITICAL("danger");

    private final String color;

    Severity(String color) {
        this.color = color;
    }

    public String getColor() {
        return color;
    }
}
