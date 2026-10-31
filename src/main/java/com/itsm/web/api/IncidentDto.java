package com.itsm.web.api;

import com.itsm.domain.incident.Incident;

import java.time.LocalDateTime;

/** JSON view of an incident. Entities are never serialised directly, so internals stay private. */
public record IncidentDto(Long id, String number, String title, String description, String status, String priority,
                          String category, String service, String reporter, String assignee,
                          LocalDateTime createdAt, LocalDateTime responseDeadline, LocalDateTime resolutionDeadline,
                          boolean slaBreached, String resolution) {

    public static IncidentDto from(Incident i) {
        return new IncidentDto(i.getId(), i.getNumber(), i.getTitle(), i.getDescription(), i.getStatus().name(),
                i.getPriority() == null ? null : i.getPriority().name(),
                i.getCategory() == null ? null : i.getCategory().getCode(),
                i.getService().getName(), i.getReporter().getName(),
                i.getAssignee() == null ? null : i.getAssignee().getName(),
                i.getCreatedAt(), i.getSla().getResponseDeadline(), i.getSla().getResolutionDeadline(),
                i.getSla().isBreached(), i.getResolution());
    }
}
