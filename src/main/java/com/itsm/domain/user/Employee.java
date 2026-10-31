package com.itsm.domain.user;

import com.itsm.domain.incident.Incident;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/** A member of staff who reports incidents. */
@Entity
@DiscriminatorValue("EMPLOYEE")
public class Employee extends User {

    @Column(length = 80)
    private String department;

    protected Employee() {
        // for JPA
    }

    public Employee(String name, String email, String passwordHash, String department) {
        super(name, email, passwordHash);
        this.department = department;
    }

    @Override
    public Role getRole() {
        return Role.EMPLOYEE;
    }

    /** Employees see only incidents they reported or joined ("me too"). */
    @Override
    public boolean canView(Incident incident) {
        return isReporterOf(incident) || incident.isWatchedBy(this);
    }

    @Override
    public boolean canWorkOn(Incident incident) {
        return false;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }
}
