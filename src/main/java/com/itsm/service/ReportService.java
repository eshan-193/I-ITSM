package com.itsm.service;

import com.itsm.domain.incident.Incident;
import com.itsm.domain.incident.IncidentStatus;
import com.itsm.domain.priority.Priority;
import com.itsm.domain.user.Technician;
import com.itsm.repository.IncidentRepository;
import com.itsm.repository.SlaViolationRepository;
import com.itsm.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/** Builds the dashboards and management reports. */
@Service
@Transactional(readOnly = true)
public class ReportService {

    private static final int TREND_DAYS = 14;

    private final IncidentRepository incidents;
    private final UserRepository users;
    private final SlaViolationRepository violations;
    private final AssignmentService assignments;
    private final Clock clock;

    public ReportService(IncidentRepository incidents, UserRepository users, SlaViolationRepository violations,
                         AssignmentService assignments, Clock clock) {
        this.incidents = incidents;
        this.users = users;
        this.violations = violations;
        this.assignments = assignments;
        this.clock = clock;
    }

    public record Overview(long total, long open, long criticalOpen, long resolved, long slaBreached,
                           long unassigned, long awaitingInfo, double slaCompliancePercent,
                           double mttaMinutes, double mttrMinutes, double averageRating) {
    }

    public record TechnicianRow(Technician technician, int active, long resolved, double avgResolutionMinutes,
                                long breaches, double avgRating) {
        public int loadPercent() {
            return (int) Math.round(100.0 * active / technician.getMaxActiveIncidents());
        }
    }

    public record ServiceRow(String service, long total, long open) {
    }

    public record PriorityRow(Priority priority, long count, double avgResolutionMinutes, double compliancePercent) {
    }

    public record Trend(List<String> labels, List<Long> created, List<Long> resolved) {
    }

    public Overview overview() {
        List<Incident> all = incidents.findAll();
        long total = all.size();
        long open = all.stream().filter(Incident::isUnresolved).count();
        long critical = all.stream().filter(i -> i.isUnresolved() && i.getPriority() == Priority.P1).count();
        List<Incident> resolved = all.stream().filter(i -> i.getResolvedAt() != null).toList();
        long breached = all.stream().filter(i -> i.getSla().isBreached()).count();
        long unassigned = all.stream().filter(i -> i.isUnresolved() && i.getAssignee() == null).count();
        long awaiting = all.stream().filter(i -> i.getStatus() == IncidentStatus.AWAITING_INFO).count();
        double compliance = resolved.isEmpty() ? 100
                : 100.0 * resolved.stream().filter(i -> !i.getSla().isBreached()).count() / resolved.size();
        double mtta = average(all.stream().filter(i -> i.getAcknowledgedAt() != null)
                .map(i -> Duration.between(i.getCreatedAt(), i.getAcknowledgedAt())));
        double mttr = average(resolved.stream().map(i -> Duration.between(i.getCreatedAt(), i.getResolvedAt())));
        double rating = all.stream().map(Incident::getFeedbackRating).filter(Objects::nonNull)
                .mapToInt(Integer::intValue).average().orElse(0);
        return new Overview(total, open, critical, resolved.size(), breached, unassigned, awaiting,
                round(compliance), round(mtta), round(mttr), round(rating));
    }

    public Map<String, Long> openByPriority() {
        Map<String, Long> result = new LinkedHashMap<>();
        for (Priority p : Priority.values()) result.put(p.name(), 0L);
        for (Object[] row : incidents.countByPriorityAndStatusIn(IncidentStatus.UNRESOLVED)) {
            result.put(((Priority) row[0]).name(), (Long) row[1]);
        }
        return result;
    }

    public Map<String, Long> byStatus() {
        Map<String, Long> result = new LinkedHashMap<>();
        for (IncidentStatus s : IncidentStatus.values()) result.put(s.getLabel(), 0L);
        for (Object[] row : incidents.countByStatus()) {
            result.put(((IncidentStatus) row[0]).getLabel(), (Long) row[1]);
        }
        return result;
    }

