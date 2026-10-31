package com.itsm.domain.user;

import com.itsm.domain.incident.Incident;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/** An IT manager or administrator: configures the system and oversees every incident. */
@Entity
@DiscriminatorValue("ADMIN")
public class Administrator extends User {

    @Column(name = "job_title", length = 80)
    private String jobTitle;

    protected Administrator() {
        // for JPA
    }

    public Administrator(String name, String email, String passwordHash, String jobTitle) {
        super(name, email, passwordHash);
        this.jobTitle = jobTitle;
    }

    @Override
    public Role getRole() {
        return Role.ADMIN;
    }

    @Override
    public boolean canView(Incident incident) {
        return true;
    }

    @Override
    public boolean canWorkOn(Incident incident) {
        return true;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }
}
