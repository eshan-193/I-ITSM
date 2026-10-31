package com.itsm.domain.user;

import com.itsm.domain.catalog.Category;
import com.itsm.domain.incident.Incident;
import jakarta.persistence.*;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * A support engineer who investigates and resolves incidents.
 * <p>
 * A technician flagged as team lead is the first escalation level for unacknowledged
 * incidents, and may work on any incident.
 */
@Entity
@DiscriminatorValue("TECHNICIAN")
public class Technician extends User {

    /** A Set: a skill is either present or not, duplicates are meaningless, and lookup is O(1). */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "technician_skills",
            joinColumns = @JoinColumn(name = "technician_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id"))
    private Set<Category> skills = new HashSet<>();

    @Column(name = "max_active_incidents")
    private int maxActiveIncidents = 5;

    @Column(name = "available")
    private boolean available = true;

    @Column(name = "team_lead")
    private boolean teamLead;

    @Column(name = "experience_years")
    private int experienceYears;

    protected Technician() {
        // for JPA
    }

    public Technician(String name, String email, String passwordHash, int maxActiveIncidents, int experienceYears) {
        super(name, email, passwordHash);
        setMaxActiveIncidents(maxActiveIncidents);
        this.experienceYears = experienceYears;
    }

    @Override
    public Role getRole() {
        return Role.TECHNICIAN;
    }

    /** Support staff can view every incident, so they can spot related problems. */
    @Override
    public boolean canView(Incident incident) {
        return true;
    }

    @Override
    public boolean canWorkOn(Incident incident) {
        return teamLead || equals(incident.getAssignee());
    }

    public boolean hasSkill(Category category) {
        return category != null && skills.contains(category);
    }

    /** Hides the workload rule: available and below the configured limit. */
    public boolean canTakeMore(int currentActiveIncidents) {
        return available && isActive() && currentActiveIncidents < maxActiveIncidents;
    }

    public void addSkill(Category category) {
        skills.add(category);
    }

    public void replaceSkills(Set<Category> newSkills) {
        skills.clear();
        skills.addAll(newSkills);
    }

    public void setMaxActiveIncidents(int maxActiveIncidents) {
        if (maxActiveIncidents < 1 || maxActiveIncidents > 50) {
            throw new IllegalArgumentException("Workload limit must be between 1 and 50");
        }
        this.maxActiveIncidents = maxActiveIncidents;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public void setTeamLead(boolean teamLead) {
        this.teamLead = teamLead;
    }

    public void setExperienceYears(int experienceYears) {
        this.experienceYears = Math.max(0, experienceYears);
    }

    public Set<Category> getSkills() {
        return Collections.unmodifiableSet(skills);
    }

    public int getMaxActiveIncidents() { return maxActiveIncidents; }
    public boolean isAvailable() { return available; }
    public boolean isTeamLead() { return teamLead; }
    public int getExperienceYears() { return experienceYears; }
}
