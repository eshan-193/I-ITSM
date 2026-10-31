package com.itsm.service;

import com.itsm.domain.incident.AuditEntry;
import com.itsm.domain.incident.Incident;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.itsm.repository.AuditEntryRepository;

import java.time.LocalDate;
import java.util.Locale;

/** Searchable, paginated view of the system-wide audit log. */
@Service
@Transactional(readOnly = true)
public class AuditService {

    private static final int PAGE_SIZE = 50;

    private final AuditEntryRepository auditEntries;

    public AuditService(AuditEntryRepository auditEntries) {
        this.auditEntries = auditEntries;
    }

    public Page<AuditEntry> search(String incidentNumber, String action, String actor, LocalDate from, int page) {
        Specification<AuditEntry> spec = (root, q, cb) -> cb.conjunction();
        if (incidentNumber != null && !incidentNumber.isBlank()) {
            Long id = Incident.idFromNumber(incidentNumber).orElse(-1L);
            spec = spec.and((root, q, cb) -> cb.equal(root.get("incident").get("id"), id));
        }
        if (action != null && !action.isBlank()) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("action"), action));
        }
        if (actor != null && !actor.isBlank()) {
            String like = "%" + actor.trim().toLowerCase(Locale.ROOT) + "%";
            spec = spec.and((root, q, cb) -> cb.like(cb.lower(root.get("actorName")), like));
        }
        if (from != null) {
            spec = spec.and((root, q, cb) -> cb.greaterThanOrEqualTo(root.get("timestamp"), from.atStartOfDay()));
        }
        return auditEntries.findAll(spec, PageRequest.of(Math.max(0, page), PAGE_SIZE,
                Sort.by(Sort.Order.desc("timestamp"), Sort.Order.desc("id"))));
    }
}
