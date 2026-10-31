package com.itsm.domain.event;

import com.itsm.domain.incident.Comment;
import com.itsm.domain.incident.Incident;
import com.itsm.domain.user.User;

import java.time.LocalDateTime;
import java.util.Set;

public class CommentAddedEvent extends IncidentEvent {

    private final Comment comment;

    public CommentAddedEvent(Incident incident, User actor, LocalDateTime at, Comment comment) {
        super(incident, actor, at, incident.getStatus(), incident.getStatus());
        this.comment = comment;
    }

    @Override
    public String auditAction() {
        return "COMMENT";
    }

    @Override
    public String describe() {
        return comment.getType().getLabel() + " added by " + getActor().getName();
    }

    /** Reporter's messages go to the technician; staff messages go to the reporter if visible to them. */
    @Override
    public Set<User> recipients() {
        if (getActor().isReporterOf(getIncident())) {
            return assigneeOnly();
        }
        return comment.getType().isVisibleToReporter() ? reporterAndWatchers() : Set.of();
    }
}
