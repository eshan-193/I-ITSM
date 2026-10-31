package com.itsm.domain.incident;

/** How widely the problem affects the organisation. */
public enum Impact {
    LOW(1, "Single user"),
    MEDIUM(2, "A team or department"),
    HIGH(3, "Organisation-wide or customers");

    private final int weight;
    private final String description;

    Impact(int weight, String description) {
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
