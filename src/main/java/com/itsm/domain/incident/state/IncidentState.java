package com.itsm.domain.incident.state;

import com.itsm.domain.incident.IncidentAction;
import com.itsm.domain.incident.IncidentStatus;
import com.itsm.exception.InvalidStateTransitionException;

import java.util.Set;

/**
 * State pattern: one implementation per lifecycle status.
 * <p>
 * Each method returns the next state. By default every action is illegal, so a state
 * class only overrides the transitions it allows. Adding a new status means adding
 * one class, not editing a growing if/switch in every method.
 */
public interface IncidentState {

    IncidentStatus status();

    /** Actions legal from this state; used by the UI to show only valid buttons. */
    Set<IncidentAction> allowedActions();

    default boolean allows(IncidentAction action) {
        return allowedActions().contains(action);
    }

    default IncidentState classify() { throw invalid(IncidentAction.CLASSIFY); }
    default IncidentState assign() { throw invalid(IncidentAction.ASSIGN); }
    default IncidentState acknowledge() { throw invalid(IncidentAction.ACKNOWLEDGE); }
    default IncidentState startWork() { throw invalid(IncidentAction.START_WORK); }
    default IncidentState requestInfo() { throw invalid(IncidentAction.REQUEST_INFO); }
    default IncidentState provideInfo() { throw invalid(IncidentAction.PROVIDE_INFO); }
    default IncidentState resolve() { throw invalid(IncidentAction.RESOLVE); }
    default IncidentState verify() { throw invalid(IncidentAction.VERIFY); }
    default IncidentState reopen() { throw invalid(IncidentAction.REOPEN); }
    default IncidentState close() { throw invalid(IncidentAction.CLOSE); }
    default IncidentState closeAsDuplicate() { throw invalid(IncidentAction.CLOSE_AS_DUPLICATE); }

    private InvalidStateTransitionException invalid(IncidentAction action) {
        return new InvalidStateTransitionException(status(), action.getVerb());
    }
}
