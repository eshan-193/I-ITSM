package com.itsm.service;

import com.itsm.domain.incident.Incident;
import com.itsm.domain.sla.SlaStatus;
import com.itsm.domain.user.Role;
import com.itsm.domain.user.User;
import com.itsm.repository.IncidentRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.itsm.service.IncidentSpecifications.*;

/** Multi-criteria incident search, automatically scoped to what the viewer may see. */
@Service
@Transactional(readOnly = true)
public class IncidentSearchService {

    public static final int MAX_RESULTS = 300;

    private final IncidentRepository incidents;
    private final SlaService slaService;
    private final Clock clock;

    public IncidentSearchService(IncidentRepository incidents, SlaService slaService, Clock clock) {
        this.incidents = incidents;
        this.slaService = slaService;
        this.clock = clock;
    }

    public List<Incident> search(IncidentSearchCriteria c, User viewer) {
        Long id = null;
        if (c.getNumber() != null && !c.getNumber().isBlank()) {
            Optional<Long> parsed = Incident.idFromNumber(c.getNumber());
            if (parsed.isEmpty()) {
                return List.of();
            }
            id = parsed.get();
        }
        Specification<Incident> spec = combine(
                idEquals(id), keyword(c.getKeyword()), status(c.getStatus()), openOnly(c.isOpenOnly()),
                priority(c.getPriority()), category(c.getCategoryId()), service(c.getServiceId()),
                technician(c.getTechnicianId()), createdFrom(c.getFrom()), createdTo(c.getTo()),
                c.getSlaStatus() == SlaStatus.BREACHED ? slaBreached() : null,
                viewer.getRole() == Role.EMPLOYEE ? visibleToEmployee(viewer) : null);

        List<Incident> found = incidents.findAll(spec,
                PageRequest.of(0, MAX_RESULTS, Sort.by(Sort.Direction.DESC, "createdAt"))).getContent();

        // AT_RISK / ON_TRACK / PAUSED / MET depend on the current time, so they are filtered in memory.
        SlaStatus wanted = c.getSlaStatus();
        if (wanted != null && wanted != SlaStatus.BREACHED) {
            LocalDateTime now = LocalDateTime.now(clock);
            int warn = slaService.warningPercent();
            found = found.stream().filter(i -> i.getSla().status(now, warn) == wanted).toList();
        }
        return found;
    }

    /** AND-combines the non-null pieces; an empty filter matches everything. */
    @SafeVarargs
    private static Specification<Incident> combine(Specification<Incident>... parts) {
        Specification<Incident> result = (root, q, cb) -> cb.conjunction();
        for (Specification<Incident> part : parts) {
            if (part != null) {
                result = result.and(part);
            }
        }
        return result;
    }
}
