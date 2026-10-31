package com.itsm.service;

import com.itsm.domain.catalog.ItService;
import com.itsm.domain.incident.Impact;
import com.itsm.domain.incident.Incident;
import com.itsm.domain.incident.Urgency;
import com.itsm.domain.priority.PriorityCalculator;
import com.itsm.domain.priority.PriorityInput;
import com.itsm.domain.priority.PriorityResult;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Picks the administrator's chosen {@link PriorityCalculator} and applies it. */
@Service
public class PriorityService {

    private final Map<String, PriorityCalculator> calculators = new LinkedHashMap<>();
    private final SettingsService settings;

    public PriorityService(List<PriorityCalculator> calculators, SettingsService settings) {
        calculators.forEach(c -> this.calculators.put(c.key(), c));
        this.settings = settings;
    }

    public PriorityCalculator active() {
        PriorityCalculator calculator = calculators.get(settings.get(SettingKey.PRIORITY_CALCULATOR));
        return calculator != null ? calculator : calculators.values().iterator().next();
    }

    public List<PriorityCalculator> available() {
        return List.copyOf(calculators.values());
    }

    public PriorityResult calculate(Incident incident) {
        return calculate(incident.getImpact(), incident.getUrgency(), incident.getService(), incident.getAffectedUsers());
    }

    public PriorityResult calculate(Impact impact, Urgency urgency, ItService service, int affectedUsers) {
        return active().calculate(new PriorityInput(impact, urgency, service.getCriticality(), affectedUsers));
    }
}
