package com.itsm.domain.priority;

/**
 * Outcome of a priority calculation, with a human-readable explanation so the
 * decision is transparent to technicians and reporters.
 */
public record PriorityResult(Priority priority, int score, String rationale) {
}
