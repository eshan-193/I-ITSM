package com.itsm.service;

import com.itsm.domain.assignment.RankedTechnician;
import com.itsm.domain.catalog.Category;
import com.itsm.domain.catalog.ItService;
import com.itsm.domain.event.*;
import com.itsm.domain.incident.*;
import com.itsm.domain.priority.Priority;
import com.itsm.domain.priority.PriorityResult;
import com.itsm.domain.user.Role;
import com.itsm.domain.user.Technician;
import com.itsm.domain.user.User;
import com.itsm.exception.*;
import com.itsm.intelligence.*;
import com.itsm.repository.CategoryRepository;
import com.itsm.repository.IncidentRepository;
import com.itsm.repository.ItServiceRepository;
import com.itsm.repository.UserRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Use cases for the incident lifecycle.
 * <p>
 * Each method follows the same shape: load → check permission → call a domain method
 * (which enforces the state rules) → publish an event. The service never writes audit
 * entries or sends notifications itself; listeners do that (Observer pattern).
 */
@Service
@Transactional
public class IncidentService {

    private static final double OWN_DUPLICATE_SIMILARITY = 0.8;

    private final IncidentRepository incidents;
    private final ItServiceRepository services;
    private final CategoryRepository categories;
    private final UserRepository users;
    private final PriorityService priorityService;
    private final SlaService slaService;
    private final AssignmentService assignmentService;
    private final SettingsService settings;
    private final CategorySuggestionService categorySuggester;
    private final DuplicateDetector duplicateDetector;
    private final CorrelationEngine correlationEngine;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public IncidentService(IncidentRepository incidents, ItServiceRepository services, CategoryRepository categories,
                           UserRepository users, PriorityService priorityService, SlaService slaService,
                           AssignmentService assignmentService, SettingsService settings,
                           CategorySuggestionService categorySuggester, DuplicateDetector duplicateDetector,
                           CorrelationEngine correlationEngine, ApplicationEventPublisher events, Clock clock) {
        this.incidents = incidents;
        this.services = services;
        this.categories = categories;
        this.users = users;
        this.priorityService = priorityService;
        this.slaService = slaService;
        this.assignmentService = assignmentService;
        this.settings = settings;
        this.categorySuggester = categorySuggester;
        this.duplicateDetector = duplicateDetector;
        this.correlationEngine = correlationEngine;
        this.events = events;
        this.clock = clock;
    }

    // ================================================================== reporting

    /**
     * The full automated pipeline from the problem scenario:
     * create → classify → prioritise → SLA → assign → notify → correlate.
     */
    public Incident report(ReportIncidentCommand cmd, User reporter) {
        ItService service = services.findById(cmd.serviceId())
                .filter(ItService::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("IT service", cmd.serviceId()));
        if (!cmd.confirmedNotDuplicate()) {
            rejectOwnDuplicate(cmd, reporter, service);
        }
        LocalDateTime now = now();
        Incident incident = incidents.save(new Incident(cmd.title(), cmd.description(), reporter, service,
                cmd.impact(), cmd.urgency(), cmd.affectedUsers(), now));
        publish(new IncidentCreatedEvent(incident, reporter, now));

        classifyNew(incident, cmd.categoryId(), reporter, now);
        prioritise(incident, null, now);
        if (incident.getStatus() == IncidentStatus.CLASSIFIED && settings.getBoolean(SettingKey.AUTO_ASSIGN)) {
            tryAutoAssign(incident, now);
        }
        correlate(incident, now);
        return incident;
    }

    private void rejectOwnDuplicate(ReportIncidentCommand cmd, User reporter, ItService service) {
        var newTerms = TextAnalyzer.termSet(cmd.title());
        for (Incident existing : incidents.findByReporterAndStatusIn(reporter, IncidentStatus.UNRESOLVED)) {
            if (existing.getService().equals(service)
                    && TextAnalyzer.jaccard(newTerms, TextAnalyzer.termSet(existing.getTitle())) >= OWN_DUPLICATE_SIMILARITY) {
                throw new DuplicateIncidentException(existing.getNumber());
            }
        }
    }

