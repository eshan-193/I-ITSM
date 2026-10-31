package com.itsm.domain.incident;

/** How quickly the problem needs to be fixed. */
public enum Urgency {
    LOW(1, "Can wait, a workaround exists"),
    MEDIUM(2, "Needs attention soon"),
    HIGH(3, "Work is blocked right now");

    private final int weight;
    private final String description;

    Urgency(int weight, String description) {
        this.weight = weight;
        this.description = description;
    }

    public int getWeight() {
        return weight;
    }

    public String getDescription() {
        return description;
    }
}
