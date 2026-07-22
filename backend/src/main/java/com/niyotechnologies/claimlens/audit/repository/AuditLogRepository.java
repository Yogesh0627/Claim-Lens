package com.niyotechnologies.claimlens.audit.repository;

import com.niyotechnologies.claimlens.audit.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findAllByEntityTypeAndEntityIdOrderByCreatedAtAsc(String entityType, Long entityId);
}
