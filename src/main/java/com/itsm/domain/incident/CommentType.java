package com.itsm.domain.incident;

/** Kinds of messages on an incident's conversation thread. */
public enum CommentType {
    UPDATE("Update", true),
    INVESTIGATION_NOTE("Investigation note", false),
    INFO_REQUEST("Information requested", true),
    INFO_RESPONSE("Additional information", true);

    private final String label;
    private final boolean visibleToReporter;

    CommentType(String label, boolean visibleToReporter) {
        this.label = label;
        this.visibleToReporter = visibleToReporter;
    }

    public String getLabel() {
        return label;
    }

    /** Investigation notes are internal to support staff. */
    public boolean isVisibleToReporter() {
        return visibleToReporter;
    }
}
