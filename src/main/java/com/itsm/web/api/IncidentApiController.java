package com.itsm.web.api;

import com.itsm.domain.incident.Incident;
import com.itsm.security.CurrentUserService;
import com.itsm.service.IncidentSearchCriteria;
import com.itsm.service.IncidentSearchService;
import com.itsm.service.IncidentService;
import com.itsm.web.form.ReportIncidentForm;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API for integrations (monitoring tools, chatbots, mobile apps).
 * Uses the same services and permission checks as the web UI.
 */
@RestController
@RequestMapping("/api/incidents")
@Tag(name = "Incidents")
public class IncidentApiController {

    private final CurrentUserService currentUser;
    private final IncidentService incidents;
    private final IncidentSearchService search;

    public IncidentApiController(CurrentUserService currentUser, IncidentService incidents, IncidentSearchService search) {
        this.currentUser = currentUser;
        this.incidents = incidents;
        this.search = search;
    }

    @GetMapping
    @Operation(summary = "Search incidents visible to the caller (all filters optional)")
    public List<IncidentDto> search(@ModelAttribute IncidentSearchCriteria criteria) {
        return search.search(criteria, currentUser.require()).stream().map(IncidentDto::from).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one incident")
    public IncidentDto get(@PathVariable Long id) {
        return IncidentDto.from(incidents.getForViewer(id, currentUser.require()));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Report a new incident; it is classified, prioritised and assigned automatically")
    public IncidentDto create(@Valid @RequestBody ReportIncidentForm form) {
        Incident created = incidents.report(form.toCommand(), currentUser.require());
        return IncidentDto.from(created);
    }
}
