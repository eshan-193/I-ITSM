package com.itsm.domain.catalog;

/** How important an IT service is to the business; set by the administrator. */
public enum BusinessCriticality {
    LOW(1, "Supporting service"),
    MEDIUM(2, "Important service"),
    HIGH(3, "Business-critical service");

    private final int weight;
    private final String description;

    BusinessCriticality(int weight, String description) {
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
