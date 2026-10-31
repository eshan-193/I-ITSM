package com.itsm.domain;

import com.itsm.domain.catalog.BusinessCriticality;
import com.itsm.domain.catalog.Category;
import com.itsm.domain.catalog.ItService;
import com.itsm.domain.incident.*;
import com.itsm.domain.priority.PriorityResult;
import com.itsm.domain.priority.WeightedPriorityCalculator;
import com.itsm.domain.priority.PriorityInput;
import com.itsm.domain.priority.Priority;
import com.itsm.domain.sla.SlaPolicy;
import com.itsm.domain.user.Employee;
import com.itsm.domain.user.Technician;
import com.itsm.exception.InvalidStateTransitionException;
import com.itsm.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Map;
import java.util.function.Function;

import static com.itsm.TestFixtures.*;
import static org.assertj.core.api.Assertions.*;

/** State pattern: legal and illegal lifecycle transitions. */
class IncidentLifecycleTest {

    private Employee reporter;
    private Technician tech;
    private Category network;
    private Incident incident;

    @BeforeEach
    void setUp() {
        reporter = employee("Rohan");
        network = category("NETWORK");
        tech = technician("Arjun", 5, 5, network);
        ItService vpn = service("VPN", BusinessCriticality.MEDIUM);
        incident = incident("VPN drops", reporter, vpn, Impact.MEDIUM, Urgency.HIGH);
    }

    private void prioritise() {
        PriorityResult r = new WeightedPriorityCalculator().calculate(
                new PriorityInput(incident.getImpact(), incident.getUrgency(), BusinessCriticality.MEDIUM, 1));
        incident.applyPriority(r, new SlaPolicy(r.priority(), null, 15, 120), T0);
    }

    @Test
    @DisplayName("TC-01 Full lifecycle NEW → … → CLOSED follows the defined order")
    void fullLifecycle() {
        incident.classify(network, ClassificationSource.MANUAL, 1, T0);
        prioritise();
        incident.assignTo(tech, T0.plusMinutes(1));
        incident.acknowledge(T0.plusMinutes(5));
        incident.startWork(T0.plusMinutes(6));
        incident.resolve("Upgraded VPN client", "Client bug", T0.plusMinutes(60));
        incident.verify(5, "great", T0.plusMinutes(70));
        incident.close("done", T0.plusMinutes(80));

        assertThat(incident.getStatus()).isEqualTo(IncidentStatus.CLOSED);
        assertThat(incident.getSla().isBreached()).isFalse();
        assertThat(incident.allowedActions()).isEmpty();
        assertThat(incident.getFeedbackRating()).isEqualTo(5);
    }

    @Test
    @DisplayName("TC-02 NEW → CLOSED directly is rejected")
    void cannotCloseNewIncident() {
        assertThatThrownBy(() -> incident.close("skip", T0))
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessageContaining("Cannot close an incident that is New");
        assertThat(incident.getStatus()).isEqualTo(IncidentStatus.NEW);
    }

