package com.itsm.domain.sla;

/** SLA health of a single incident at a point in time. */
public enum SlaStatus {
    ON_TRACK("On track", "success"),
    AT_RISK("At risk", "warning"),
    BREACHED("Breached", "danger"),
    PAUSED("Paused", "secondary"),
    MET("Met", "success"),
    NOT_STARTED("No SLA", "light");

    private final String label;
    private final String badgeColor;

    SlaStatus(String label, String badgeColor) {
        this.label = label;
        this.badgeColor = badgeColor;
    }

    public String getLabel() {
        return label;
    }

    public String getBadgeColor() {
        return badgeColor;
    }
}
