package com.itsm.intelligence;

import com.itsm.domain.incident.Incident;

import java.util.List;

/** Something the correlation engine noticed about an incident and its neighbours. */
public record CorrelationFinding(Type type, String message, List<Incident> related) {

    public enum Type {
        /** An open incident on a service this one depends on: probable root cause. */
        UPSTREAM_CAUSE,
        /** Open incidents on services that depend on this one: likely knock-on effects. */
        DOWNSTREAM_IMPACT,
        /** Several incidents on the same service in a short window: probable outage. */
        OUTAGE_CLUSTER
    }

    public boolean isMajor() {
        return type == Type.OUTAGE_CLUSTER;
    }
}
