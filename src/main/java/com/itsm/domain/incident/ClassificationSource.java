package com.itsm.domain.incident;

/** Where an incident's category came from, shown in the UI for transparency. */
public enum ClassificationSource {
    MANUAL("Chosen by user"),
    KEYWORD("Keyword/TF-IDF classifier"),
    AI("AI (Gemini)"),
    SERVICE_DEFAULT("Service default");

    private final String label;

    ClassificationSource(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
