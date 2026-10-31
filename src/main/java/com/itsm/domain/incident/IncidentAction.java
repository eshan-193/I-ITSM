package com.itsm.domain.incident;

/** Every lifecycle action a user (or the system) can attempt on an incident. */
public enum IncidentAction {
    CLASSIFY("classify"),
    ASSIGN("assign"),
    ACKNOWLEDGE("acknowledge"),
    START_WORK("start work on"),
    REQUEST_INFO("request information on"),
    PROVIDE_INFO("provide information on"),
    RESOLVE("resolve"),
    VERIFY("verify"),
    REOPEN("reopen"),
    CLOSE("close"),
    CLOSE_AS_DUPLICATE("close as duplicate");

    private final String verb;

    IncidentAction(String verb) {
        this.verb = verb;
    }

    public String getVerb() {
        return verb;
    }
}
