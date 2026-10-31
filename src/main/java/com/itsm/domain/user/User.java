package com.itsm.domain.user;

import com.itsm.domain.incident.Incident;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Base class for everyone who can log in.
 * <p>
 * Abstract because nobody is "just a user": every account is an Employee, a Technician
 * or an Administrator. Subclasses override the permission methods, so the rest of the
 * system asks {@code user.canView(incident)} without checking the concrete type.
 * All roles share one table (single-table inheritance) with a {@code role} discriminator.
 */
@Entity
@Table(name = "users")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "role", length = 20)
public abstract class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 120)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(length = 30)
    private String phone;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected User() {
        // for JPA
    }

    protected User(String name, String email, String passwordHash) {
        this.name = name;
        this.email = email.trim().toLowerCase();
        this.passwordHash = passwordHash;
        this.createdAt = LocalDateTime.now();
    }

    public abstract Role getRole();

    /** Short human-friendly code, e.g. E12 or T5, used in audit messages. */
    public String getCode() {
        return getRole().name().charAt(0) + String.valueOf(id);
    }

    /** May this user open the incident's detail page? */
    public abstract boolean canView(Incident incident);

    /** May this user do technician work (acknowledge, investigate, resolve) on the incident? */
    public abstract boolean canWorkOn(Incident incident);

    /** Is this user the person who reported the incident? */
    public boolean isReporterOf(Incident incident) {
        return incident.getReporter() != null && incident.getReporter().equals(this);
    }

    public void updateProfile(String name, String phone) {
        this.name = name;
        this.phone = phone;
    }

    public void changeEmail(String email) {
        this.email = email.trim().toLowerCase();
    }

    public void changePassword(String newPasswordHash) {
        this.passwordHash = newPasswordHash;
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getPhone() { return phone; }
    public boolean isActive() { return active; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User other)) return false;
        return getId() != null && getId().equals(other.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }

    @Override
    public String toString() {
        return name + " (" + getCode() + ")";
    }
}
