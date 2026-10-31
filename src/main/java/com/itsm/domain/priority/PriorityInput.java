package com.itsm.domain.priority;

import com.itsm.domain.catalog.BusinessCriticality;
import com.itsm.domain.incident.Impact;
import com.itsm.domain.incident.Urgency;

/** Everything a priority calculator may consider. */
public record PriorityInput(Impact impact, Urgency urgency, BusinessCriticality criticality, int affectedUsers) {
}
