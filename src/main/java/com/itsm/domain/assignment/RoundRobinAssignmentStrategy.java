package com.itsm.domain.assignment;

import com.itsm.domain.incident.Incident;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/** Technicians take turns in a fixed order, skipping anyone unavailable or at their limit. */
public class RoundRobinAssignmentStrategy extends AbstractAssignmentStrategy {

    /** Thread-safe turn counter shared by concurrent requests. */
    private final AtomicInteger turn = new AtomicInteger();

    @Override
    public String key() {
        return "ROUND_ROBIN";
    }

    @Override
    public String displayName() {
        return "Round-robin";
    }

    @Override
    public String description() {
        return "Technicians take turns in a fixed order; simple and fair when everyone has similar skills.";
    }

    @Override
    protected List<RankedTechnician> order(Incident incident, List<TechnicianCandidate> eligible) {
        List<TechnicianCandidate> sorted = new ArrayList<>(eligible);
        sorted.sort(Comparator.comparing(c -> c.technician().getId(), Comparator.nullsLast(Comparator.naturalOrder())));
        int start = Math.floorMod(turn.getAndIncrement(), sorted.size());
        List<RankedTechnician> ranked = new ArrayList<>();
        for (int i = 0; i < sorted.size(); i++) {
            TechnicianCandidate c = sorted.get((start + i) % sorted.size());
            ranked.add(new RankedTechnician(c, 1.0 - (double) i / sorted.size(), i == 0 ? "Next in rotation" : "Later in rotation"));
        }
        return ranked;
    }
}
