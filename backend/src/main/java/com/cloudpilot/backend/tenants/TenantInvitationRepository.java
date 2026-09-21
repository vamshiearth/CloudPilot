package com.cloudpilot.backend.tenants;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

public interface TenantInvitationRepository
        extends JpaRepository<TenantInvitation, Long> {

    @EntityGraph(attributePaths = {"tenant", "role", "invitedBy"})
    List<TenantInvitation> findAllByTenant_IdAndStatus(
            Long tenantId,
            String status
    );

        @EntityGraph(attributePaths = {"tenant", "role", "invitedBy"})
        Optional<TenantInvitation> findByToken(String token);

    boolean existsByTenant_IdAndEmailAndStatus(
            Long tenantId,
            String email,
            String status
    );

    long countByTenant_IdAndStatusAndExpiresAtAfter(
            Long tenantId,
            String status,
            LocalDateTime dateTime
    );

    @EntityGraph(attributePaths = {"tenant", "role", "invitedBy"})
    Optional<TenantInvitation> findByIdAndTenant_IdAndStatus(
            Long invitationId,
            Long tenantId,
            String status
    );
}