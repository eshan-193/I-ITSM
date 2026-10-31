package com.itsm.web.api;

import com.itsm.intelligence.CorrelationEngine;
import com.itsm.intelligence.ServiceHealth;
import com.itsm.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

/** Data for the live dashboard charts and the service dependency graph. */
@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "Dashboard")
@PreAuthorize("hasAnyRole('ADMIN','TECHNICIAN')")
public class DashboardApiController {

    private final ReportService reports;
    private final CorrelationEngine correlation;

    public DashboardApiController(ReportService reports, CorrelationEngine correlation) {
        this.reports = reports;
        this.correlation = correlation;
    }

    @GetMapping
    @Operation(summary = "Headline numbers and chart series for the operations dashboard")
    public Map<String, Object> stats() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("overview", reports.overview());
        result.put("openByPriority", reports.openByPriority());
        result.put("byCategory", reports.byCategory());
        result.put("byStatus", reports.byStatus());
        result.put("trend", reports.trend());
        result.put("workload", reports.technicianWorkload().stream()
                .map(r -> Map.of("name", r.technician().getName(), "active", r.active(),
                        "max", r.technician().getMaxActiveIncidents()))
                .toList());
        return result;
    }

    @GetMapping("/graph")
    @Operation(summary = "Service dependency graph with live health")
    public Map<String, Object> graph() {
        List<Map<String, Object>> nodes = new ArrayList<>();
        List<Map<String, Object>> edges = new ArrayList<>();
        for (ServiceHealth h : correlation.serviceHealth()) {
            nodes.add(Map.of("id", h.service().getId(), "label", h.service().getName(),
                    "status", h.status().name(), "open", h.openIncidents(),
                    "criticality", h.service().getCriticality().name()));
            h.service().getDependsOn().forEach(d -> edges.add(Map.of("from", h.service().getId(), "to", d.getId())));
        }
        return Map.of("nodes", nodes, "edges", edges);
    }
}
