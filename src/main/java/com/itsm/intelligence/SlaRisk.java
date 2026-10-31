package com.itsm.intelligence;

import java.util.List;

/** Predicted likelihood (0–100) that an open incident will miss its SLA, with reasons. */
public record SlaRisk(int score, Level level, List<String> reasons) {

    public enum Level {
        LOW("Low", "success"),
        MEDIUM("Medium", "warning"),
        HIGH("High", "danger");

        private final String label;
        private final String color;

        Level(String label, String color) {
            this.label = label;
            this.color = color;
        }

        public String getLabel() { return label; }
        public String getColor() { return color; }
    }

    public static SlaRisk of(int score, List<String> reasons) {
        int s = Math.max(0, Math.min(100, score));
        Level level = s >= 70 ? Level.HIGH : s >= 40 ? Level.MEDIUM : Level.LOW;
        return new SlaRisk(s, level, List.copyOf(reasons));
    }
}
