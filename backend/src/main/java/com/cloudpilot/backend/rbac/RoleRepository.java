package com.cloudpilot.backend.rbac;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoleRepository
        extends JpaRepository<Role, Long> {

    Optional<Role> findByTenant_IdAndName(
            Long tenantId,
            String name
    );

    List<Role> findAllByTenant_Id(
            Long tenantId
    );

    boolean existsByTenant_IdAndName(
            Long tenantId,
            String name
    );
}
