package com.itsm.intelligence;

import com.itsm.domain.catalog.ItService;
import com.itsm.domain.incident.Incident;
import com.itsm.domain.incident.IncidentStatus;
import com.itsm.domain.priority.Priority;
import com.itsm.repository.IncidentRepository;
import com.itsm.repository.ItServiceRepository;
import com.itsm.service.SettingKey;
import com.itsm.service.SettingsService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Relates incidents through the service dependency graph.
 * <p>
 * If "Payment Gateway" depends on "Core Database" and the database already has an open
 * incident, a new payment incident probably shares that root cause. If several
 * incidents hit one service within a short window, a major outage is likely.
 */
@Component
@Transactional(readOnly = true)
public class CorrelationEngine {

    private final IncidentRepository incidents;
    private final ItServiceRepository services;
    private final SettingsService settings;

    public CorrelationEngine(IncidentRepository incidents, ItServiceRepository services, SettingsService settings) {
        this.incidents = incidents;
        this.services = services;
        this.settings = settings;
    }

    public List<CorrelationFinding> analyse(Incident incident) {
        List<CorrelationFinding> findings = new ArrayList<>();
        ItService service = incident.getService();

        for (ItService upstream : service.getDependsOn()) {
            List<Incident> open = openOn(upstream, incident);
            if (!open.isEmpty()) {
                findings.add(new CorrelationFinding(CorrelationFinding.Type.UPSTREAM_CAUSE,
                        "Probable root cause: " + numbers(open) + " open on " + upstream.getName()
                                + ", which " + service.getName() + " depends on", open));
            }
        }

        for (ItService downstream : services.findDependents(service)) {
            List<Incident> open = openOn(downstream, incident);
            if (!open.isEmpty()) {
                findings.add(new CorrelationFinding(CorrelationFinding.Type.DOWNSTREAM_IMPACT,
                        "Likely knock-on effect: " + numbers(open) + " on " + downstream.getName()
                                + ", which depends on " + service.getName(), open));
            }
        }

        LocalDateTime windowStart = incident.getCreatedAt().minusMinutes(settings.getInt(SettingKey.CLUSTER_WINDOW_MINUTES));
        List<Incident> cluster = openOn(service, incident).stream()
                .filter(i -> !i.getCreatedAt().isBefore(windowStart))
                .toList();
        int threshold = settings.getInt(SettingKey.CLUSTER_SIZE);
        if (cluster.size() + 1 >= threshold) {
            findings.add(new CorrelationFinding(CorrelationFinding.Type.OUTAGE_CLUSTER,
                    "Possible major outage: " + (cluster.size() + 1) + " incidents on " + service.getName()
                            + " within " + settings.getInt(SettingKey.CLUSTER_WINDOW_MINUTES) + " minutes", cluster));
        }
        return findings;
    }

    /** Health of every active service, for the dashboard and dependency graph. */
    public List<ServiceHealth> serviceHealth() {
        Map<ItService, List<Incident>> openByService = incidents.findByStatusIn(IncidentStatus.UNRESOLVED).stream()
                .collect(Collectors.groupingBy(Incident::getService));
        List<ServiceHealth> result = new ArrayList<>();
        for (ItService s : services.findByActiveTrueOrderByName()) {
            List<Incident> open = openByService.getOrDefault(s, List.of());
            Priority worst = open.stream().map(Incident::getPriority).filter(Objects::nonNull)
                    .min(Comparator.naturalOrder()).orElse(null);
            ServiceHealth.Status status;
            if (worst == Priority.P1 || open.size() >= settings.getInt(SettingKey.CLUSTER_SIZE)) {
                status = ServiceHealth.Status.OUTAGE;
            } else if (!open.isEmpty()) {
                status = ServiceHealth.Status.DEGRADED;
            } else {
                status = ServiceHealth.Status.OPERATIONAL;
            }
            result.add(new ServiceHealth(s, open.size(), worst, status));
        }
        return result;
    }

    private List<Incident> openOn(ItService service, Incident exclude) {
        return incidents.findByServiceAndStatusIn(service, IncidentStatus.UNRESOLVED).stream()
                .filter(i -> !i.equals(exclude))
                .toList();
    }

    private static String numbers(List<Incident> list) {
        return list.stream().limit(3).map(Incident::getNumber).collect(Collectors.joining(", "))
                + (list.size() > 3 ? " +" + (list.size() - 3) + " more" : "");
    }
}
