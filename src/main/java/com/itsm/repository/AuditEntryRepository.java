package com.itsm.repository;

import com.itsm.domain.incident.AuditEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AuditEntryRepository extends JpaRepository<AuditEntry, Long>, JpaSpecificationExecutor<AuditEntry> {
}
