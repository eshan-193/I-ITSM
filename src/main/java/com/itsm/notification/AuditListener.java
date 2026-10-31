package com.itsm.notification;

import com.itsm.domain.event.IncidentEvent;
import com.itsm.domain.incident.AuditEntry;
import com.itsm.repository.AuditEntryRepository;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Observer: writes one audit entry for every incident event. Runs synchronously inside
 * the same transaction as the change, so the change and its audit record are saved
 * together or not at all.
 */
@Component
public class AuditListener {

    private final AuditEntryRepository auditEntries;

    public AuditListener(AuditEntryRepository auditEntries) {
        this.auditEntries = auditEntries;
    }

    @EventListener
    @Order(1)
    public void on(IncidentEvent event) {
        AuditEntry entry = new AuditEntry(event.getIncident(), event.getActor(), event.getOccurredAt(),
                event.auditAction(), event.getFromStatus(), event.getToStatus(), event.describe());
        event.getIncident().addAuditEntry(auditEntries.save(entry));
    }
}
