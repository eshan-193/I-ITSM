package com.itsm.domain.assignment;

import com.itsm.domain.incident.Incident;

import java.util.Comparator;
import java.util.List;

/** Only technicians skilled in the incident's category; among them, the least loaded first. */
public class SkillBasedAssignmentStrategy extends AbstractAssignmentStrategy {

    @Override
    public String key() {
        return "SKILL";
    }

    @Override
    public String displayName() {
        return "Skill-based";
    }

    @Override
    public String description() {
        return "Chooses a technician skilled in the incident's category; ties go to the least loaded, then the most experienced.";
    }

    @Override
    protected List<RankedTechnician> order(Incident incident, List<TechnicianCandidate> eligible) {
        return eligible.stream()
                .filter(c -> c.technician().hasSkill(incident.getCategory()))
                .sorted(Comparator.comparingInt(TechnicianCandidate::activeIncidents)
                        .thenComparing(c -> -c.technician().getExperienceYears()))
                .map(c -> new RankedTechnician(c, 1.0 - c.loadRatio(),
                        "Skilled in " + incident.getCategory().getName() + ", " + c.activeIncidents() + " active incidents"))
                .toList();
    }
}
