package com.itsm.intelligence;

import com.itsm.domain.catalog.ItService;
import com.itsm.domain.priority.Priority;

/** Live health of one IT service, derived from its open incidents. */
public record ServiceHealth(ItService service, int openIncidents, Priority worstPriority, Status status) {

    public enum Status {
        OPERATIONAL("Operational", "success"),
        DEGRADED("Degraded", "warning"),
        OUTAGE("Outage", "danger");

        private final String label;
        private final String color;

        Status(String label, String color) {
            this.label = label;
            this.color = color;
        }

        public String getLabel() { return label; }
        public String getColor() { return color; }
    }
}
