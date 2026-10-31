package com.itsm.repository;

import com.itsm.domain.catalog.ItService;
import com.itsm.domain.priority.Priority;
import com.itsm.domain.sla.SlaPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SlaPolicyRepository extends JpaRepository<SlaPolicy, Long> {

    Optional<SlaPolicy> findByPriorityAndService(Priority priority, ItService service);

    Optional<SlaPolicy> findByPriorityAndServiceIsNull(Priority priority);

    List<SlaPolicy> findByServiceIsNullOrderByPriority();

    List<SlaPolicy> findByServiceIsNotNullOrderByPriority();
}