    private void classifyNew(Incident incident, Long categoryId, User reporter, LocalDateTime now) {
        IncidentStatus from = incident.getStatus();
        User actor = null;
        if (categoryId != null) {
            incident.classify(findCategory(categoryId), ClassificationSource.MANUAL, 1.0, now);
            actor = reporter;
        } else {
            Optional<CategorySuggestion> suggestion = categorySuggester.suggest(incident.getTitle() + ". " + incident.getDescription());
            if (suggestion.isPresent()) {
                CategorySuggestion s = suggestion.get();
                incident.classify(s.category(), s.source(), s.confidence(), now);
            } else if (incident.getService().getDefaultCategory() != null) {
                incident.classify(incident.getService().getDefaultCategory(), ClassificationSource.SERVICE_DEFAULT, 0.5, now);
            } else {
                return; // stays NEW until a technician classifies it
            }
        }
        publish(new IncidentClassifiedEvent(incident, actor, now, from));
    }

    private void prioritise(Incident incident, User actor, LocalDateTime now) {
        Priority previous = incident.getPriority();
        PriorityResult result = priorityService.calculate(incident);
        incident.applyPriority(result, slaService.policyFor(result.priority(), incident.getService()), now);
        if (previous != result.priority()) {
            publish(new PriorityChangedEvent(incident, actor, now, previous));
        }
    }

    private void tryAutoAssign(Incident incident, LocalDateTime now) {
        Optional<RankedTechnician> choice = assignmentService.selectFor(incident);
        String strategy = assignmentService.activeStrategy().displayName();
        if (choice.isEmpty()) {
            publish(new IncidentUnassignedEvent(incident, now, strategy));
            return;
        }
        IncidentStatus from = incident.getStatus();
        incident.assignTo(choice.get().candidate().technician(), now);
        publish(new IncidentAssignedEvent(incident, null, now, from, strategy, choice.get().reason()));
    }

    private void correlate(Incident incident, LocalDateTime now) {
        for (SimilarIncident dup : duplicateDetector.findPossibleDuplicates(
                incident.getTitle(), incident.getDescription(), incident.getService(), incident.getId())) {
            publish(new RelatedIncidentsDetectedEvent(incident, now,
                    "Possible duplicate of " + dup.incident().getNumber() + " (" + dup.similarityPercent() + "% similar)", false));
            break; // report the strongest match only
        }
        for (CorrelationFinding finding : correlationEngine.analyse(incident)) {
            publish(new RelatedIncidentsDetectedEvent(incident, now, finding.message(), finding.isMajor()));
        }
    }

    // ================================================================== lifecycle

    public Incident acknowledge(Long id, User actor) {
        Incident incident = load(id);
        requireWorker(actor, incident);
        IncidentStatus from = incident.getStatus();
        incident.acknowledge(now());
        publish(new IncidentStatusChangedEvent(incident, actor, now(), from, IncidentAction.ACKNOWLEDGE, null));
        return incident;
    }

    public Incident startWork(Long id, User actor) {
        Incident incident = load(id);
        requireWorker(actor, incident);
        IncidentStatus from = incident.getStatus();
        incident.startWork(now());
        publish(new IncidentStatusChangedEvent(incident, actor, now(), from, IncidentAction.START_WORK, null));
        return incident;
    }

    public Incident requestInfo(Long id, User actor, String question) {
        Incident incident = load(id);
        requireWorker(actor, incident);
        IncidentStatus from = incident.getStatus();
        LocalDateTime now = now();
        incident.requestInfo(now);
        incident.addComment(actor, question, CommentType.INFO_REQUEST, now);
        publish(new IncidentStatusChangedEvent(incident, actor, now, from, IncidentAction.REQUEST_INFO, question));
        return incident;
    }

    public Incident provideInfo(Long id, User actor, String information) {
        Incident incident = load(id);
        requireReporterOrAdmin(actor, incident);
        IncidentStatus from = incident.getStatus();
        LocalDateTime now = now();
        incident.provideInfo(now);
        incident.addComment(actor, information, CommentType.INFO_RESPONSE, now);
        publish(new IncidentStatusChangedEvent(incident, actor, now, from, IncidentAction.PROVIDE_INFO, null));
        return incident;
    }

