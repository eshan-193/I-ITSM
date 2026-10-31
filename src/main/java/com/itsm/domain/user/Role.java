package com.itsm.domain.user;

/** The three user roles. The names double as Spring Security authorities (ROLE_*). */
public enum Role {
    EMPLOYEE("Employee"),
    TECHNICIAN("Technician"),
    ADMIN("Administrator");

    private final String label;

    Role(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public String authority() {
        return "ROLE_" + name();
    }
}
