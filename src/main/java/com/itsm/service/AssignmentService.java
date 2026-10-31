package com.itsm.service;

import com.itsm.domain.assignment.AssignmentStrategy;
import com.itsm.domain.assignment.RankedTechnician;
import com.itsm.domain.assignment.TechnicianCandidate;
import com.itsm.domain.incident.Incident;
import com.itsm.domain.incident.IncidentStatus;
import com.itsm.domain.user.Technician;
import com.itsm.repository.IncidentRepository;
import com.itsm.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Chooses technicians. Holds every {@link AssignmentStrategy} bean in a map keyed by
 * its id and delegates to whichever one the administrator selected; it never knows
 * which concrete class it is talking to.
 */
@Service
@Transactional(readOnly = true)
public class AssignmentService {

    private final Map<String, AssignmentStrategy> strategies = new LinkedHashMap<>();
    private final SettingsService settings;
    private final UserRepository users;
    private final IncidentRepository incidents;

    public AssignmentService(List<AssignmentStrategy> strategies, SettingsService settings,
                             UserRepository users, IncidentRepository incidents) {
        strategies.forEach(s -> this.strategies.put(s.key(), s));
        this.settings = settings;
        this.users = users;
        this.incidents = incidents;
    }

    public AssignmentStrategy activeStrategy() {
        AssignmentStrategy strategy = strategies.get(settings.get(SettingKey.ASSIGNMENT_STRATEGY));
        return strategy != null ? strategy : strategies.values().iterator().next();
    }

    public List<AssignmentStrategy> availableStrategies() {
        return List.copyOf(strategies.values());
    }

    public boolean isKnownStrategy(String key) {
        return strategies.containsKey(key);
    }

    /** Best technician according to the active strategy, if anyone can take the incident. */
    public Optional<RankedTechnician> selectFor(Incident incident) {
        return activeStrategy().select(incident, candidatesFor(incident));
    }

    /** Ranked list for the "recommended technicians" panel, always using the smart strategy for its explanations. */
    public List<RankedTechnician> recommendFor(Incident incident) {
        AssignmentStrategy smart = strategies.getOrDefault("SMART", activeStrategy());
        return smart.rank(incident, candidatesFor(incident));
    }

    /** Current active-incident count per technician id. */
    public Map<Long, Integer> workloads() {
        Map<Long, Integer> load = new HashMap<>();
        for (Object[] row : incidents.countByAssigneeGrouped(IncidentStatus.ACTIVE_WORK)) {
            load.put((Long) row[0], ((Number) row[1]).intValue());
        }
        return load;
    }

    public int workloadOf(Technician technician) {
        return (int) incidents.countByAssigneeAndStatusIn(technician, IncidentStatus.ACTIVE_WORK);
    }

    public List<TechnicianCandidate> candidatesFor(Incident incident) {
        Map<Long, Integer> load = workloads();
        Map<Long, Long> byCategory = incident.getCategory() == null ? Map.of()
                : toMap(incidents.countResolvedByAssigneeInCategory(incident.getCategory()));
        Map<Long, Long> byService = toMap(incidents.countResolvedByAssigneeForService(incident.getService()));

        List<TechnicianCandidate> candidates = new ArrayList<>();
        for (Technician t : users.findActiveTechnicians()) {
            if (t.equals(incident.getAssignee())) {
                continue; // re-assignment should pick someone else
            }
            candidates.add(new TechnicianCandidate(t, load.getOrDefault(t.getId(), 0),
                    byCategory.getOrDefault(t.getId(), 0L), byService.getOrDefault(t.getId(), 0L)));
        }
        return candidates;
    }

    private static Map<Long, Long> toMap(List<Object[]> rows) {
        Map<Long, Long> map = new HashMap<>();
        for (Object[] row : rows) {
            map.put((Long) row[0], ((Number) row[1]).longValue());
        }
        return map;
    }
}
