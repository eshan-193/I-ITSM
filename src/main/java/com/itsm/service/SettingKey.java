package com.itsm.service;

/** Runtime settings an administrator can change, each with a safe default. */
public enum SettingKey {
    ASSIGNMENT_STRATEGY("SMART", "Active technician assignment strategy"),
    PRIORITY_CALCULATOR("WEIGHTED", "Active priority algorithm"),
    AUTO_ASSIGN("true", "Assign a technician automatically when an incident is reported"),
    SLA_WARNING_PERCENT("75", "Warn when this % of the resolution time has been used"),
    ESCALATION_ENABLED("true", "Escalate unacknowledged P1/P2 incidents automatically"),
    DUPLICATE_THRESHOLD("0.35", "Similarity (0–1) above which incidents are flagged as possible duplicates"),
    CLUSTER_SIZE("3", "Open incidents on one service within the window that suggest a major outage"),
    CLUSTER_WINDOW_MINUTES("60", "Time window for outage cluster detection"),
    AI_ENABLED("true", "Use the Gemini AI provider when an API key is configured");

    private final String defaultValue;
    private final String description;

    SettingKey(String defaultValue, String description) {
        this.defaultValue = defaultValue;
        this.description = description;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public String getDescription() {
        return description;
    }
}
