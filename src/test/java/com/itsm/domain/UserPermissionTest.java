package com.itsm.domain;

import com.itsm.domain.catalog.BusinessCriticality;
import com.itsm.domain.catalog.Category;
import com.itsm.domain.incident.ClassificationSource;
import com.itsm.domain.incident.Impact;
import com.itsm.domain.incident.Incident;
import com.itsm.domain.incident.Urgency;
import com.itsm.domain.user.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.itsm.TestFixtures.*;
import static org.assertj.core.api.Assertions.*;

/** Polymorphism: each User subclass answers permission questions differently. */
class UserPermissionTest {

    @Test
    @DisplayName("TC-24 Role permissions: employee own only, technician assigned only, team lead and admin everything")
    void permissionsByRole() {
        Category c = category("APPLICATION");
        Employee owner = employee("Owner");
        Employee stranger = employee("Stranger");
        Technician assigned = technician("Assigned", 5, 1, c);
        Technician other = technician("Other", 5, 1, c);
        Technician lead = technician("Lead", 5, 1, c);
        lead.setTeamLead(true);
        Administrator admin = admin();
        Incident i = incident("Portal down", owner, service("Portal", BusinessCriticality.LOW), Impact.LOW, Urgency.LOW);
        i.classify(c, ClassificationSource.MANUAL, 1, T0);
        i.assignTo(assigned, T0);

        assertThat(owner.canView(i)).isTrue();
        assertThat(stranger.canView(i)).isFalse();
        assertThat(owner.canWorkOn(i)).isFalse();
        assertThat(assigned.canWorkOn(i)).isTrue();
        assertThat(other.canWorkOn(i)).isFalse();
        assertThat(other.canView(i)).isTrue();
        assertThat(lead.canWorkOn(i)).isTrue();
        assertThat(admin.canWorkOn(i)).isTrue();
        assertThat(admin.getRole()).isEqualTo(Role.ADMIN);
    }

    @Test
    @DisplayName("TC-25 Technician workload limit is enforced and validated")
    void workloadLimit() {
        Technician t = technician("T", 2, 1);
        assertThat(t.canTakeMore(1)).isTrue();
        assertThat(t.canTakeMore(2)).isFalse();
        t.setAvailable(false);
        assertThat(t.canTakeMore(0)).isFalse();
        assertThatThrownBy(() -> t.setMaxActiveIncidents(0)).isInstanceOf(IllegalArgumentException.class);
    }
}
