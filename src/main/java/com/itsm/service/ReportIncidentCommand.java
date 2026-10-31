package com.itsm.service;

import com.itsm.domain.incident.Impact;
import com.itsm.domain.incident.Urgency;

/**
 * Input for reporting an incident. {@code categoryId} may be null, in which case the
 * system suggests one. {@code confirmedNotDuplicate} lets the reporter override the
 * "you already reported this" guard.
 */
public record ReportIncidentCommand(String title, String description, Long serviceId, Long categoryId,
                                    Impact impact, Urgency urgency, int affectedUsers,
                                    boolean confirmedNotDuplicate) {
}
