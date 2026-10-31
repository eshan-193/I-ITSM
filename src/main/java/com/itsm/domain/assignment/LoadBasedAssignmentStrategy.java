package com.itsm.domain.assignment;

import com.itsm.domain.incident.Incident;

import java.util.Comparator;
import java.util.List;

/** The technician with the most spare capacity (lowest load relative to their limit). */
public class LoadBasedAssignmentStrategy extends AbstractAssignmentStrategy {

    @Override
    public String key() {
        return "LOAD";
    }

    @Override
    public String displayName() {
        return "Load-based";
    }

    @Override
    public String description() {
        return "Balances work: chooses the technician with the lowest workload relative to their limit, ignoring skills.";
    }

    @Override
    protected List<RankedTechnician> order(Incident incident, List<TechnicianCandidate> eligible) {
        return eligible.stream()
                .sorted(Comparator.comparingDouble(TechnicianCandidate::loadRatio)
                        .thenComparingInt(TechnicianCandidate::activeIncidents))
                .map(c -> new RankedTechnician(c, 1.0 - c.loadRatio(),
                        c.activeIncidents() + "/" + c.technician().getMaxActiveIncidents() + " capacity used"))
                .toList();
    }
}
