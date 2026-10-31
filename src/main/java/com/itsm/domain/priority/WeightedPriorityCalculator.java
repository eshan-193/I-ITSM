package com.itsm.domain.priority;

/**
 * Default algorithm: {@code score = impact × urgency × business criticality} (1–27).
 * <ul>
 *   <li>P1 if score ≥ 18, P2 if ≥ 9, P3 if ≥ 4, otherwise P4;</li>
 *   <li>raised one level when more than {@value #MASS_IMPACT_USERS} users are affected.</li>
 * </ul>
 * Multiplication means a low value in any factor pulls the score down sharply, so a
 * minor issue on a critical service is not automatically P1.
 */
public class WeightedPriorityCalculator implements PriorityCalculator {

    public static final int MASS_IMPACT_USERS = 50;

    @Override
    public String key() {
        return "WEIGHTED";
    }

    @Override
    public String displayName() {
        return "Weighted score (impact × urgency × criticality)";
    }

    @Override
    public String description() {
        return "Multiplies the three factors (1–27). P1 ≥ 18, P2 ≥ 9, P3 ≥ 4. Raised one level if more than "
                + MASS_IMPACT_USERS + " users are affected.";
    }

    @Override
    public PriorityResult calculate(PriorityInput input) {
        int score = input.impact().getWeight() * input.urgency().getWeight() * input.criticality().getWeight();
        Priority priority = fromScore(score);
        StringBuilder rationale = new StringBuilder()
                .append("Impact ").append(input.impact().getWeight())
                .append(" × Urgency ").append(input.urgency().getWeight())
                .append(" × Criticality ").append(input.criticality().getWeight())
                .append(" = ").append(score).append(" → ").append(priority);
        if (input.affectedUsers() > MASS_IMPACT_USERS && priority != Priority.P1) {
            priority = priority.raise();
            rationale.append("; raised to ").append(priority)
                    .append(" because ").append(input.affectedUsers()).append(" users are affected");
        }
        return new PriorityResult(priority, score, rationale.toString());
    }

    static Priority fromScore(int score) {
        if (score >= 18) return Priority.P1;
        if (score >= 9) return Priority.P2;
        if (score >= 4) return Priority.P3;
        return Priority.P4;
    }
}
