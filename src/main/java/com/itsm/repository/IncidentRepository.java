package com.itsm.repository;

import com.itsm.domain.catalog.Category;
import com.itsm.domain.catalog.ItService;
import com.itsm.domain.incident.Incident;
import com.itsm.domain.incident.IncidentStatus;
import com.itsm.domain.user.Technician;
import com.itsm.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * Repository pattern for incidents. Simple lookups are derived from method names;
 * the multi-criteria search uses JPA Specifications (see IncidentSpecifications).
 */
public interface IncidentRepository extends JpaRepository<Incident, Long>, JpaSpecificationExecutor<Incident> {

    List<Incident> findByStatusIn(Collection<IncidentStatus> statuses);

    List<Incident> findByAssigneeAndStatusInOrderByCreatedAtDesc(Technician assignee, Collection<IncidentStatus> statuses);

    List<Incident> findByAssigneeOrderByUpdatedAtDesc(Technician assignee);

    long countByAssigneeAndStatusIn(Technician assignee, Collection<IncidentStatus> statuses);

    @Query("""
            select distinct i from Incident i left join i.watchers w
            where i.reporter = :user or w = :user
            order by i.createdAt desc
            """)
    List<Incident> findReportedOrWatchedBy(User user);

    List<Incident> findByReporterAndStatusIn(User reporter, Collection<IncidentStatus> statuses);

    List<Incident> findByStatusInAndCreatedAtAfter(Collection<IncidentStatus> statuses, LocalDateTime after);

    List<Incident> findByServiceAndStatusIn(ItService service, Collection<IncidentStatus> statuses);

    /** Fixed incidents that carry a resolution: the knowledge base for suggested resolutions. */
    @Query("""
            select i from Incident i
            where i.resolution is not null and i.duplicateOf is null
              and i.status in (com.itsm.domain.incident.IncidentStatus.RESOLVED,
                               com.itsm.domain.incident.IncidentStatus.VERIFIED,
                               com.itsm.domain.incident.IncidentStatus.CLOSED)
            """)
    List<Incident> findKnowledgeBase();

    /** Incidents with a category: training data for the TF-IDF classifier. */
    @Query("select i from Incident i where i.category is not null and i.classificationSource <> com.itsm.domain.incident.ClassificationSource.SERVICE_DEFAULT")
    List<Incident> findClassified();

    // ---- workload and history statistics, returned as [technicianId, count]

    @Query("select i.assignee.id, count(i) from Incident i where i.assignee is not null and i.status in :statuses group by i.assignee.id")
    List<Object[]> countByAssigneeGrouped(Collection<IncidentStatus> statuses);

    @Query("select i.assignee.id, count(i) from Incident i where i.assignee is not null and i.resolvedAt is not null and i.category = :category group by i.assignee.id")
    List<Object[]> countResolvedByAssigneeInCategory(Category category);

    @Query("select i.assignee.id, count(i) from Incident i where i.assignee is not null and i.resolvedAt is not null and i.service = :service group by i.assignee.id")
    List<Object[]> countResolvedByAssigneeForService(ItService service);

    // ---- reporting aggregates

    @Query("select i.status, count(i) from Incident i group by i.status")
    List<Object[]> countByStatus();

    @Query("select i.priority, count(i) from Incident i where i.priority is not null group by i.priority")
    List<Object[]> countByPriority();

    @Query("select i.priority, count(i) from Incident i where i.priority is not null and i.status in :statuses group by i.priority")
    List<Object[]> countByPriorityAndStatusIn(Collection<IncidentStatus> statuses);

    @Query("select c.name, count(i) from Incident i join i.category c group by c.name order by count(i) desc")
    List<Object[]> countByCategory();

    @Query("select s.name, count(i), sum(case when i.status in :open then 1 else 0 end) from Incident i join i.service s group by s.name order by count(i) desc")
    List<Object[]> countByService(Collection<IncidentStatus> open);

    long countByStatusIn(Collection<IncidentStatus> statuses);

    @Query("select count(i) from Incident i where i.sla.responseBreached = true or i.sla.resolutionBreached = true")
    long countSlaBreached();

    List<Incident> findByCreatedAtAfter(LocalDateTime after);

    List<Incident> findByResolvedAtAfter(LocalDateTime after);

    @Query("select i from Incident i where i.resolvedAt is not null")
    List<Incident> findAllResolved();
}
