package com.itsm.domain;

import com.itsm.domain.priority.Priority;
import com.itsm.domain.sla.SlaBreachType;
import com.itsm.domain.sla.SlaPolicy;
import com.itsm.domain.sla.SlaStatus;
import com.itsm.domain.sla.SlaTracker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.itsm.TestFixtures.T0;
import static org.assertj.core.api.Assertions.*;

class SlaTrackerTest {

    private SlaTracker sla;

    @BeforeEach
    void setUp() {
        sla = new SlaTracker();
        sla.start(T0, new SlaPolicy(Priority.P1, null, 5, 30));
    }

    @Test
    @DisplayName("TC-19 Deadlines are computed from the policy")
    void deadlines() {
        assertThat(sla.getResponseDeadline()).isEqualTo(T0.plusMinutes(5));
        assertThat(sla.getResolutionDeadline()).isEqualTo(T0.plusMinutes(30));
    }

    @Test
    @DisplayName("TC-20 Breaches are detected exactly once, only after the deadline")
    void breachDetection() {
        assertThat(sla.detectNewBreaches(T0.plusMinutes(5))).isEmpty();             // at deadline: not yet
        assertThat(sla.detectNewBreaches(T0.plusMinutes(6))).containsExactly(SlaBreachType.RESPONSE);
        assertThat(sla.detectNewBreaches(T0.plusMinutes(7))).isEmpty();             // not reported twice
        assertThat(sla.detectNewBreaches(T0.plusMinutes(31))).containsExactly(SlaBreachType.RESOLUTION);
        assertThat(sla.status(T0.plusMinutes(31), 75)).isEqualTo(SlaStatus.BREACHED);
    }

    @Test
    @DisplayName("TC-21 Waiting for the reporter pauses the clock and pushes the deadline back")
    void pauseExtendsDeadline() {
        sla.recordResponse(T0.plusMinutes(2));
        sla.pause(T0.plusMinutes(10));
        assertThat(sla.status(T0.plusMinutes(50), 75)).isEqualTo(SlaStatus.PAUSED);
        assertThat(sla.detectNewBreaches(T0.plusMinutes(50))).isEmpty();
        sla.resume(T0.plusMinutes(50));
        assertThat(sla.getResolutionDeadline()).isEqualTo(T0.plusMinutes(70));
    }

    @Test
    @DisplayName("TC-22 Warning fires at the configured threshold, once")
    void warningThreshold() {
        sla.recordResponse(T0.plusMinutes(1));
        assertThat(sla.shouldWarn(T0.plusMinutes(22), 75)).isFalse();   // 73%
        assertThat(sla.shouldWarn(T0.plusMinutes(23), 75)).isTrue();    // 77%
        assertThat(sla.status(T0.plusMinutes(23), 75)).isEqualTo(SlaStatus.AT_RISK);
        sla.markWarningSent();
        assertThat(sla.shouldWarn(T0.plusMinutes(25), 75)).isFalse();
    }

    @Test
    @DisplayName("TC-23 Invalid SLA policy values are rejected")
    void invalidPolicy() {
        assertThatThrownBy(() -> new SlaPolicy(Priority.P2, null, 0, 10)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SlaPolicy(Priority.P2, null, 60, 30)).isInstanceOf(IllegalArgumentException.class);
    }
}
