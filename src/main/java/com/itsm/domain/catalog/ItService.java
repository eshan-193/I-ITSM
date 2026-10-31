package com.itsm.domain.catalog;

import jakarta.persistence.*;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * An IT service the organisation runs, such as "Payment Gateway" or "Corporate VPN".
 * <p>
 * Services form a dependency graph (Payments depends on Database and Authentication),
 * which the correlation engine uses to point at a probable root cause.
 */
@Entity
@Table(name = "it_services")
public class ItService {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BusinessCriticality criticality;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "default_category_id")
    private Category defaultCategory;

    @ManyToMany
    @JoinTable(name = "service_dependencies",
            joinColumns = @JoinColumn(name = "service_id"),
            inverseJoinColumns = @JoinColumn(name = "depends_on_id"))
    private Set<ItService> dependsOn = new LinkedHashSet<>();

    @Column(nullable = false)
    private boolean active = true;

    protected ItService() {
        // for JPA
    }

    public ItService(String name, String description, BusinessCriticality criticality, Category defaultCategory) {
        this.name = name;
        this.description = description;
        this.criticality = criticality;
        this.defaultCategory = defaultCategory;
    }

    public void update(String name, String description, BusinessCriticality criticality,
                       Category defaultCategory, boolean active) {
        this.name = name;
        this.description = description;
        this.criticality = criticality;
        this.defaultCategory = defaultCategory;
        this.active = active;
    }

    public void addDependency(ItService service) {
        if (service.equals(this)) {
            throw new IllegalArgumentException("A service cannot depend on itself");
        }
        dependsOn.add(service);
    }

    public void replaceDependencies(Set<ItService> services) {
        dependsOn.clear();
        services.forEach(this::addDependency);
    }

    public boolean dependsOn(ItService other) {
        return dependsOn.contains(other);
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public BusinessCriticality getCriticality() { return criticality; }
    public Category getDefaultCategory() { return defaultCategory; }
    public boolean isActive() { return active; }

    public Set<ItService> getDependsOn() {
        return Collections.unmodifiableSet(dependsOn);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ItService other)) return false;
        return getId() != null && getId().equals(other.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }

    @Override
    public String toString() {
        return name;
    }
}
