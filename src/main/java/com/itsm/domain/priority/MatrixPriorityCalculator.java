package com.itsm.domain.priority;

import com.itsm.domain.catalog.BusinessCriticality;

/**
 * Alternative algorithm: the classic ITIL impact/urgency matrix, adjusted for
 * business-critical services.
 * <pre>
 *               Urgency HIGH  MEDIUM  LOW
 *   Impact HIGH         P1    P2      P3
 *   Impact MEDIUM       P2    P3      P4
 *   Impact LOW          P3    P4      P4
 * </pre>
 * Business-critical services are raised one level; supporting services lowered one.
 */
public class MatrixPriorityCalculator implements PriorityCalculator {

    private static final Priority[][] MATRIX = {
            // urgency:   LOW          MEDIUM       HIGH
            /* LOW    */ {Priority.P4, Priority.P4, Priority.P3},
            /* MEDIUM */ {Priority.P4, Priority.P3, Priority.P2},
            /* HIGH   */ {Priority.P3, Priority.P2, Priority.P1},
    };

    @Override
    public String key() {
        return "MATRIX";
    }

    @Override
    public String displayName() {
        return "ITIL impact/urgency matrix";
    }

    @Override
    public String description() {
        return "Looks up the standard 3×3 ITIL matrix, then raises the priority one level for business-critical "
                + "services and lowers it one level for supporting services.";
    }

    @Override
    public PriorityResult calculate(PriorityInput input) {
        Priority base = MATRIX[input.impact().ordinal()][input.urgency().ordinal()];
        Priority result = base;
        String adjustment = "";
        if (input.criticality() == BusinessCriticality.HIGH && base != Priority.P1) {
            result = base.raise();
            adjustment = "; raised for a business-critical service";
        } else if (input.criticality() == BusinessCriticality.LOW && base != Priority.P4) {
            result = Priority.values()[base.ordinal() + 1];
            adjustment = "; lowered for a supporting service";
        }
        int score = input.impact().getWeight() * input.urgency().getWeight() * input.criticality().getWeight();
        String rationale = "Matrix(" + input.impact() + " impact, " + input.urgency() + " urgency) = " + base + adjustment;
        return new PriorityResult(result, score, rationale);
    }
}
