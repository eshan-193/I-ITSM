package com.itsm.service;

import com.itsm.domain.catalog.ItService;
import com.itsm.domain.incident.Incident;
import com.itsm.domain.priority.Priority;
import com.itsm.domain.sla.SlaPolicy;
import com.itsm.domain.sla.SlaStatus;
import com.itsm.exception.SlaConfigurationMissingException;
import com.itsm.repository.SlaPolicyRepository;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;

/** Finds the applicable SLA policy and reports SLA status. */
@Service
public class SlaService {

    private final SlaPolicyRepository policies;
    private final SettingsService settings;
    private final Clock clock;

    public SlaService(SlaPolicyRepository policies, SettingsService settings, Clock clock) {
        this.policies = policies;
        this.settings = settings;
        this.clock = clock;
    }

    /** A service-specific policy wins over the organisation default. */
    public SlaPolicy policyFor(Priority priority, ItService service) {
        return policies.findByPriorityAndService(priority, service)
                .or(() -> policies.findByPriorityAndServiceIsNull(priority))
                .orElseThrow(() -> new SlaConfigurationMissingException(priority));
    }

    public SlaStatus statusOf(Incident incident) {
        return incident.getSla().status(LocalDateTime.now(clock), warningPercent());
    }

    public int warningPercent() {
        return settings.getInt(SettingKey.SLA_WARNING_PERCENT);
    }
}
