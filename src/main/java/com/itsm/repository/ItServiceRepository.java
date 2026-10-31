package com.itsm.repository;

import com.itsm.domain.catalog.ItService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ItServiceRepository extends JpaRepository<ItService, Long> {

    Optional<ItService> findByNameIgnoreCase(String name);

    List<ItService> findByActiveTrueOrderByName();

    List<ItService> findAllByOrderByName();

    /** Services that depend on the given one (downstream impact). */
    @Query("select s from ItService s join s.dependsOn d where d = :service")
    List<ItService> findDependents(ItService service);
}
