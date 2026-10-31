package com.itsm.service;

import com.itsm.domain.incident.Incident;
import com.itsm.domain.incident.IncidentStatus;
import com.itsm.domain.user.User;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.Locale;

/**
 * Small composable query pieces. Each method returns null when its criterion is empty,
 * and Spring Data ignores null specifications when they are combined with {@code and}.
 */
final class IncidentSpecifications {

    private IncidentSpecifications() {
    }

    static Specification<Incident> idEquals(Long id) {
        return id == null ? null : (root, q, cb) -> cb.equal(root.get("id"), id);
    }

    static Specification<Incident> keyword(String keyword) {
        if (keyword == null || keyword.isBlank()) return null;
        String like = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
        return (root, q, cb) -> cb.or(
                cb.like(cb.lower(root.get("title")), like),
                cb.like(cb.lower(root.get("description")), like));
    }

    static Specification<Incident> status(IncidentStatus status) {
        return status == null ? null : (root, q, cb) -> cb.equal(root.get("status"), status);
    }

    static Specification<Incident> openOnly(boolean openOnly) {
        return !openOnly ? null : (root, q, cb) -> root.get("status").in(IncidentStatus.UNRESOLVED);
    }

    static Specification<Incident> priority(Object priority) {
        return priority == null ? null : (root, q, cb) -> cb.equal(root.get("priority"), priority);
    }

    static Specification<Incident> category(Long categoryId) {
        return categoryId == null ? null : (root, q, cb) -> cb.equal(root.get("category").get("id"), categoryId);
    }

    static Specification<Incident> service(Long serviceId) {
        return serviceId == null ? null : (root, q, cb) -> cb.equal(root.get("service").get("id"), serviceId);
    }

    static Specification<Incident> technician(Long technicianId) {
        if (technicianId == null) return null;
        if (technicianId < 0) return (root, q, cb) -> cb.isNull(root.get("assignee"));
        return (root, q, cb) -> cb.equal(root.get("assignee").get("id"), technicianId);
    }

    static Specification<Incident> slaBreached() {
        return (root, q, cb) -> cb.or(
                cb.isTrue(root.get("sla").get("responseBreached")),
                cb.isTrue(root.get("sla").get("resolutionBreached")));
    }

    static Specification<Incident> createdFrom(LocalDate from) {
        return from == null ? null : (root, q, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), from.atStartOfDay());
    }

    static Specification<Incident> createdTo(LocalDate to) {
        return to == null ? null : (root, q, cb) -> cb.lessThan(root.get("createdAt"), to.plusDays(1).atStartOfDay());
    }

    /** Employees only ever see incidents they reported or joined. */
    static Specification<Incident> visibleToEmployee(User user) {
        return (root, q, cb) -> {
            q.distinct(true);
            Join<Incident, User> watchers = root.join("watchers", JoinType.LEFT);
            return cb.or(cb.equal(root.get("reporter"), user), cb.equal(watchers, user));
        };
    }
}
