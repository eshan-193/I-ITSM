package com.itsm.service;

import com.itsm.domain.catalog.ItService;
import com.itsm.domain.incident.*;
import com.itsm.domain.priority.Priority;
import com.itsm.domain.user.Technician;
import com.itsm.domain.user.User;
import com.itsm.exception.*;
import com.itsm.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests: real Spring context, real services, H2 database with the demo data.
 * Each test runs in a transaction that is rolled back afterwards.
 */
@SpringBootTest
@Transactional
class IncidentServiceIT {

    @Autowired IncidentService incidentService;
    @Autowired IncidentSearchService searchService;
    @Autowired SlaService slaService;
    @Autowired SlaMonitor slaMonitor;
    @Autowired SettingsService settings;
    @Autowired AssignmentService assignmentService;
    @Autowired UserService userService;
    @Autowired CatalogService catalogService;
    @Autowired UserRepository users;
    @Autowired ItServiceRepository services;
    @Autowired IncidentRepository incidents;
    @Autowired NotificationRepository notifications;
    @Autowired SlaViolationRepository violations;
    @Autowired SlaPolicyRepository slaPolicies;

    private User user(String email) {
        return users.findByEmailIgnoreCase(email).orElseThrow();
    }

    private ItService service(String name) {
        return services.findByNameIgnoreCase(name).orElseThrow();
    }

    private Incident reportPaymentOutage(User reporter) {
        return incidentService.report(new ReportIncidentCommand("Checkout failing for all customers",
                "Card payments fail at checkout with an error after entering card details",
                service("Payment Gateway").getId(), null, Impact.HIGH, Urgency.HIGH, 200, false), reporter);
    }

    @Test
    @DisplayName("TC-32 Reporting runs the full pipeline: classify, prioritise, SLA, assign, audit, notify")
    void reportPipeline() {
        Incident i = reportPaymentOutage(user("vikram@itsm.local"));

        assertThat(i.getPriority()).isEqualTo(Priority.P1);
        assertThat(i.getCategory()).isNotNull();
        assertThat(i.getStatus()).isEqualTo(IncidentStatus.ASSIGNED);
        assertThat(i.getAssignee()).isNotNull();
        assertThat(i.getSla().getResponseDeadline()).isEqualTo(i.getCreatedAt().plusMinutes(5));
        assertThat(i.getAuditTrail()).extracting(AuditEntry::getAction)
                .startsWith("CREATED", "CLASSIFIED", "PRIORITY_SET", "ASSIGNED");
        assertThat(notifications.findTop50ByRecipientOrderByCreatedAtDesc(i.getAssignee()))
                .anyMatch(n -> n.getMessage().contains(i.getNumber()));
    }

    @Test
    @DisplayName("TC-33 Reporting the same problem twice is blocked unless the reporter confirms it is different")
    void ownDuplicateRejected() {
        User reporter = user("aisha@itsm.local");
        reportPaymentOutage(reporter);
        assertThatThrownBy(() -> reportPaymentOutage(reporter)).isInstanceOf(DuplicateIncidentException.class);
        Incident confirmed = incidentService.report(new ReportIncidentCommand("Checkout failing for all customers",
                "Different problem in fact", service("Payment Gateway").getId(), null, Impact.LOW, Urgency.LOW, 1, true), reporter);
        assertThat(confirmed.getId()).isNotNull();
    }

    @Test
    @DisplayName("TC-34 Employees cannot do technician work or open other people's incidents")
    void unauthorizedOperations() {
        Incident i = reportPaymentOutage(user("vikram@itsm.local"));
        User reporter = user("vikram@itsm.local");
        User stranger = user("dev@itsm.local");
        assertThatThrownBy(() -> incidentService.acknowledge(i.getId(), reporter)).isInstanceOf(UnauthorizedOperationException.class);
        assertThatThrownBy(() -> incidentService.getForViewer(i.getId(), stranger)).isInstanceOf(UnauthorizedOperationException.class);
        assertThatThrownBy(() -> incidentService.assign(i.getId(), i.getAssignee().getId(), reporter, false))
                .isInstanceOf(UnauthorizedOperationException.class);
    }

    @Test
    @DisplayName("TC-35 Manual assignment respects workload limits unless explicitly overridden")
    void workloadExceeded() {
        Technician karan = (Technician) user("karan@itsm.local");
        int load = assignmentService.workloadOf(karan);
        userService.updateTechnician(karan.getId(), Set.of(), Math.max(1, load), 2, true, false);
        Incident i = reportPaymentOutage(user("vikram@itsm.local"));
        User admin = user("admin@itsm.local");
        assertThatThrownBy(() -> incidentService.assign(i.getId(), karan.getId(), admin, false))
                .isInstanceOf(WorkloadExceededException.class);
        assertThat(incidentService.assign(i.getId(), karan.getId(), admin, true).getAssignee()).isEqualTo(karan);
    }

    @Test
    @DisplayName("TC-36 Unavailable technician cannot be assigned")
    void unavailableTechnician() {
        Technician sneha = (Technician) user("sneha@itsm.local");
        userService.setAvailability(sneha, false);
        Incident i = reportPaymentOutage(user("vikram@itsm.local"));
        assertThatThrownBy(() -> incidentService.assign(i.getId(), sneha.getId(), user("admin@itsm.local"), true))
                .isInstanceOf(TechnicianUnavailableException.class);
    }

