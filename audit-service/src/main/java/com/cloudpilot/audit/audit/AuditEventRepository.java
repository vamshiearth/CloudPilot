package com.cloudpilot.audit.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {

    boolean existsByEventId(UUID eventId);

    Page<AuditEvent> findByTenantIdOrderByOccurredAtDesc(Long tenantId, Pageable pageable);
}
