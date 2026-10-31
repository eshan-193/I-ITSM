package com.itsm.domain.event;

import com.itsm.domain.incident.Incident;
import com.itsm.domain.incident.IncidentStatus;
import com.itsm.domain.user.User;

import java.time.LocalDateTime;

public class IncidentClassifiedEvent extends IncidentEvent {

    public IncidentClassifiedEvent(Incident incident, User actor, LocalDateTime at, IncidentStatus from) {
        super(incident, actor, at, from, incident.getStatus());
    }

    @Override
    public String auditAction() {
        return "CLASSIFIED";
    }

    @Override
    public String describe() {
        Incident i = getIncident();
        String confidence = i.getClassificationConfidence() < 1
                ? " (" + Math.round(i.getClassificationConfidence() * 100) + "% confidence)" : "";
        return "Classified as " + i.getCategory().getName() + " by " + i.getClassificationSource().getLabel() + confidence;
    }
}
