package com.itsm.domain.incident;

import com.itsm.domain.catalog.Category;
import com.itsm.domain.catalog.ItService;
import com.itsm.domain.incident.state.IncidentState;
import com.itsm.domain.priority.Priority;
import com.itsm.domain.priority.PriorityResult;
import com.itsm.domain.sla.SlaPolicy;
import com.itsm.domain.sla.SlaTracker;
import com.itsm.domain.user.Technician;
import com.itsm.domain.user.User;
import com.itsm.exception.ValidationException;
import jakarta.persistence.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

/**
 * The central aggregate: one reported IT problem and everything that happens to it.
 * <p>
 * <b>Encapsulation:</b> there is no public setter for {@code status}. The only way to
 * change it is through business methods such as {@link #acknowledge}, which ask the
 * current {@link IncidentState} whether the transition is legal first. No controller,
 * bug or teammate can put an incident into an illegal state.
 * <p>
 * <b>Relationships:</b> the incident <em>refers to</em> its reporter, assignee and
 * service (association: they live on independently) but <em>owns</em> its SLA tracker,
 * comments and audit trail (composition: they are deleted with it).
 * <p>
 * Every time-dependent method takes {@code now} as a parameter instead of reading the
 * system clock, which keeps the class deterministic and easy to unit test.
 */
@Entity
@Table(name = "incidents")
public class Incident {

