package com.cloudpilot.backend.tenants;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;
import java.util.Optional;

public interface TenantMembershipRepository
        extends JpaRepository<TenantMembership, Long> {

    List<TenantMembership> findByUserId(Long userId);

    Optional<TenantMembership> findByUserIdAndTenantId(
            Long userId,
            Long tenantId
    );

    boolean existsByUserIdAndTenantId(
            Long userId,
            Long tenantId
    );

    @EntityGraph(
            attributePaths = {
                    "role",
                    "role.permissions"
            }
    )
    Optional<TenantMembership> findByUserIdAndTenantIdAndStatus(
            Long userId,
            Long tenantId,
            String status
    );

    @EntityGraph(
            attributePaths = {
                    "user",
                    "role"
            }
    )
    List<TenantMembership> findAllByTenant_IdAndStatus(
            Long tenantId,
            String status
    );

    @EntityGraph(
            attributePaths = {
                    "user",
                    "role"
            }
    )
    Optional<TenantMembership> findByIdAndTenant_Id(
            Long membershipId,
            Long tenantId
    );

    long countByTenant_IdAndRole_NameAndStatus(
            Long tenantId,
            String roleName,
            String status
    );

    long countByTenant_IdAndStatus(
            Long tenantId,
            String status
    );
}
