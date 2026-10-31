package com.itsm.intelligence;

import com.itsm.domain.incident.Incident;

/** An incident and how similar (0–1) it is to some text. */
public record SimilarIncident(Incident incident, double similarity) {

    public int similarityPercent() {
        return (int) Math.round(similarity * 100);
    }
}
