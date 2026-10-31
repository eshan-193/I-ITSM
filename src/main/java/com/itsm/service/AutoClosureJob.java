package com.itsm.service;

import com.itsm.domain.event.IncidentStatusChangedEvent;
import com.itsm.domain.incident.Incident;
import com.itsm.domain.incident.IncidentAction;
import com.itsm.domain.incident.IncidentStatus;
import com.itsm.repository.IncidentRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Housekeeping: reporters often forget to confirm a fix. A RESOLVED incident with no
 * response for 3 days is verified automatically (rating 3 = "no feedback"), and VERIFIED
 * incidents are closed after 1 day, so the queue does not fill up with stale items.
 */
@Service
public class AutoClosureJob {

    static final int AUTO_VERIFY_DAYS = 3;
    static final int AUTO_CLOSE_DAYS = 1;

    private final IncidentRepository incidents;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public AutoClosureJob(IncidentRepository incidents, ApplicationEventPublisher events, Clock clock) {
        this.incidents = incidents;
        this.events = events;
        this.clock = clock;
    }

    @Scheduled(fixedDelay = 15 * 60 * 1000L, initialDelay = 60_000)
    @Transactional
    public void run() {
        LocalDateTime now = LocalDateTime.now(clock);
        for (Incident i : incidents.findByStatusIn(List.of(IncidentStatus.RESOLVED))) {
            if (i.getResolvedAt() != null && i.getResolvedAt().plusDays(AUTO_VERIFY_DAYS).isBefore(now)) {
                i.verify(3, "Automatically verified: no response from reporter within " + AUTO_VERIFY_DAYS + " days", now);
                events.publishEvent(new IncidentStatusChangedEvent(i, null, now, IncidentStatus.RESOLVED, IncidentAction.VERIFY, "auto"));
            }
        }
        for (Incident i : incidents.findByStatusIn(List.of(IncidentStatus.VERIFIED))) {
            if (i.getUpdatedAt().plusDays(AUTO_CLOSE_DAYS).isBefore(now)) {
                i.close("Closed automatically after verification", now);
                events.publishEvent(new IncidentStatusChangedEvent(i, null, now, IncidentStatus.VERIFIED, IncidentAction.CLOSE, null));
            }
        }
    }
}