    @Test
    @DisplayName("TC-03 Cannot acknowledge before a technician is assigned")
    void cannotAcknowledgeUnassigned() {
        incident.classify(network, ClassificationSource.MANUAL, 1, T0);
        assertThatThrownBy(() -> incident.acknowledge(T0)).isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    @DisplayName("TC-04 Resolving requires resolution details")
    void resolveRequiresDetails() {
        incident.classify(network, ClassificationSource.MANUAL, 1, T0);
        incident.assignTo(tech, T0);
        incident.acknowledge(T0);
        incident.startWork(T0);
        assertThatThrownBy(() -> incident.resolve("  ", null, T0)).isInstanceOf(ValidationException.class);
        assertThat(incident.getStatus()).isEqualTo(IncidentStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("TC-05 Reporter rejecting a fix reopens the incident and counts the reopen")
    void reopenGoesBackToInProgress() {
        incident.classify(network, ClassificationSource.MANUAL, 1, T0);
        incident.assignTo(tech, T0);
        incident.acknowledge(T0);
        incident.startWork(T0);
        incident.resolve("Restarted", null, T0.plusMinutes(10));
        incident.reopen(T0.plusMinutes(20));
        assertThat(incident.getStatus()).isEqualTo(IncidentStatus.IN_PROGRESS);
        assertThat(incident.getReopenCount()).isEqualTo(1);
        assertThat(incident.getResolvedAt()).isNull();
    }

    @Test
    @DisplayName("TC-06 Feedback rating must be 1–5 (boundary values)")
    void ratingBoundaries() {
        incident.classify(network, ClassificationSource.MANUAL, 1, T0);
        incident.assignTo(tech, T0);
        incident.acknowledge(T0);
        incident.startWork(T0);
        incident.resolve("Fixed", null, T0);
        assertThatThrownBy(() -> incident.verify(0, null, T0)).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> incident.verify(6, null, T0)).isInstanceOf(ValidationException.class);
        incident.verify(1, null, T0);
        assertThat(incident.getStatus()).isEqualTo(IncidentStatus.VERIFIED);
    }

    @Test
    @DisplayName("TC-07 Invalid incident input is rejected at construction")
    void invalidIncidentInput() {
        ItService s = service("X", BusinessCriticality.LOW);
        assertThatThrownBy(() -> new Incident(" ", "desc", reporter, s, Impact.LOW, Urgency.LOW, 1, T0))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> new Incident("Title", "desc", reporter, s, Impact.LOW, Urgency.LOW, 0, T0))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("TC-08 Closing as duplicate links to the original and is only allowed before work starts")
    void closeAsDuplicate() {
        Incident original = incident("VPN down", reporter, incident.getService(), Impact.HIGH, Urgency.HIGH);
        incident.classify(network, ClassificationSource.MANUAL, 1, T0);
        incident.closeAsDuplicate(original, T0);
        assertThat(incident.getStatus()).isEqualTo(IncidentStatus.CLOSED);
        assertThat(incident.getDuplicateOf()).isSameAs(original);
        assertThatThrownBy(() -> original.closeAsDuplicate(original, T0)).isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("TC-09 'Me too' adds a watcher and increases affected users once")
    void watchers() {
        Employee other = employee("Meera");
        assertThat(incident.addWatcher(other)).isTrue();
        assertThat(incident.addWatcher(other)).isFalse();
        assertThat(incident.addWatcher(reporter)).isFalse();
        assertThat(incident.getAffectedUsers()).isEqualTo(2);
        assertThat(other.canView(incident)).isTrue();
    }

    private static final Map<IncidentAction, Function<com.itsm.domain.incident.state.IncidentState, Object>> CALLS = Map.ofEntries(
            Map.entry(IncidentAction.CLASSIFY, s -> s.classify()),
            Map.entry(IncidentAction.ASSIGN, s -> s.assign()),
            Map.entry(IncidentAction.ACKNOWLEDGE, s -> s.acknowledge()),
            Map.entry(IncidentAction.START_WORK, s -> s.startWork()),
            Map.entry(IncidentAction.REQUEST_INFO, s -> s.requestInfo()),
            Map.entry(IncidentAction.PROVIDE_INFO, s -> s.provideInfo()),
            Map.entry(IncidentAction.RESOLVE, s -> s.resolve()),
            Map.entry(IncidentAction.VERIFY, s -> s.verify()),
            Map.entry(IncidentAction.REOPEN, s -> s.reopen()),
            Map.entry(IncidentAction.CLOSE, s -> s.close()),
            Map.entry(IncidentAction.CLOSE_AS_DUPLICATE, s -> s.closeAsDuplicate()));

    @ParameterizedTest(name = "{0}")
    @EnumSource(IncidentStatus.class)
    @DisplayName("TC-10 Every state's advertised actions match the transitions it really allows")
    void allowedActionsMatchBehaviour(IncidentStatus status) {
        var state = status.state();
        assertThat(state.status()).isEqualTo(status);
        for (IncidentAction action : IncidentAction.values()) {
            if (state.allows(action)) {
                assertThatCode(() -> CALLS.get(action).apply(state)).doesNotThrowAnyException();
            } else {
                assertThatThrownBy(() -> CALLS.get(action).apply(state)).isInstanceOf(InvalidStateTransitionException.class);
            }
        }
    }
}
