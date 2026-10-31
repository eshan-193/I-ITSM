package com.itsm.domain;

import com.itsm.domain.assignment.*;
import com.itsm.domain.catalog.BusinessCriticality;
import com.itsm.domain.catalog.Category;
import com.itsm.domain.incident.ClassificationSource;
import com.itsm.domain.incident.Impact;
import com.itsm.domain.incident.Incident;
import com.itsm.domain.incident.Urgency;
import com.itsm.domain.user.Technician;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.itsm.TestFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;

/** Strategy + Template Method: each strategy picks a different but sensible technician. */
class AssignmentStrategyTest {

    private Category database;
    private Incident incident;
    private Technician dbExpertBusy;
    private Technician dbJuniorFree;
    private Technician networkFree;
    private Technician dbFull;

    @BeforeEach
    void setUp() {
        database = category("DATABASE");
        Category network = category("NETWORK");
        dbExpertBusy = technician("Ananya", 5, 9, database);
        dbJuniorFree = technician("Rahul", 5, 1, database);
        networkFree = technician("Karan", 5, 3, network);
        dbFull = technician("Full", 2, 10, database);
        incident = incident("DB slow", employee("Rohan"), service("Core DB", BusinessCriticality.HIGH), Impact.HIGH, Urgency.HIGH);
        incident.classify(database, ClassificationSource.MANUAL, 1, T0);
    }

    private List<TechnicianCandidate> candidates() {
        return List.of(
                new TechnicianCandidate(dbExpertBusy, 3, 8, 4),
                new TechnicianCandidate(dbJuniorFree, 0, 0, 0),
                new TechnicianCandidate(networkFree, 0, 0, 0),
                new TechnicianCandidate(dbFull, 2, 10, 5));     // at limit
    }

    @Test
    @DisplayName("TC-14 Skill-based: only skilled technicians, least loaded first; full technicians excluded")
    void skillBased() {
        List<RankedTechnician> ranked = new SkillBasedAssignmentStrategy().rank(incident, candidates());
        assertThat(ranked).extracting(r -> r.candidate().technician()).containsExactly(dbJuniorFree, dbExpertBusy);
    }

    @Test
    @DisplayName("TC-15 Load-based: lowest workload ratio regardless of skill")
    void loadBased() {
        RankedTechnician first = new LoadBasedAssignmentStrategy().select(incident, candidates()).orElseThrow();
        assertThat(first.candidate().activeIncidents()).isZero();
    }

    @Test
    @DisplayName("TC-16 Round-robin: consecutive incidents go to different technicians")
    void roundRobinRotates() {
        RoundRobinAssignmentStrategy rr = new RoundRobinAssignmentStrategy();
        Technician first = rr.select(incident, candidates()).orElseThrow().candidate().technician();
        Technician second = rr.select(incident, candidates()).orElseThrow().candidate().technician();
        assertThat(first).isNotEqualTo(second);
    }

    @Test
    @DisplayName("TC-17 Smart: an experienced, skilled technician with history beats an idle unskilled one")
    void smartPrefersRelevantExperience() {
        RankedTechnician best = new SmartAssignmentStrategy().select(incident, candidates()).orElseThrow();
        assertThat(best.candidate().technician()).isEqualTo(dbExpertBusy);
        assertThat(best.reason()).contains("skill match");
    }

    @Test
    @DisplayName("TC-18 No eligible technician (all unavailable or full) → empty result")
    void noneAvailable() {
        dbJuniorFree.setAvailable(false);
        List<TechnicianCandidate> onlyFull = List.of(new TechnicianCandidate(dbFull, 2, 0, 0),
                new TechnicianCandidate(dbJuniorFree, 0, 0, 0));
        assertThat(new SmartAssignmentStrategy().select(incident, onlyFull)).isEmpty();
        assertThat(new LoadBasedAssignmentStrategy().select(incident, onlyFull)).isEmpty();
    }
}
