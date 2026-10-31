package com.itsm.notification;

import com.itsm.domain.event.IncidentEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Map;

/** Observer: tells every open dashboard to refresh after an incident changes. */
@Component
public class DashboardRefreshListener {

    private final LiveUpdateHub hub;

    public DashboardRefreshListener(LiveUpdateHub hub) {
        this.hub = hub;
    }

    @EventListener
    @Order(3)
    public void on(IncidentEvent event) {
        Map<String, Object> payload = Map.of(
                "incidentId", event.getIncident().getId(),
                "action", event.auditAction());
        AfterCommit.run(() -> hub.broadcast("incident-changed", payload));
    }
}
