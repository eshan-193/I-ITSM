package com.itsm.service;

import com.itsm.domain.event.SlaBreachedEvent;
import com.itsm.domain.event.SlaWarningEvent;
import com.itsm.domain.incident.Incident;
import com.itsm.domain.incident.IncidentStatus;
import com.itsm.domain.sla.SlaBreachType;
import com.itsm.domain.sla.SlaViolation;
import com.itsm.repository.IncidentRepository;
import com.itsm.repository.SlaViolationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.PriorityQueue;

/**
 * Background job: runs every 30 seconds so SLA breaches are caught even when nobody
 * is looking at the screen.
 * <p>
 * Open incidents are processed from a {@link PriorityQueue} ordered by resolution
 * deadline, so the most urgent ones are always handled first.
 */
@Service
public class SlaMonitor {

    private static final Logger log = LoggerFactory.getLogger(SlaMonitor.class);

    private final IncidentRepository incidents;
    private final SlaViolationRepository violations;
    private final SettingsService settings;
    private final EscalationService escalation;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public SlaMonitor(IncidentRepository incidents, SlaViolationRepository violations, SettingsService settings,
                      EscalationService escalation, ApplicationEventPublisher events, Clock clock) {
        this.incidents = incidents;
        this.violations = violations;
        this.settings = settings;
        this.escalation = escalation;
        this.events = events;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${itsm.sla.monitor-interval-ms:30000}", initialDelay = 10_000)
    @Transactional
    public void run() {
        LocalDateTime now = LocalDateTime.now(clock);
        int warningPercent = settings.getInt(SettingKey.SLA_WARNING_PERCENT);
        boolean escalationOn = settings.getBoolean(SettingKey.ESCALATION_ENABLED);

        PriorityQueue<Incident> queue = new PriorityQueue<>(Comparator.comparing(
                (Incident i) -> i.getSla().getResolutionDeadline(), Comparator.nullsLast(Comparator.naturalOrder())));
        queue.addAll(incidents.findByStatusIn(IncidentStatus.UNRESOLVED));

        int breaches = 0;
        while (!queue.isEmpty()) {
            Incident incident = queue.poll();
            for (SlaBreachType type : incident.getSla().detectNewBreaches(now)) {
                violations.save(new SlaViolation(incident, type, now));
                events.publishEvent(new SlaBreachedEvent(incident, now, type));
                breaches++;
            }
            if (incident.getSla().shouldWarn(now, warningPercent)) {
                incident.getSla().markWarningSent();
                events.publishEvent(new SlaWarningEvent(incident, now, incident.getSla().minutesToResolutionDeadline(now)));
            }
            if (escalationOn) {
                escalation.escalateIfNeeded(incident, now);
            }
        }
        if (breaches > 0) {
            log.info("SLA monitor recorded {} new breach(es)", breaches);
        }
    }
}
