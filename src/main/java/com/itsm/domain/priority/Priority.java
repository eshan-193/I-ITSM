package com.itsm.domain.priority;

/** Incident priority levels, from most to least urgent. */
public enum Priority {
    P1("Critical", "danger"),
    P2("High", "warning"),
    P3("Medium", "info"),
    P4("Low", "secondary");

    private final String label;
    private final String badgeColor;

    Priority(String label, String badgeColor) {
        this.label = label;
        this.badgeColor = badgeColor;
    }

    public String getLabel() {
        return label;
    }

    /** Bootstrap colour name used for badges in the UI. */
    public String getBadgeColor() {
        return badgeColor;
    }

    public String getDisplayName() {
        return name() + " – " + label;
    }

    /** One level more urgent, or the same level if already P1. */
    public Priority raise() {
        return this == P1 ? P1 : values()[ordinal() - 1];
    }

    public boolean isHigherThan(Priority other) {
        return ordinal() < other.ordinal();
    }
}
