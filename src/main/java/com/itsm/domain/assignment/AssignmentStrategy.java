package com.itsm.domain.assignment;

import com.itsm.domain.incident.Incident;

import java.util.List;
import java.util.Optional;

/**
 * Strategy pattern: an interchangeable rule for choosing a technician.
 * <p>
 * The assignment service holds a reference to <em>an</em> AssignmentStrategy and never
 * knows which one. Adding a strategy means adding one class (Open/Closed Principle).
 */
public interface AssignmentStrategy {

    /** Stable identifier stored in settings, e.g. "SKILL". */
    String key();

    String displayName();

    String description();

    /** Eligible technicians, best first. Empty if nobody can take the incident. */
    List<RankedTechnician> rank(Incident incident, List<TechnicianCandidate> candidates);

    default Optional<RankedTechnician> select(Incident incident, List<TechnicianCandidate> candidates) {
        return rank(incident, candidates).stream().findFirst();
    }
}