    public Map<String, Long> byCategory() {
        Map<String, Long> result = new LinkedHashMap<>();
        for (Object[] row : incidents.countByCategory()) {
            result.put((String) row[0], (Long) row[1]);
        }
        return result;
    }

    public List<ServiceRow> byService() {
        return incidents.countByService(IncidentStatus.UNRESOLVED).stream()
                .map(r -> new ServiceRow((String) r[0], ((Number) r[1]).longValue(), r[2] == null ? 0 : ((Number) r[2]).longValue()))
                .toList();
    }

    public List<TechnicianRow> technicianWorkload() {
        Map<Long, Integer> load = assignments.workloads();
        Map<Long, List<Incident>> resolvedBy = incidents.findAllResolved().stream()
                .filter(i -> i.getAssignee() != null)
                .collect(Collectors.groupingBy(i -> i.getAssignee().getId()));
        Map<Long, Long> breachesBy = violations.findAll().stream()
                .filter(v -> v.getTechnician() != null)
                .collect(Collectors.groupingBy(v -> v.getTechnician().getId(), Collectors.counting()));
        List<TechnicianRow> rows = new ArrayList<>();
        for (Technician t : users.findActiveTechnicians()) {
            List<Incident> done = resolvedBy.getOrDefault(t.getId(), List.of());
            double avg = average(done.stream().map(i -> Duration.between(i.getCreatedAt(), i.getResolvedAt())));
            double rating = done.stream().map(Incident::getFeedbackRating).filter(Objects::nonNull)
                    .mapToInt(Integer::intValue).average().orElse(0);
            rows.add(new TechnicianRow(t, load.getOrDefault(t.getId(), 0), done.size(), round(avg),
                    breachesBy.getOrDefault(t.getId(), 0L), round(rating)));
        }
        rows.sort(Comparator.comparingInt(TechnicianRow::active).reversed());
        return rows;
    }

    public List<PriorityRow> slaByPriority() {
        List<Incident> resolved = incidents.findAllResolved();
        List<PriorityRow> rows = new ArrayList<>();
        for (Priority p : Priority.values()) {
            List<Incident> list = resolved.stream().filter(i -> i.getPriority() == p).toList();
            double avg = average(list.stream().map(i -> Duration.between(i.getCreatedAt(), i.getResolvedAt())));
            double compliance = list.isEmpty() ? 100
                    : 100.0 * list.stream().filter(i -> !i.getSla().isBreached()).count() / list.size();
            rows.add(new PriorityRow(p, list.size(), round(avg), round(compliance)));
        }
        return rows;
    }

    public Trend trend() {
        LocalDate today = LocalDate.now(clock);
        LocalDateTime since = today.minusDays(TREND_DAYS - 1).atStartOfDay();
        Map<LocalDate, Long> created = incidents.findByCreatedAtAfter(since).stream()
                .collect(Collectors.groupingBy(i -> i.getCreatedAt().toLocalDate(), Collectors.counting()));
        Map<LocalDate, Long> resolved = incidents.findByResolvedAtAfter(since).stream()
                .collect(Collectors.groupingBy(i -> i.getResolvedAt().toLocalDate(), Collectors.counting()));
        List<String> labels = new ArrayList<>();
        List<Long> c = new ArrayList<>();
        List<Long> r = new ArrayList<>();
        for (int d = TREND_DAYS - 1; d >= 0; d--) {
            LocalDate day = today.minusDays(d);
            labels.add(day.getDayOfMonth() + " " + day.getMonth().name().substring(0, 3));
            c.add(created.getOrDefault(day, 0L));
            r.add(resolved.getOrDefault(day, 0L));
        }
        return new Trend(labels, c, r);
    }

    private static double average(java.util.stream.Stream<Duration> durations) {
        return durations.mapToLong(Duration::toMinutes).average().orElse(0);
    }

    private static double round(double value) {
        return Math.round(value * 10) / 10.0;
    }
}
