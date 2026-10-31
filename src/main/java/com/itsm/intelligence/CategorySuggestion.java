package com.itsm.intelligence;

import com.itsm.domain.catalog.Category;
import com.itsm.domain.incident.ClassificationSource;

/** A suggested category with confidence (0–1), where it came from, and why. */
public record CategorySuggestion(Category category, double confidence, ClassificationSource source, String explanation) {

    public int confidencePercent() {
        return (int) Math.round(confidence * 100);
    }
}
