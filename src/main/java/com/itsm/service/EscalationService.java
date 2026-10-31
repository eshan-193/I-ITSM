package com.itsm.service;

import com.itsm.domain.event.IncidentEscalatedEvent;
import com.itsm.domain.incident.Incident;
import com.itsm.domain.incident.IncidentStatus;
import com.itsm.domain.priority.Priority;
import com.itsm.domain.user.Technician;
import com.itsm.domain.user.User;
import com.itsm.repository.UserRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Automatic escalation chain for P1/P2 incidents that nobody has acknowledged:
 * <pre>
 *   Technician ──(response deadline passed)──▶ Team Lead ──(another response window)──▶ IT Manager (admins)
 * </pre>
 * Unassigned P1/P2 incidents skip straight to the team leads. Each step is published
 * as an event, so it is audited and notified like everything else.
 */
@Service
public class EscalationService {

    static final int MAX_LEVEL = 2;

    private final UserRepository users;
    private final ApplicationEventPublisher events;

    public EscalationService(UserRepository users, ApplicationEventPublisher events) {
        this.users = users;
        this.events = events;
    }

    /** Called by the SLA monitor for every open incident. */
    public void escalateIfNeeded(Incident incident, LocalDateTime now) {
        if (!needsEscalation(incident, now)) {
            return;
        }
        incident.escalate(now);
        Set<User> targets = new LinkedHashSet<>();
        String reason;
        if (incident.getEscalationLevel() == 1) {
            List<Technician> leads = users.findTeamLeads();
            targets.addAll(leads);
            if (incident.getAssignee() != null) {
                targets.add(incident.getAssignee());
            }
            reason = incident.getAssignee() == null
                    ? "no technician assigned to a " + incident.getPriority() + " incident"
                    : incident.getAssignee().getName() + " has not acknowledged within the response target";
            if (leads.isEmpty()) {
                targets.addAll(users.findActiveAdministrators());
            }
        } else {
            targets.addAll(users.findActiveAdministrators());
            targets.addAll(users.findTeamLeads());
            reason = "still not acknowledged after team lead escalation";
        }
        events.publishEvent(new IncidentEscalatedEvent(incident, now, targets, reason));
    }

    /**
     * Pure rule, unit-tested separately: only P1/P2, only while not yet acknowledged, and
     * each further level requires another full response window to pass.
     */
    static boolean needsEscalation(Incident incident, LocalDateTime now) {
        if (incident.getPriority() != Priority.P1 && incident.getPriority() != Priority.P2) {
            return false;
        }
        boolean waitingForResponse = incident.getStatus() == IncidentStatus.ASSIGNED
                || incident.getStatus() == IncidentStatus.CLASSIFIED
                || incident.getStatus() == IncidentStatus.NEW;
        if (!waitingForResponse || incident.getEscalationLevel() >= MAX_LEVEL || !incident.getSla().isStarted()) {
            return false;
        }
        Duration window = Duration.between(incident.getSla().getStartedAt(), incident.getSla().getResponseDeadline());
        LocalDateTime dueAt = incident.getSla().getResponseDeadline().plus(window.multipliedBy(incident.getEscalationLevel()));
        return now.isAfter(dueAt);
    }
}
