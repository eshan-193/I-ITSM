package com.itsm.domain;

import com.itsm.domain.catalog.BusinessCriticality;
import com.itsm.domain.incident.Impact;
import com.itsm.domain.incident.Urgency;
import com.itsm.domain.priority.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/** Strategy pattern: two interchangeable priority algorithms. */
class PriorityCalculatorTest {

    private final PriorityCalculator weighted = new WeightedPriorityCalculator();
    private final PriorityCalculator matrix = new MatrixPriorityCalculator();

    @ParameterizedTest(name = "{0}×{1}×{2} = {3} → {4}")
    @CsvSource({
            "HIGH,HIGH,HIGH,27,P1",
            "HIGH,MEDIUM,HIGH,18,P1",     // lower boundary of P1
            "MEDIUM,MEDIUM,HIGH,12,P2",
            "HIGH,HIGH,LOW,9,P2",         // lower boundary of P2
            "MEDIUM,MEDIUM,MEDIUM,8,P3",
            "LOW,MEDIUM,MEDIUM,4,P3",     // lower boundary of P3
            "LOW,HIGH,LOW,3,P4",
            "LOW,LOW,LOW,1,P4"})
    @DisplayName("TC-11 Weighted score boundaries map to the documented priority bands")
    void weightedBands(Impact impact, Urgency urgency, BusinessCriticality crit, int score, Priority expected) {
        PriorityResult r = weighted.calculate(new PriorityInput(impact, urgency, crit, 1));
        assertThat(r.score()).isEqualTo(score);
        assertThat(r.priority()).isEqualTo(expected);
    }

    @Test
    @DisplayName("TC-12 More than 50 affected users raises priority one level (never above P1)")
    void massImpactRaisesPriority() {
        PriorityInput fiftyUsers = new PriorityInput(Impact.MEDIUM, Urgency.MEDIUM, BusinessCriticality.MEDIUM, 50);
        PriorityInput fiftyOne = new PriorityInput(Impact.MEDIUM, Urgency.MEDIUM, BusinessCriticality.MEDIUM, 51);
        assertThat(weighted.calculate(fiftyUsers).priority()).isEqualTo(Priority.P3);
        assertThat(weighted.calculate(fiftyOne).priority()).isEqualTo(Priority.P2);
        assertThat(weighted.calculate(fiftyOne).rationale()).contains("51 users");
        assertThat(weighted.calculate(new PriorityInput(Impact.HIGH, Urgency.HIGH, BusinessCriticality.HIGH, 500)).priority())
                .isEqualTo(Priority.P1);
    }

    @Test
    @DisplayName("TC-13 ITIL matrix adjusts for business criticality")
    void matrixAdjustsForCriticality() {
        assertThat(matrix.calculate(new PriorityInput(Impact.MEDIUM, Urgency.MEDIUM, BusinessCriticality.MEDIUM, 1)).priority())
                .isEqualTo(Priority.P3);
        assertThat(matrix.calculate(new PriorityInput(Impact.MEDIUM, Urgency.MEDIUM, BusinessCriticality.HIGH, 1)).priority())
                .isEqualTo(Priority.P2);
        assertThat(matrix.calculate(new PriorityInput(Impact.MEDIUM, Urgency.MEDIUM, BusinessCriticality.LOW, 1)).priority())
                .isEqualTo(Priority.P4);
        assertThat(matrix.calculate(new PriorityInput(Impact.HIGH, Urgency.HIGH, BusinessCriticality.LOW, 1)).priority())
                .isEqualTo(Priority.P2);
    }
}
