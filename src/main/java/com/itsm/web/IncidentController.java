package com.itsm.web;

import com.itsm.domain.incident.*;
import com.itsm.domain.priority.Priority;
import com.itsm.domain.sla.SlaStatus;
import com.itsm.domain.user.Role;
import com.itsm.domain.user.Technician;
import com.itsm.domain.user.User;
import com.itsm.exception.DuplicateIncidentException;
import com.itsm.exception.ValidationException;
import com.itsm.intelligence.*;
import com.itsm.security.CurrentUserService;
import com.itsm.service.*;
import com.itsm.web.form.ReportIncidentForm;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.time.Clock;
import java.time.LocalDateTime;

/** Every page and action about incidents, shared by all roles; what you see depends on who you are. */
@Controller
@RequestMapping("/incidents")
public class IncidentController {

    private final CurrentUserService currentUser;
    private final IncidentService incidentService;
    private final IncidentSearchService searchService;
    private final CatalogService catalog;
    private final UserService userService;
    private final AssignmentService assignmentService;
    private final SlaService slaService;
    private final SlaRiskPredictor riskPredictor;
    private final DuplicateDetector duplicateDetector;
    private final CorrelationEngine correlationEngine;
    private final ResolutionAdvisor resolutionAdvisor;
    private final IncidentDraftExtractor draftExtractor;
    private final ViewHelper fmt;
    private final Clock clock;

    public IncidentController(CurrentUserService currentUser, IncidentService incidentService,
                              IncidentSearchService searchService, CatalogService catalog, UserService userService,
                              AssignmentService assignmentService, SlaService slaService, SlaRiskPredictor riskPredictor,
                              DuplicateDetector duplicateDetector, CorrelationEngine correlationEngine,
                              ResolutionAdvisor resolutionAdvisor, IncidentDraftExtractor draftExtractor, ViewHelper fmt,
                              Clock clock) {
        this.currentUser = currentUser;
        this.incidentService = incidentService;
        this.searchService = searchService;
        this.catalog = catalog;
        this.userService = userService;
        this.assignmentService = assignmentService;
        this.slaService = slaService;
        this.riskPredictor = riskPredictor;
        this.duplicateDetector = duplicateDetector;
        this.correlationEngine = correlationEngine;
        this.resolutionAdvisor = resolutionAdvisor;
        this.draftExtractor = draftExtractor;
        this.fmt = fmt;
        this.clock = clock;
    }

    // ================================================================== search

    @GetMapping
    public String search(@ModelAttribute("criteria") IncidentSearchCriteria criteria, Model model) {
        User me = currentUser.require();
        List<Incident> results = searchService.search(criteria, me);
        model.addAttribute("results", results);
        model.addAttribute("risk", riskPredictor.assess(results));
        addSearchOptions(model);
        return "incidents/list";
    }

    @GetMapping("/export.csv")
    public void exportCsv(@ModelAttribute IncidentSearchCriteria criteria, HttpServletResponse response) throws IOException {
        User me = currentUser.require();
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=incidents.csv");
        PrintWriter out = response.getWriter();
        out.println("Number,Title,Status,Priority,Category,Service,Reporter,Assignee,Created,Resolved,SLA");
        for (Incident i : searchService.search(criteria, me)) {
            out.println(String.join(",", i.getNumber(), csv(i.getTitle()), i.getStatus().name(),
                    i.getPriority() == null ? "" : i.getPriority().name(),
                    i.getCategory() == null ? "" : csv(i.getCategory().getName()), csv(i.getService().getName()),
                    csv(i.getReporter().getName()), i.getAssignee() == null ? "" : csv(i.getAssignee().getName()),
                    String.valueOf(i.getCreatedAt()), i.getResolvedAt() == null ? "" : String.valueOf(i.getResolvedAt()),
                    slaService.statusOf(i).name()));
        }
    }

