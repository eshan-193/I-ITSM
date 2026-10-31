package com.itsm.notification;

import com.itsm.domain.incident.Incident;
import com.itsm.domain.notification.Severity;
import com.itsm.domain.user.User;

/** What to tell one person, independent of how it is delivered. */
public record NotificationMessage(User recipient, Incident incident, String subject, String body, Severity severity) {
}
