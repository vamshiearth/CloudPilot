package com.cloudpilot.backend.tenants;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TenantRepository
        extends JpaRepository<Tenant, Long> {

    Optional<Tenant> findBySlug(String slug);

    boolean existsBySlug(String slug);

    @EntityGraph(attributePaths = {"plan", "plan.features"})
    Optional<Tenant> findOneById(Long id);
}