    private static String csv(String value) {
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private void addSearchOptions(Model model) {
        model.addAttribute("statuses", IncidentStatus.values());
        model.addAttribute("priorities", Priority.values());
        model.addAttribute("slaStatuses", SlaStatus.values());
        model.addAttribute("categories", catalog.allCategories());
        model.addAttribute("services", catalog.allServices());
        model.addAttribute("technicians", userService.technicians());
    }

    // ================================================================== reporting

    @GetMapping("/new")
    public String newForm(Model model) {
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new ReportIncidentForm());
        }
        addFormOptions(model);
        return "incidents/new";
    }

    @PostMapping
    public String report(@Valid @ModelAttribute("form") ReportIncidentForm form, BindingResult result,
                         Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            addFormOptions(model);
            return "incidents/new";
        }
        try {
            Incident incident = incidentService.report(form.toCommand(), currentUser.require());
            String assigned = incident.getAssignee() == null ? "awaiting assignment"
                    : "assigned to " + incident.getAssignee().getName();
            redirect.addFlashAttribute("success", incident.getNumber() + " created as " + incident.getPriority()
                    + " (" + incident.getPriority().getLabel() + ") and " + assigned + ".");
            return "redirect:/incidents/" + incident.getId();
        } catch (DuplicateIncidentException e) {
            model.addAttribute("ownDuplicate", e.getExistingIncidentNumber());
            result.reject("duplicate", e.getMessage() + ". Tick the box below if this really is a different problem.");
            addFormOptions(model);
            return "incidents/new";
        }
    }

    /** AI Quick Report: free text in, pre-filled form out. */
    @GetMapping("/quick")
    public String quickForm() {
        return "incidents/quick";
    }

    @PostMapping("/quick")
    public String quickReport(@RequestParam String text, RedirectAttributes redirect) {
        if (text == null || text.trim().length() < 10) {
            throw new ValidationException("Please describe the problem in a sentence or two");
        }
        IncidentDraft draft = draftExtractor.extract(text.trim());
        ReportIncidentForm form = new ReportIncidentForm();
        form.setTitle(draft.title());
        form.setDescription(draft.description());
        form.setServiceId(draft.service() == null ? null : draft.service().getId());
        form.setCategoryId(draft.category() == null ? null : draft.category().getId());
        form.setImpact(draft.impact());
        form.setUrgency(draft.urgency());
        form.setAffectedUsers(draft.affectedUsers());
        redirect.addFlashAttribute("form", form);
        redirect.addFlashAttribute("info", "Form pre-filled from your description. Please check it before submitting.");
        return "redirect:/incidents/new";
    }

    private void addFormOptions(Model model) {
        model.addAttribute("services", catalog.activeServices());
        model.addAttribute("categories", catalog.activeCategories());
        model.addAttribute("impacts", Impact.values());
        model.addAttribute("urgencies", Urgency.values());
    }

    // ================================================================== detail

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        User me = currentUser.require();
        Incident incident = incidentService.getForViewer(id, me);
        boolean staff = me.getRole() != Role.EMPLOYEE;
        boolean canAssign = me.getRole() == Role.ADMIN || (me instanceof Technician t && t.isTeamLead());

        model.addAttribute("incident", incident);
        model.addAttribute("can", incident.allowedActions().stream().map(Enum::name).collect(java.util.stream.Collectors.toSet()));
        model.addAttribute("canWork", me.canWorkOn(incident));
        model.addAttribute("isReporter", me.isReporterOf(incident) || me.getRole() == Role.ADMIN);
        model.addAttribute("staff", staff);
        model.addAttribute("canAssign", canAssign);
        model.addAttribute("comments", incident.getComments().stream().filter(c -> c.isVisibleTo(me)).toList());
        model.addAttribute("slaStatus", slaService.statusOf(incident));
        model.addAttribute("slaUsed", fmt.percent(incident.getSla().resolutionElapsedFraction(LocalDateTime.now(clock))));
        model.addAttribute("commentTypes", staff
                ? List.of(CommentType.UPDATE, CommentType.INVESTIGATION_NOTE) : List.of(CommentType.UPDATE));

        if (incident.isUnresolved()) {
            model.addAttribute("slaRisk", riskPredictor.assess(incident));
        }
        if (staff) {
            if (incident.isUnresolved()) {
                model.addAttribute("duplicates", duplicateDetector.findPossibleDuplicates(
                        incident.getTitle(), incident.getDescription(), incident.getService(), incident.getId()));
                model.addAttribute("findings", correlationEngine.analyse(incident));
            }
            model.addAttribute("knowledge", resolutionAdvisor.similarResolved(incident.getTitle(), incident.getDescription(), incident.getId()));
            model.addAttribute("categories", catalog.activeCategories());
            model.addAttribute("impacts", Impact.values());
            model.addAttribute("urgencies", Urgency.values());
        }
        if (canAssign && incident.allows(IncidentAction.ASSIGN)) {
            model.addAttribute("recommendations", assignmentService.recommendFor(incident));
            model.addAttribute("technicians", userService.technicians());
            model.addAttribute("workloads", assignmentService.workloads());
        }
        return "incidents/detail";
    }

    // ================================================================== lifecycle actions

    @PostMapping("/{id}/acknowledge")
    public String acknowledge(@PathVariable Long id, RedirectAttributes r) {
        incidentService.acknowledge(id, currentUser.require());
        return done(id, r, "Incident acknowledged; response SLA met.");
    }

    @PostMapping("/{id}/start")
    public String start(@PathVariable Long id, RedirectAttributes r) {
        incidentService.startWork(id, currentUser.require());
        return done(id, r, "Investigation started.");
    }

    @PostMapping("/{id}/request-info")
    public String requestInfo(@PathVariable Long id, @RequestParam String question, RedirectAttributes r) {
        incidentService.requestInfo(id, currentUser.require(), question);
        return done(id, r, "Question sent to the reporter; SLA clock paused.");
    }

    @PostMapping("/{id}/provide-info")
    public String provideInfo(@PathVariable Long id, @RequestParam String information, RedirectAttributes r) {
        incidentService.provideInfo(id, currentUser.require(), information);
        return done(id, r, "Thank you; the technician has been notified.");
    }

    @PostMapping("/{id}/resolve")
    public String resolve(@PathVariable Long id, @RequestParam String resolution,
                          @RequestParam(required = false) String rootCause, RedirectAttributes r) {
        incidentService.resolve(id, currentUser.require(), resolution, rootCause);
        return done(id, r, "Incident resolved; the reporter has been asked to confirm.");
    }

    @PostMapping("/{id}/verify")
    public String verify(@PathVariable Long id, @RequestParam int rating,
                         @RequestParam(required = false) String feedback, RedirectAttributes r) {
        incidentService.verify(id, currentUser.require(), rating, feedback);
        return done(id, r, "Thanks for your feedback!");
    }

    @PostMapping("/{id}/reopen")
    public String reopen(@PathVariable Long id, @RequestParam String reason, RedirectAttributes r) {
        incidentService.reopen(id, currentUser.require(), reason);
        return done(id, r, "Incident reopened; the technician will continue working on it.");
    }

    @PostMapping("/{id}/close")
    public String close(@PathVariable Long id, @RequestParam(required = false) String note, RedirectAttributes r) {
        incidentService.close(id, currentUser.require(), note);
        return done(id, r, "Incident closed.");
    }

    @PostMapping("/{id}/duplicate")
    public String closeAsDuplicate(@PathVariable Long id, @RequestParam String originalNumber, RedirectAttributes r) {
        incidentService.closeAsDuplicate(id, originalNumber, currentUser.require());
        return done(id, r, "Closed as duplicate; the reporter now follows " + originalNumber.toUpperCase() + ".");
    }

    @PostMapping("/{id}/assign")
    public String assign(@PathVariable Long id, @RequestParam Long technicianId,
                         @RequestParam(defaultValue = "false") boolean override, RedirectAttributes r) {
        Incident i = incidentService.assign(id, technicianId, currentUser.require(), override);
        return done(id, r, "Assigned to " + i.getAssignee().getName() + ".");
    }

    @PostMapping("/{id}/auto-assign")
    public String autoAssign(@PathVariable Long id, RedirectAttributes r) {
        Incident i = incidentService.autoAssign(id, currentUser.require());
        return done(id, r, "Auto-assigned to " + i.getAssignee().getName() + ".");
    }

    @PostMapping("/{id}/triage")
    public String triage(@PathVariable Long id, @RequestParam(required = false) Long categoryId,
                         @RequestParam Impact impact, @RequestParam Urgency urgency,
                         @RequestParam int affectedUsers, RedirectAttributes r) {
        Incident i = incidentService.triage(id, categoryId, impact, urgency, affectedUsers, currentUser.require());
        return done(id, r, "Classification updated; priority is now " + i.getPriority() + ".");
    }

    @PostMapping("/{id}/join")
    public String join(@PathVariable Long id, RedirectAttributes r) {
        Incident i = incidentService.join(id, currentUser.require());
        return done(id, r, "You are now following " + i.getNumber() + " and will be notified when it is fixed.");
    }

    @PostMapping("/{id}/comments")
    public String comment(@PathVariable Long id, @RequestParam String text,
                          @RequestParam(defaultValue = "UPDATE") CommentType type, RedirectAttributes r) {
        incidentService.addComment(id, currentUser.require(), text, type);
        return done(id, r, "Comment added.");
    }

    private static String done(Long id, RedirectAttributes redirect, String message) {
        redirect.addFlashAttribute("success", message);
        return "redirect:/incidents/" + id;
    }
}