    private static final int NUMBER_OFFSET = 1000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private long version;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, length = 4000)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(name = "classification_source", length = 20)
    private ClassificationSource classificationSource;

    @Column(name = "classification_confidence")
    private double classificationConfidence;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_id")
    private ItService service;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Impact impact;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Urgency urgency;

    @Column(name = "affected_users", nullable = false)
    private int affectedUsers;

    @Enumerated(EnumType.STRING)
    @Column(length = 5)
    private Priority priority;

    @Column(name = "priority_score")
    private int priorityScore;

    @Column(name = "priority_rationale", length = 500)
    private String priorityRationale;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IncidentStatus status;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reporter_id")
    private User reporter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    private Technician assignee;

    @Column(name = "escalation_level", nullable = false)
    private int escalationLevel;

    @Column(name = "escalated_at")
    private LocalDateTime escalatedAt;

    @Embedded
    private SlaTracker sla = new SlaTracker();

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;

    @Column(name = "acknowledged_at")
    private LocalDateTime acknowledgedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(length = 4000)
    private String resolution;

    @Column(name = "root_cause", length = 1000)
    private String rootCause;

    @Column(name = "closure_note", length = 1000)
    private String closureNote;

    @Column(name = "feedback_rating")
    private Integer feedbackRating;

    @Column(name = "feedback_comment", length = 1000)
    private String feedbackComment;

    @Column(name = "reopen_count", nullable = false)
    private int reopenCount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "duplicate_of_id")
    private Incident duplicateOf;

    /** Other employees affected by the same problem who clicked "me too". */
    @ManyToMany
    @JoinTable(name = "incident_watchers",
            joinColumns = @JoinColumn(name = "incident_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id"))
    private Set<User> watchers = new HashSet<>();

    @OneToMany(mappedBy = "incident", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    private List<Comment> comments = new ArrayList<>();

    @OneToMany(mappedBy = "incident", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("timestamp ASC, id ASC")
    private List<AuditEntry> auditTrail = new ArrayList<>();

    protected Incident() {
        // for JPA
    }

    public Incident(String title, String description, User reporter, ItService service,
                    Impact impact, Urgency urgency, int affectedUsers, LocalDateTime now) {
        if (title == null || title.isBlank()) {
            throw new ValidationException("Title is required");
        }
        if (description == null || description.isBlank()) {
            throw new ValidationException("Description is required");
        }
        if (affectedUsers < 1) {
            throw new ValidationException("At least one user must be affected");
        }
        this.title = title.trim();
        this.description = description.trim();
        this.reporter = Objects.requireNonNull(reporter, "reporter");
        this.service = Objects.requireNonNull(service, "service");
        this.impact = Objects.requireNonNull(impact, "impact");
        this.urgency = Objects.requireNonNull(urgency, "urgency");
        this.affectedUsers = affectedUsers;
        this.status = IncidentStatus.NEW;
        this.createdAt = now;
        this.updatedAt = now;
    }

    // ------------------------------------------------------------------ lifecycle

    private IncidentState state() {
        return status.state();
    }

    private void moveTo(IncidentState next, LocalDateTime now) {
        this.status = next.status();
        this.updatedAt = now;
    }

    public void classify(Category category, ClassificationSource source, double confidence, LocalDateTime now) {
        Objects.requireNonNull(category, "category");
        IncidentState next = state().classify();
        this.category = category;
        this.classificationSource = source;
        this.classificationConfidence = confidence;
        moveTo(next, now);
    }

    /** Correct the category later in the lifecycle without changing status. */
    public void changeCategory(Category category, LocalDateTime now) {
        requireUnresolved("change the category of");
        this.category = Objects.requireNonNull(category);
        this.classificationSource = ClassificationSource.MANUAL;
        this.classificationConfidence = 1.0;
        this.updatedAt = now;
    }

    /** Support staff correct the reporter's impact/urgency assessment; priority is then recalculated. */
    public void updateAssessment(Impact impact, Urgency urgency, int affectedUsers, LocalDateTime now) {
        requireUnresolved("reassess");
        if (affectedUsers < 1) {
            throw new ValidationException("At least one user must be affected");
        }
        this.impact = Objects.requireNonNull(impact);
        this.urgency = Objects.requireNonNull(urgency);
        this.affectedUsers = affectedUsers;
        this.updatedAt = now;
    }

    /** Sets (or re-sets) the priority and restarts SLA deadlines from the creation time. */
    public void applyPriority(PriorityResult result, SlaPolicy policy, LocalDateTime now) {
        requireUnresolved("re-prioritise");
        this.priority = result.priority();
        this.priorityScore = result.score();
        this.priorityRationale = result.rationale();
        this.sla.start(createdAt, policy);
        this.updatedAt = now;
    }

    public void assignTo(Technician technician, LocalDateTime now) {
        Objects.requireNonNull(technician, "technician");
        IncidentState next = state().assign();
        this.assignee = technician;
        this.assignedAt = now;
        moveTo(next, now);
    }

    public void acknowledge(LocalDateTime now) {
        IncidentState next = state().acknowledge();
        this.acknowledgedAt = now;
        this.sla.recordResponse(now);
        moveTo(next, now);
    }

    public void startWork(LocalDateTime now) {
        moveTo(state().startWork(), now);
    }

    public void requestInfo(LocalDateTime now) {
        IncidentState next = state().requestInfo();
        this.sla.pause(now);
        moveTo(next, now);
    }

    public void provideInfo(LocalDateTime now) {
        IncidentState next = state().provideInfo();
        this.sla.resume(now);
        moveTo(next, now);
    }

    public void resolve(String resolution, String rootCause, LocalDateTime now) {
        if (resolution == null || resolution.isBlank()) {
            throw new ValidationException("Resolution details are required");
        }
        IncidentState next = state().resolve();
        this.resolution = resolution.trim();
        this.rootCause = rootCause == null || rootCause.isBlank() ? null : rootCause.trim();
        this.resolvedAt = now;
        this.sla.recordResolution(now);
        moveTo(next, now);
    }

    /** The reporter confirms the fix and leaves feedback (rating 1–5). */
    public void verify(int rating, String feedback, LocalDateTime now) {
        if (rating < 1 || rating > 5) {
            throw new ValidationException("Rating must be between 1 and 5");
        }
        IncidentState next = state().verify();
        this.feedbackRating = rating;
        this.feedbackComment = feedback == null || feedback.isBlank() ? null : feedback.trim();
        moveTo(next, now);
    }

    /** The reporter says the fix did not work; investigation resumes. */
    public void reopen(LocalDateTime now) {
        IncidentState next = state().reopen();
        this.resolvedAt = null;
        this.reopenCount++;
        this.sla.reopen();
        moveTo(next, now);
    }

    public void close(String closureNote, LocalDateTime now) {
        IncidentState next = state().close();
        this.closureNote = closureNote == null || closureNote.isBlank() ? "Closed" : closureNote.trim();
        this.closedAt = now;
        moveTo(next, now);
    }

    public void closeAsDuplicate(Incident original, LocalDateTime now) {
        if (original == null || original.equals(this)) {
            throw new ValidationException("An incident cannot be a duplicate of itself");
        }
        if (!original.getStatus().isUnresolved()) {
            throw new ValidationException(original.getNumber() + " is no longer open; link to an open incident");
        }
        IncidentState next = state().closeAsDuplicate();
        this.duplicateOf = original;
        this.closureNote = "Duplicate of " + original.getNumber();
        this.closedAt = now;
        moveTo(next, now);
    }

    /** Moves the incident one level up the escalation chain. */
    public void escalate(LocalDateTime now) {
        requireUnresolved("escalate");
        this.escalationLevel++;
        this.escalatedAt = now;
        this.updatedAt = now;
    }

    // ------------------------------------------------------------------ collaboration

    public Comment addComment(User author, String text, CommentType type, LocalDateTime now) {
        if (text == null || text.isBlank()) {
            throw new ValidationException("Comment text is required");
        }
        if (status == IncidentStatus.CLOSED) {
            throw new ValidationException("Closed incidents cannot be commented on");
        }
        Comment comment = new Comment(this, author, text.trim(), type, now);
        comments.add(comment);
        this.updatedAt = now;
        return comment;
    }

    public void addAuditEntry(AuditEntry entry) {
        auditTrail.add(entry);
    }

    /**
     * Another employee reports being affected by the same problem.
     *
     * @return true if they were newly added
     */
    public boolean addWatcher(User user) {
        requireUnresolved("join");
        if (user.equals(reporter) || !watchers.add(user)) {
            return false;
        }
        affectedUsers++;
        return true;
    }

    public boolean isWatchedBy(User user) {
        return watchers.contains(user);
    }

    // ------------------------------------------------------------------ queries

    public String getNumber() {
        return id == null ? "INC-new" : "INC" + (NUMBER_OFFSET + id);
    }

    public static Optional<Long> idFromNumber(String number) {
        String digits = number.trim().toUpperCase().replace("INC", "");
        try {
            long value = Long.parseLong(digits);
            return value > NUMBER_OFFSET ? Optional.of(value - NUMBER_OFFSET) : Optional.of(value);
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    public Set<IncidentAction> allowedActions() {
        return state().allowedActions();
    }

    public boolean allows(IncidentAction action) {
        return state().allows(action);
    }

    public boolean isUnresolved() {
        return status.isUnresolved();
    }

    public boolean isOpen() {
        return status != IncidentStatus.CLOSED;
    }

    public Optional<Duration> timeToResolve() {
        return resolvedAt == null ? Optional.empty() : Optional.of(Duration.between(createdAt, resolvedAt));
    }

    public String getEscalationLabel() {
        return switch (escalationLevel) {
            case 0 -> "Not escalated";
            case 1 -> "Escalated to Team Lead";
            default -> "Escalated to IT Manager";
        };
    }

    private void requireUnresolved(String action) {
        if (!status.isUnresolved()) {
            throw new ValidationException("Cannot " + action + " an incident that is " + status.getLabel());
        }
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public Category getCategory() { return category; }
    public ClassificationSource getClassificationSource() { return classificationSource; }
    public double getClassificationConfidence() { return classificationConfidence; }
    public ItService getService() { return service; }
    public Impact getImpact() { return impact; }
    public Urgency getUrgency() { return urgency; }
    public int getAffectedUsers() { return affectedUsers; }
    public Priority getPriority() { return priority; }
    public int getPriorityScore() { return priorityScore; }
    public String getPriorityRationale() { return priorityRationale; }
    public IncidentStatus getStatus() { return status; }
    public User getReporter() { return reporter; }
    public Technician getAssignee() { return assignee; }
    public int getEscalationLevel() { return escalationLevel; }
    public LocalDateTime getEscalatedAt() { return escalatedAt; }
    public SlaTracker getSla() { return sla; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public LocalDateTime getAssignedAt() { return assignedAt; }
    public LocalDateTime getAcknowledgedAt() { return acknowledgedAt; }
    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public LocalDateTime getClosedAt() { return closedAt; }
    public String getResolution() { return resolution; }
    public String getRootCause() { return rootCause; }
    public String getClosureNote() { return closureNote; }
    public Integer getFeedbackRating() { return feedbackRating; }
    public String getFeedbackComment() { return feedbackComment; }
    public int getReopenCount() { return reopenCount; }
    public Incident getDuplicateOf() { return duplicateOf; }

    public Set<User> getWatchers() {
        return Collections.unmodifiableSet(watchers);
    }

    public List<Comment> getComments() {
        return Collections.unmodifiableList(comments);
    }

    public List<AuditEntry> getAuditTrail() {
        return Collections.unmodifiableList(auditTrail);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Incident other)) return false;
        return getId() != null && getId().equals(other.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }

    @Override
    public String toString() {
        return getNumber() + " " + title;
    }
}
