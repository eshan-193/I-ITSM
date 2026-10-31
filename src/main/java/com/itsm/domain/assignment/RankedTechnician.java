package com.itsm.domain.assignment;

/** A candidate with the score and reason a strategy gave it; shown to admins as a recommendation. */
public record RankedTechnician(TechnicianCandidate candidate, double score, String reason) {
}
