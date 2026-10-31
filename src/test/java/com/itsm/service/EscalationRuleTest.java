package com.itsm.service;

import com.itsm.domain.catalog.BusinessCriticality;
import com.itsm.domain.incident.Impact;
import com.itsm.domain.incident.Incident;
import com.itsm.domain.incident.Urgency;
import com.itsm.domain.priority.Priority;
import com.itsm.domain.priority.PriorityResult;
import com.itsm.domain.sla.SlaPolicy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.itsm.TestFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;

class EscalationRuleTest {

    private Incident withPriority(Priority p) {
        Incident i = incident("Outage", employee("E"), service("S", BusinessCriticality.HIGH), Impact.HIGH, Urgency.HIGH);
        i.applyPriority(new PriorityResult(p, 1, ""), new SlaPolicy(p, null, 5, 30), T0);
        return i;
    }

    @Test
    @DisplayName("TC-30 Unacknowledged P1 escalates after the response target, then again one window later")
    void escalationChain() {
        Incident i = withPriority(Priority.P1);
        assertThat(EscalationService.needsEscalation(i, T0.plusMinutes(5))).isFalse();
        assertThat(EscalationService.needsEscalation(i, T0.plusMinutes(6))).isTrue();
        i.escalate(T0.plusMinutes(6));
        assertThat(EscalationService.needsEscalation(i, T0.plusMinutes(9))).isFalse();
        assertThat(EscalationService.needsEscalation(i, T0.plusMinutes(11))).isTrue();
        i.escalate(T0.plusMinutes(11));
        assertThat(EscalationService.needsEscalation(i, T0.plusMinutes(60))).isFalse(); // top of the chain
    }

    @Test
    @DisplayName("TC-31 P3/P4 incidents are never auto-escalated")
    void lowPriorityNotEscalated() {
        assertThat(EscalationService.needsEscalation(withPriority(Priority.P3), T0.plusDays(1))).isFalse();
    }
}
