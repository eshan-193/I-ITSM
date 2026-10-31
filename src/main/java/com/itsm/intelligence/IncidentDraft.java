package com.itsm.intelligence;

import com.itsm.domain.catalog.Category;
import com.itsm.domain.catalog.ItService;
import com.itsm.domain.incident.Impact;
import com.itsm.domain.incident.Urgency;

/** A pre-filled incident form produced from a free-text description ("AI Quick Report"). */
public record IncidentDraft(String title, String description, ItService service, Category category,
                            Impact impact, Urgency urgency, int affectedUsers, String source) {
}
