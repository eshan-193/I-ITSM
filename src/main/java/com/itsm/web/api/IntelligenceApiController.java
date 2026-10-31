package com.itsm.web.api;

import com.itsm.domain.catalog.ItService;
import com.itsm.domain.incident.Impact;
import com.itsm.domain.incident.Incident;
import com.itsm.domain.incident.Urgency;
import com.itsm.domain.priority.PriorityResult;
import com.itsm.domain.sla.SlaPolicy;
import com.itsm.intelligence.*;
import com.itsm.security.CurrentUserService;
import com.itsm.service.CatalogService;
import com.itsm.service.IncidentService;
import com.itsm.service.PriorityService;
import com.itsm.service.SlaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Live analysis used by the "report incident" form while the employee types:
 * suggested category, predicted priority and SLA, possible duplicates and known fixes.
 */
@RestController
@RequestMapping("/api/intelligence")
@Tag(name = "Intelligence")
public class IntelligenceApiController {

    private final CategorySuggestionService categories;
    private final DuplicateDetector duplicates;
    private final ResolutionAdvisor advisor;
    private final PriorityService priorityService;
    private final SlaService slaService;
    private final CatalogService catalog;
    private final IncidentService incidents;
    private final CurrentUserService currentUser;

    public IntelligenceApiController(CategorySuggestionService categories, DuplicateDetector duplicates,
                                     ResolutionAdvisor advisor, PriorityService priorityService, SlaService slaService,
                                     CatalogService catalog, IncidentService incidents, CurrentUserService currentUser) {
        this.categories = categories;
        this.duplicates = duplicates;
        this.advisor = advisor;
        this.priorityService = priorityService;
        this.slaService = slaService;
        this.catalog = catalog;
        this.incidents = incidents;
        this.currentUser = currentUser;
    }

    public record AnalyzeRequest(String title, String description, Long serviceId, Impact impact, Urgency urgency,
                                 Integer affectedUsers) {
    }

    @PostMapping("/analyze")
    @Operation(summary = "Analyse a draft incident before it is submitted")
    public Map<String, Object> analyze(@RequestBody AnalyzeRequest req) {
        currentUser.require();
        String title = req.title() == null ? "" : req.title();
        String description = req.description() == null ? "" : req.description();
        Map<String, Object> result = new LinkedHashMap<>();

        if ((title + description).trim().length() >= 8) {
            categories.suggestOffline(title + ". " + description).ifPresent(s -> result.put("category", Map.of(
                    "id", s.category().getId(), "name", s.category().getName(),
                    "confidence", s.confidencePercent(), "source", s.source().getLabel(),
                    "explanation", s.explanation())));
        }

        ItService service = req.serviceId() == null ? null : catalog.getService(req.serviceId());
        if (service != null && req.impact() != null && req.urgency() != null) {
            int users = req.affectedUsers() == null || req.affectedUsers() < 1 ? 1 : req.affectedUsers();
            PriorityResult p = priorityService.calculate(req.impact(), req.urgency(), service, users);
            Map<String, Object> priority = new LinkedHashMap<>();
            priority.put("level", p.priority().name());
            priority.put("label", p.priority().getLabel());
            priority.put("color", p.priority().getBadgeColor());
            priority.put("rationale", p.rationale());
            try {
                SlaPolicy policy = slaService.policyFor(p.priority(), service);
                priority.put("responseMinutes", policy.getResponseMinutes());
                priority.put("resolutionMinutes", policy.getResolutionMinutes());
            } catch (RuntimeException ignored) {
                // SLA shown as unknown
            }
            result.put("priority", priority);
        }

        if (title.length() >= 4) {
            List<Map<String, Object>> dups = duplicates.findPossibleDuplicates(title, description, service, null).stream()
                    .map(s -> Map.<String, Object>of("id", s.incident().getId(), "number", s.incident().getNumber(),
                            "title", s.incident().getTitle(), "status", s.incident().getStatus().getLabel(),
                            "service", s.incident().getService().getName(), "similarity", s.similarityPercent()))
                    .toList();
            result.put("duplicates", dups);

            List<Map<String, Object>> fixes = advisor.similarResolved(title, description, null).stream()
                    .map(s -> Map.<String, Object>of("number", s.incident().getNumber(), "title", s.incident().getTitle(),
                            "resolution", s.incident().getResolution(), "similarity", s.similarityPercent()))
                    .toList();
            result.put("knownFixes", fixes);
        }
        return result;
    }

    @GetMapping("/incidents/{id}/advice")
    @Operation(summary = "AI-generated troubleshooting steps for an incident (requires GEMINI_API_KEY)")
    public Map<String, Object> advice(@PathVariable Long id) {
        Incident incident = incidents.getForViewer(id, currentUser.require());
        return advisor.aiAdvice(incident)
                .<Map<String, Object>>map(a -> Map.of("available", true, "likelyCause", a.likelyCause(),
                        "steps", a.steps(), "provider", a.provider()))
                .orElseGet(() -> Map.of("available", false,
                        "message", "AI is unavailable right now (not configured, or the provider is busy). "
                                + "The known fixes from similar incidents still apply; try again in a few minutes."));
    }
}
