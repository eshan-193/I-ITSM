package com.itsm.domain.assignment;

import com.itsm.domain.user.Technician;

/**
 * A technician plus the live statistics an assignment strategy needs.
 * Built by the service layer so strategies stay pure and easy to test.
 *
 * @param activeIncidents     incidents currently in ASSIGNED/ACKNOWLEDGED/IN_PROGRESS/AWAITING_INFO
 * @param resolvedInCategory  incidents this technician has resolved in the incident's category
 * @param resolvedForService  incidents this technician has resolved on the affected service
 */
public record TechnicianCandidate(Technician technician, int activeIncidents,
                                  long resolvedInCategory, long resolvedForService) {

    public boolean canTakeMore() {
        return technician.canTakeMore(activeIncidents);
    }

    /** 0.0 = idle, 1.0 = at the workload limit. */
    public double loadRatio() {
        return (double) activeIncidents / technician.getMaxActiveIncidents();
    }
}