    @Test
    @DisplayName("TC-37 Background SLA monitor records breaches and escalates an unacknowledged P1")
    void slaMonitorBreachAndEscalation() {
        LocalDateTime twoHoursAgo = LocalDateTime.now().minusHours(2);
        Incident i = new Incident("Core DB down", "Database not responding", user("rohan@itsm.local"),
                service("Core Database"), Impact.HIGH, Urgency.HIGH, 300, twoHoursAgo);
        i.classify(catalogService.activeCategories().get(0), ClassificationSource.MANUAL, 1, twoHoursAgo);
        i.applyPriority(new com.itsm.domain.priority.PriorityResult(Priority.P1, 27, "test"),
                slaService.policyFor(Priority.P1, i.getService()), twoHoursAgo);
        i.assignTo((Technician) user("ananya@itsm.local"), twoHoursAgo);
        incidents.save(i);
        long before = violations.count();

        slaMonitor.run();

        assertThat(i.getSla().isResponseBreached()).isTrue();
        assertThat(i.getSla().isResolutionBreached()).isTrue();
        assertThat(violations.count()).isEqualTo(before + 2);
        assertThat(i.getEscalationLevel()).isEqualTo(1);
        assertThat(i.getAuditTrail()).extracting(AuditEntry::getAction).contains("SLA_BREACHED", "ESCALATED");
    }

    @Test
    @DisplayName("TC-38 Missing SLA configuration is reported clearly")
    void missingSlaConfiguration() {
        slaPolicies.deleteAll();
        assertThatThrownBy(() -> slaService.policyFor(Priority.P3, service("CRM")))
                .isInstanceOf(SlaConfigurationMissingException.class);
    }

    @Test
    @DisplayName("TC-39 Search filters by priority and scopes employees to their own incidents")
    void search() {
        IncidentSearchCriteria p1 = new IncidentSearchCriteria();
        p1.setPriority(Priority.P1);
        assertThat(searchService.search(p1, user("admin@itsm.local"))).isNotEmpty()
                .allMatch(i -> i.getPriority() == Priority.P1);

        User rohan = user("rohan@itsm.local");
        assertThat(searchService.search(new IncidentSearchCriteria(), rohan)).isNotEmpty()
                .allMatch(i -> i.getReporter().equals(rohan) || i.isWatchedBy(rohan));

        IncidentSearchCriteria bad = new IncidentSearchCriteria();
        bad.setNumber("not-a-number");
        assertThat(searchService.search(bad, user("admin@itsm.local"))).isEmpty();
    }

    @Test
    @DisplayName("TC-40 Switching the assignment strategy at runtime changes who is chosen; invalid settings rejected")
    void strategySwitchAndSettingsValidation() {
        settings.set(SettingKey.ASSIGNMENT_STRATEGY, "LOAD");
        assertThat(assignmentService.activeStrategy().key()).isEqualTo("LOAD");
        settings.set(SettingKey.ASSIGNMENT_STRATEGY, "SKILL");
        assertThat(assignmentService.activeStrategy().key()).isEqualTo("SKILL");
        assertThatThrownBy(() -> settings.set(SettingKey.SLA_WARNING_PERCENT, "150")).isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("TC-41 Registration validates password strength and unique email")
    void registrationValidation() {
        assertThatThrownBy(() -> userService.registerEmployee("New", "new@itsm.local", "short", null))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> userService.registerEmployee("Dup", "rohan@itsm.local", "Password123", null))
                .isInstanceOf(ValidationException.class).hasMessageContaining("already exists");
        assertThat(userService.registerEmployee("New", "new@itsm.local", "Password123", "QA").getId()).isNotNull();
    }

    @Test
    @DisplayName("TC-42 Circular service dependencies are rejected")
    void circularDependency() {
        ItService db = service("Core Database");
        ItService payments = service("Payment Gateway");   // Payment Gateway already depends on Core Database
        assertThatThrownBy(() -> catalogService.updateService(db.getId(), db.getName(), null, db.getCriticality(),
                null, Set.of(payments.getId()), true)).isInstanceOf(ValidationException.class).hasMessageContaining("Circular");
    }

    @Test
    @DisplayName("TC-43 'Me too' raises affected users and can raise the priority")
    void joinRaisesPriority() {
        Incident i = incidentService.report(new ReportIncidentCommand("Wifi weak in meeting room",
                "Signal drops in room 4B", service("Corporate Network").getId(), null, Impact.MEDIUM, Urgency.MEDIUM, 50, false),
                user("meera@itsm.local"));
        Priority before = i.getPriority();
        incidentService.join(i.getId(), user("dev@itsm.local"));
        assertThat(i.getAffectedUsers()).isEqualTo(51);
        assertThat(i.getPriority().isHigherThan(before)).isTrue();
        assertThatThrownBy(() -> incidentService.join(i.getId(), user("dev@itsm.local"))).isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("TC-50 A reporter's reply in the conversation answers an information request and resumes the SLA")
    void reporterReplyResumesWork() {
        User reporter = user("vikram@itsm.local");
        Incident i = reportPaymentOutage(reporter);
        User tech = i.getAssignee();
        incidentService.acknowledge(i.getId(), tech);
        incidentService.startWork(i.getId(), tech);
        incidentService.requestInfo(i.getId(), tech, "Does it fail for all card types?");
        assertThat(i.getStatus()).isEqualTo(IncidentStatus.AWAITING_INFO);
        assertThat(i.getSla().isPaused()).isTrue();

        Comment reply = incidentService.addComment(i.getId(), reporter, "All cards fail after the OTP step.", CommentType.UPDATE);

        assertThat(i.getStatus()).isEqualTo(IncidentStatus.IN_PROGRESS);
        assertThat(i.getSla().isPaused()).isFalse();
        assertThat(reply.getType()).isEqualTo(CommentType.INFO_RESPONSE);
    }
}
