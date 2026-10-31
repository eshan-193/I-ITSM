package com.itsm.domain.sla;

import com.itsm.domain.catalog.ItService;
import com.itsm.domain.priority.Priority;
import jakarta.persistence.*;

import java.time.Duration;

/**
 * Response and resolution targets for one priority.
 * <p>
 * A policy with no service is the organisation-wide default; a policy linked to a
 * service overrides the default for that service only.
 */
@Entity
@Table(name = "sla_policies", uniqueConstraints = @UniqueConstraint(columnNames = {"priority", "service_id"}))
public class SlaPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 5)
    private Priority priority;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id")
    private ItService service;

    @Column(name = "response_minutes", nullable = false)
    private int responseMinutes;

    @Column(name = "resolution_minutes", nullable = false)
    private int resolutionMinutes;

    protected SlaPolicy() {
        // for JPA
    }

    public SlaPolicy(Priority priority, ItService service, int responseMinutes, int resolutionMinutes) {
        this.priority = priority;
        this.service = service;
        setTargets(responseMinutes, resolutionMinutes);
    }

    public void setTargets(int responseMinutes, int resolutionMinutes) {
        if (responseMinutes <= 0 || resolutionMinutes <= 0) {
            throw new IllegalArgumentException("SLA targets must be positive");
        }
        if (resolutionMinutes < responseMinutes) {
            throw new IllegalArgumentException("Resolution target cannot be shorter than response target");
        }
        this.responseMinutes = responseMinutes;
        this.resolutionMinutes = resolutionMinutes;
    }

    public Duration responseTarget() {
        return Duration.ofMinutes(responseMinutes);
    }

    public Duration resolutionTarget() {
        return Duration.ofMinutes(resolutionMinutes);
    }

    public boolean isDefault() {
        return service == null;
    }

    public Long getId() { return id; }
    public Priority getPriority() { return priority; }
    public ItService getService() { return service; }
    public int getResponseMinutes() { return responseMinutes; }
    public int getResolutionMinutes() { return resolutionMinutes; }
}
