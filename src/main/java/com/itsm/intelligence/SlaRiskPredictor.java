package com.itsm.intelligence;

import com.itsm.domain.incident.Incident;
import com.itsm.domain.incident.IncidentStatus;
import com.itsm.domain.priority.Priority;
import com.itsm.domain.sla.SlaTracker;
import com.itsm.repository.IncidentRepository;
import com.itsm.service.AssignmentService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Predicts which open incidents are likely to breach their SLA <em>before</em> they do.
 * <p>
 * An explainable additive model (each factor adds points, capped at 100):
 * <ul>
 *   <li>time already used of the resolution window (up to 55)</li>
 *   <li>still unacknowledged late in the response window (+15)</li>
 *   <li>historical average fix time for this category/priority exceeds the time left (+20)</li>
 *   <li>no technician, or the technician is near their workload limit (+15 / +10)</li>
 *   <li>reopened at least once (+5)</li>
 * </ul>
 */
@Component
public class SlaRiskPredictor {

    private final IncidentRepository incidents;
    private final AssignmentService assignments;
    private final Clock clock;

    public SlaRiskPredictor(IncidentRepository incidents, AssignmentService assignments, Clock clock) {
        this.incidents = incidents;
        this.assignments = assignments;
        this.clock = clock;
    }

    /** Assess many incidents at once, building the historical model only once. */
    @Transactional(readOnly = true)
    public Map<Long, SlaRisk> assess(Collection<Incident> open) {
        HistoryModel history = HistoryModel.from(incidents.findAllResolved());
        Map<Long, Integer> load = assignments.workloads();
        LocalDateTime now = LocalDateTime.now(clock);
        Map<Long, SlaRisk> result = new HashMap<>();
        for (Incident i : open) {
            if (i.getStatus().isUnresolved()) {
                int active = i.getAssignee() == null ? 0 : load.getOrDefault(i.getAssignee().getId(), 0);
                result.put(i.getId(), predict(i, now, history, active));
            }
        }
        return result;
    }

    public SlaRisk assess(Incident incident) {
        return assess(List.of(incident)).getOrDefault(incident.getId(), SlaRisk.of(0, List.of()));
    }

    static SlaRisk predict(Incident incident, LocalDateTime now, HistoryModel history, int assigneeLoad) {
        SlaTracker sla = incident.getSla();
        List<String> reasons = new ArrayList<>();
        if (!sla.isStarted()) {
            return SlaRisk.of(0, List.of("No SLA"));
        }
        if (sla.isBreached()) {
            return SlaRisk.of(100, List.of("SLA already breached"));
        }
        if (sla.isPaused()) {
            return SlaRisk.of(10, List.of("Waiting for reporter (clock paused)"));
        }

        double used = sla.resolutionElapsedFraction(now);
        int score = (int) Math.round(Math.min(used, 1.0) * 55);
        if (used >= 0.5) {
            reasons.add(Math.round(used * 100) + "% of resolution time used");
        }

        if (incident.getAcknowledgedAt() == null) {
            long window = Duration.between(sla.getStartedAt(), sla.getResponseDeadline()).toSeconds();
            long elapsed = Duration.between(sla.getStartedAt(), now).toSeconds();
            if (window > 0 && elapsed >= window * 0.5) {
                score += 15;
                reasons.add("Not yet acknowledged");
            }
        }

        long minutesLeft = sla.minutesToResolutionDeadline(now);
        OptionalDouble typical = history.typicalMinutes(incident);
        if (typical.isPresent() && typical.getAsDouble() > minutesLeft) {
            score += 20;
            reasons.add("Similar incidents usually take ~" + Math.round(typical.getAsDouble()) + " min; " + minutesLeft + " min left");
        }

        if (incident.getAssignee() == null) {
            score += 15;
            reasons.add("No technician assigned");
        } else if ((double) assigneeLoad / incident.getAssignee().getMaxActiveIncidents() >= 0.8) {
            score += 10;
            reasons.add(incident.getAssignee().getName() + " is near their workload limit");
        }

        if (incident.getReopenCount() > 0) {
            score += 5;
            reasons.add("Reopened " + incident.getReopenCount() + "×");
        }
        if (reasons.isEmpty()) {
            reasons.add("On track");
        }
        return SlaRisk.of(score, reasons);
    }

    /** Average historical resolution minutes by (category, priority), falling back to priority only. */
    record HistoryModel(Map<String, Double> byCategoryAndPriority, Map<Priority, Double> byPriority) {

        static HistoryModel from(List<Incident> resolved) {
            Map<String, List<Long>> byKey = new HashMap<>();
            Map<Priority, List<Long>> byPrio = new EnumMap<>(Priority.class);
            for (Incident i : resolved) {
                if (i.getPriority() == null || i.getResolvedAt() == null || i.getStatus() == IncidentStatus.CLOSED && i.getDuplicateOf() != null) {
                    continue;
                }
                long minutes = Duration.between(i.getCreatedAt(), i.getResolvedAt()).toMinutes();
                byPrio.computeIfAbsent(i.getPriority(), k -> new ArrayList<>()).add(minutes);
                if (i.getCategory() != null) {
                    byKey.computeIfAbsent(key(i), k -> new ArrayList<>()).add(minutes);
                }
            }
            Map<String, Double> a = new HashMap<>();
            byKey.forEach((k, v) -> { if (v.size() >= 2) a.put(k, avg(v)); });
            Map<Priority, Double> b = new EnumMap<>(Priority.class);
            byPrio.forEach((k, v) -> b.put(k, avg(v)));
            return new HistoryModel(a, b);
        }

        OptionalDouble typicalMinutes(Incident i) {
            if (i.getPriority() == null) return OptionalDouble.empty();
            Double specific = i.getCategory() == null ? null : byCategoryAndPriority.get(key(i));
            if (specific != null) return OptionalDouble.of(specific);
            Double general = byPriority.get(i.getPriority());
            return general == null ? OptionalDouble.empty() : OptionalDouble.of(general);
        }

        private static String key(Incident i) {
            return i.getCategory().getCode() + "|" + i.getPriority();
        }

        private static double avg(List<Long> values) {
            return values.stream().mapToLong(Long::longValue).average().orElse(0);
        }
    }
}
