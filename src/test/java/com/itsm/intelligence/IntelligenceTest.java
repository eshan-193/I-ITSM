package com.itsm.intelligence;

import com.itsm.domain.catalog.BusinessCriticality;
import com.itsm.domain.catalog.Category;
import com.itsm.domain.catalog.ItService;
import com.itsm.domain.incident.ClassificationSource;
import com.itsm.domain.incident.Impact;
import com.itsm.domain.incident.Incident;
import com.itsm.domain.incident.Urgency;
import com.itsm.domain.priority.PriorityResult;
import com.itsm.domain.priority.Priority;
import com.itsm.domain.sla.SlaPolicy;
import com.itsm.domain.user.Employee;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static com.itsm.TestFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;

class IntelligenceTest {

    private final Employee reporter = employee("Rohan");
    private final ItService payments = service("Payments", BusinessCriticality.HIGH);
    private final ItService hr = service("HR Portal", BusinessCriticality.LOW);

    private Incident make(String title, String description, ItService s) {
        return withId(new Incident(title, description, reporter, s, Impact.HIGH, Urgency.HIGH, 10, T0));
    }

    @Test
    @DisplayName("TC-26 Duplicate detection groups the brief's payment examples and ignores unrelated incidents")
    void duplicateDetection() {
        Incident a = make("Payment application unavailable", "The payment app shows an error page", payments);
        Incident b = make("Printer jammed on floor 2", "Paper stuck in printer", hr);
        Incident c = make("Leave balance wrong", "HR portal shows wrong leave days", hr);
        List<SimilarIncident> found = DuplicateDetector.rank("Payment transaction failing",
                "Card payment transaction fails with error", payments, List.of(a, b, c), 0.35);
        assertThat(found).extracting(SimilarIncident::incident).containsExactly(a);
        assertThat(found.get(0).similarityPercent()).isGreaterThanOrEqualTo(35);
    }

    @Test
    @DisplayName("TC-27 Text analysis stems words and drops stop words")
    void textAnalysis() {
        assertThat(TextAnalyzer.terms("The VPN keeps disconnecting and failing")).containsExactly("vpn", "keep", "disconnect", "fail");
        assertThat(TextAnalyzer.jaccard(TextAnalyzer.termSet("vpn failing"), TextAnalyzer.termSet("vpn failed"))).isEqualTo(1.0);
    }

    @Test
    @DisplayName("TC-28 Keyword classifier picks the category with the strongest evidence")
    void keywordClassifier() {
        Category network = withId(new Category("NETWORK", "Network", null, "vpn, wifi, internet"));
        Category auth = withId(new Category("AUTHENTICATION", "Authentication", null, "login, password, sso, mfa"));
        CategorySuggestion s = KeywordCategoryClassifier.classify("Cannot login, my password is rejected by SSO",
                List.of(network, auth), Map.of()).orElseThrow();
        assertThat(s.category()).isEqualTo(auth);
        assertThat(s.source()).isEqualTo(ClassificationSource.KEYWORD);
        assertThat(KeywordCategoryClassifier.classify("Coffee machine broken", List.of(network, auth), Map.of())).isEmpty();
    }

    @Test
    @DisplayName("TC-29 SLA risk: fresh incident is low risk; unassigned and nearly due is high risk")
    void slaRisk() {
        Incident i = make("Payment down", "error", payments);
        i.applyPriority(new PriorityResult(Priority.P1, 27, "test"), new SlaPolicy(Priority.P1, null, 5, 30), T0);
        var model = new SlaRiskPredictor.HistoryModel(Map.of(), Map.of(Priority.P1, 45.0));

        SlaRisk fresh = SlaRiskPredictor.predict(i, T0.plusMinutes(1), model, 0);
        SlaRisk late = SlaRiskPredictor.predict(i, T0.plusMinutes(25), model, 0);
        assertThat(late.score()).isGreaterThan(fresh.score());
        assertThat(late.level()).isEqualTo(SlaRisk.Level.HIGH);
        assertThat(late.reasons()).anyMatch(r -> r.contains("No technician"));
        assertThat(SlaRiskPredictor.predict(i, T0.plusMinutes(40), model, 0).score()).isGreaterThanOrEqualTo(70);
    }
}