    public Incident resolve(Long id, User actor, String resolution, String rootCause) {
        Incident incident = load(id);
        requireWorker(actor, incident);
        IncidentStatus from = incident.getStatus();
        incident.resolve(resolution, rootCause, now());
        publish(new IncidentStatusChangedEvent(incident, actor, now(), from, IncidentAction.RESOLVE, resolution));
        return incident;
    }

    public Incident verify(Long id, User actor, int rating, String feedback) {
        Incident incident = load(id);
        requireReporterOrAdmin(actor, incident);
        IncidentStatus from = incident.getStatus();
        incident.verify(rating, feedback, now());
        publish(new IncidentStatusChangedEvent(incident, actor, now(), from, IncidentAction.VERIFY, feedback));
        return incident;
    }

    public Incident reopen(Long id, User actor, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new ValidationException("Please say why the resolution did not work");
        }
        Incident incident = load(id);
        requireReporterOrAdmin(actor, incident);
        IncidentStatus from = incident.getStatus();
        LocalDateTime now = now();
        incident.reopen(now);
        incident.addComment(actor, reason, CommentType.UPDATE, now);
        publish(new IncidentStatusChangedEvent(incident, actor, now, from, IncidentAction.REOPEN, reason));
        return incident;
    }

    public Incident close(Long id, User actor, String note) {
        Incident incident = load(id);
        if (!(actor.isReporterOf(incident) || actor.canWorkOn(incident))) {
            throw new UnauthorizedOperationException("Only the reporter, the assigned technician or an administrator can close this incident");
        }
        IncidentStatus from = incident.getStatus();
        incident.close(note, now());
        publish(new IncidentStatusChangedEvent(incident, actor, now(), from, IncidentAction.CLOSE, note));
        return incident;
    }

    public Incident closeAsDuplicate(Long id, String originalNumber, User actor) {
        Incident incident = load(id);
        requireStaff(actor);
        Incident original = Incident.idFromNumber(originalNumber).flatMap(incidents::findById)
                .orElseThrow(() -> new ResourceNotFoundException("Incident", originalNumber));
        IncidentStatus from = incident.getStatus();
        LocalDateTime now = now();
        incident.closeAsDuplicate(original, now);
        publish(new IncidentStatusChangedEvent(incident, actor, now, from, IncidentAction.CLOSE_AS_DUPLICATE, null));
        // The reporter of the duplicate is now affected by the original incident.
        if (original.addWatcher(incident.getReporter())) {
            publish(new WatcherAddedEvent(original, incident.getReporter(), now));
            prioritise(original, actor, now);
        }
        return incident;
    }

    // ================================================================== assignment & triage

    public Incident assign(Long id, Long technicianId, User actor, boolean overrideWorkload) {
        requireAdminOrTeamLead(actor);
        Incident incident = load(id);
        Technician technician = users.findById(technicianId)
                .filter(Technician.class::isInstance).map(Technician.class::cast)
                .orElseThrow(() -> new ResourceNotFoundException("Technician", technicianId));
        if (!technician.isActive() || !technician.isAvailable()) {
            throw new TechnicianUnavailableException(technician.getName() + " is not available");
        }
        int load = assignmentService.workloadOf(technician);
        if (!technician.canTakeMore(load) && !overrideWorkload) {
            throw new WorkloadExceededException(technician.getName(), load, technician.getMaxActiveIncidents());
        }
        IncidentStatus from = incident.getStatus();
        incident.assignTo(technician, now());
        publish(new IncidentAssignedEvent(incident, actor, now(), from, null, overrideWorkload ? "workload limit overridden" : null));
        return incident;
    }

    public Incident autoAssign(Long id, User actor) {
        requireAdminOrTeamLead(actor);
        Incident incident = load(id);
        RankedTechnician choice = assignmentService.selectFor(incident)
                .orElseThrow(() -> new TechnicianUnavailableException("No technician is currently available for " + incident.getNumber()));
        IncidentStatus from = incident.getStatus();
        incident.assignTo(choice.candidate().technician(), now());
        publish(new IncidentAssignedEvent(incident, actor, now(), from, assignmentService.activeStrategy().displayName(), choice.reason()));
        return incident;
    }

    /** Staff correct category, impact, urgency or affected users; priority and SLA are recalculated. */
    public Incident triage(Long id, Long categoryId, Impact impact, Urgency urgency, int affectedUsers, User actor) {
        requireStaff(actor);
        Incident incident = load(id);
        LocalDateTime now = now();
        if (categoryId != null && (incident.getCategory() == null || !categoryId.equals(incident.getCategory().getId()))) {
            Category category = findCategory(categoryId);
            IncidentStatus from = incident.getStatus();
            if (incident.getStatus() == IncidentStatus.NEW) {
                incident.classify(category, ClassificationSource.MANUAL, 1.0, now);
            } else {
                incident.changeCategory(category, now);
            }
            publish(new IncidentClassifiedEvent(incident, actor, now, from));
        }
        incident.updateAssessment(impact, urgency, affectedUsers, now);
        prioritise(incident, actor, now);
        return incident;
    }

    /** "This affects me too": folds a would-be duplicate report into the existing incident. */
    public Incident join(Long id, User actor) {
        Incident incident = load(id);
        if (!incident.addWatcher(actor)) {
            throw new ValidationException("You are already following " + incident.getNumber());
        }
        LocalDateTime now = now();
        publish(new WatcherAddedEvent(incident, actor, now));
        prioritise(incident, null, now);
        return incident;
    }

    public Comment addComment(Long id, User actor, String text, CommentType type) {
        Incident incident = load(id);
        requireView(actor, incident);
        if (type == CommentType.INVESTIGATION_NOTE && actor.getRole() == Role.EMPLOYEE) {
            throw new UnauthorizedOperationException("Only support staff can add investigation notes");
        }
        if (type == CommentType.INFO_REQUEST || type == CommentType.INFO_RESPONSE) {
            throw new ValidationException("Use the request/provide information actions for this message type");
        }
        LocalDateTime now = now();
        Comment comment = incident.addComment(actor, text, type, now);
        publish(new CommentAddedEvent(incident, actor, now, comment));
        return comment;
    }

    // ================================================================== reading

    @Transactional(readOnly = true)
    public Incident getForViewer(Long id, User viewer) {
        Incident incident = load(id);
        requireView(viewer, incident);
        return incident;
    }

    @Transactional(readOnly = true)
    public List<Incident> reportedOrWatchedBy(User user) {
        return incidents.findReportedOrWatchedBy(user);
    }

    @Transactional(readOnly = true)
    public List<Incident> queueOf(Technician technician) {
        return incidents.findByAssigneeAndStatusInOrderByCreatedAtDesc(technician, IncidentStatus.ACTIVE_WORK);
    }

    // ================================================================== helpers

    private Incident load(Long id) {
        return incidents.findById(id).orElseThrow(() -> new ResourceNotFoundException("Incident", id));
    }

    private Category findCategory(Long id) {
        return categories.findById(id).filter(Category::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));
    }

    private void publish(IncidentEvent event) {
        events.publishEvent(event);
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    private static void requireView(User actor, Incident incident) {
        if (!actor.canView(incident)) {
            throw new UnauthorizedOperationException("You do not have access to " + incident.getNumber());
        }
    }

    private static void requireWorker(User actor, Incident incident) {
        if (!actor.canWorkOn(incident)) {
            throw new UnauthorizedOperationException("Only the assigned technician, a team lead or an administrator can do this");
        }
    }

    private static void requireReporterOrAdmin(User actor, Incident incident) {
        if (!actor.isReporterOf(incident) && actor.getRole() != Role.ADMIN) {
            throw new UnauthorizedOperationException("Only the person who reported " + incident.getNumber() + " can do this");
        }
    }

    private static void requireStaff(User actor) {
        if (actor.getRole() == Role.EMPLOYEE) {
            throw new UnauthorizedOperationException("Only support staff can do this");
        }
    }

    private static void requireAdminOrTeamLead(User actor) {
        boolean lead = actor instanceof Technician t && t.isTeamLead();
        if (actor.getRole() != Role.ADMIN && !lead) {
            throw new UnauthorizedOperationException("Only administrators and team leads can assign incidents");
        }
    }
}
