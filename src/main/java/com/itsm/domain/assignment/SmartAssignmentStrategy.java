package com.itsm.domain.assignment;

import com.itsm.domain.incident.Incident;
import com.itsm.domain.priority.Priority;

import java.util.Comparator;
import java.util.List;

/**
 * Intelligent recommendation: a weighted score over several signals.
 * <pre>
 *   skill match            40
 *   spare capacity         25 × (1 − load ratio)
 *   service history        15 × min(resolved on this service, 5) / 5
 *   category history       10 × min(resolved in this category, 10) / 10
 *   seniority              10 × min(years, 10) / 10   (doubled weight for P1/P2)
 * </pre>
 * The reason string lists the strongest signals so the recommendation is explainable.
 */
public class SmartAssignmentStrategy extends AbstractAssignmentStrategy {

    @Override
    public String key() {
        return "SMART";
    }

    @Override
    public String displayName() {
        return "Smart recommendation (weighted)";
    }

    @Override
    public String description() {
        return "Scores every technician on skill, spare capacity, past work on the same service and category, and "
                + "seniority (weighted higher for P1/P2), then picks the best.";
    }

    @Override
    protected List<RankedTechnician> order(Incident incident, List<TechnicianCandidate> eligible) {
        return eligible.stream()
                .map(c -> score(incident, c))
                .sorted(Comparator.comparingDouble(RankedTechnician::score).reversed())
                .toList();
    }

    RankedTechnician score(Incident incident, TechnicianCandidate c) {
        boolean skilled = c.technician().hasSkill(incident.getCategory());
        boolean urgent = incident.getPriority() == Priority.P1 || incident.getPriority() == Priority.P2;

        double skill = skilled ? 40 : 0;
        double capacity = 25 * (1 - c.loadRatio());
        double service = 15 * Math.min(c.resolvedForService(), 5) / 5.0;
        double category = 10 * Math.min(c.resolvedInCategory(), 10) / 10.0;
        double seniority = (urgent ? 20 : 10) * Math.min(c.technician().getExperienceYears(), 10) / 10.0;
        double total = skill + capacity + service + category + seniority;

        StringBuilder reason = new StringBuilder();
        reason.append(skilled ? "skill match" : "no skill match");
        reason.append(", ").append(c.activeIncidents()).append(" active");
        if (c.resolvedForService() > 0) {
            reason.append(", fixed ").append(c.resolvedForService()).append(" on this service");
        }
        if (c.resolvedInCategory() > 0) {
            reason.append(", ").append(c.resolvedInCategory()).append(" in category");
        }
        reason.append(", ").append(c.technician().getExperienceYears()).append(" yrs");
        return new RankedTechnician(c, Math.round(total * 10) / 10.0, reason.toString());
    }
}
