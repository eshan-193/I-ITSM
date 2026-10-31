package com.itsm.web;

import com.itsm.domain.incident.Incident;
import com.itsm.domain.sla.SlaStatus;
import com.itsm.service.SlaService;
import org.springframework.stereotype.Component;

/** Template helper: {@code ${@slaView.status(incident)}}. */
@Component("slaView")
public class SlaView {

    private final SlaService slaService;

    public SlaView(SlaService slaService) {
        this.slaService = slaService;
    }

    public SlaStatus status(Incident incident) {
        return slaService.statusOf(incident);
    }
}
