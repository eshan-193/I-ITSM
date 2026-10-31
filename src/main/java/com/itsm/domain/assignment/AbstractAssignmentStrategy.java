package com.itsm.domain.assignment;

import com.itsm.domain.incident.Incident;

import java.util.List;

/**
 * Template Method pattern: every strategy first removes technicians who are unavailable
 * or at their workload limit, then orders the rest in its own way.
 * Subclasses only implement {@link #order}; the filtering rule is written once.
 */
public abstract class AbstractAssignmentStrategy implements AssignmentStrategy {

    @Override
    public final List<RankedTechnician> rank(Incident incident, List<TechnicianCandidate> candidates) {
        List<TechnicianCandidate> eligible = candidates.stream()
                .filter(TechnicianCandidate::canTakeMore)
                .toList();
        if (eligible.isEmpty()) {
            return List.of();
        }
        return order(incident, eligible);
    }

    /** Orders eligible candidates, best first. May drop candidates that do not fit at all. */
    protected abstract List<RankedTechnician> order(Incident incident, List<TechnicianCandidate> eligible);
}
