package com.itsm.domain.notification;

/** The ways a notification can reach a person. */
public enum ChannelType {
    IN_APP("In-app", "bell"),
    LIVE("Live push", "broadcast"),
    EMAIL("Email", "envelope"),
    SMS("SMS", "phone");

    private final String label;
    private final String icon;

    ChannelType(String label, String icon) {
        this.label = label;
        this.icon = icon;
    }

    public String getLabel() {
        return label;
    }

    /** Bootstrap Icons name. */
    public String getIcon() {
        return icon;
    }
}
