package com.cloudpilot.backend.cost;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TenantCostBudgetRepository
    extends JpaRepository<TenantCostBudget, Long> {

    Optional<TenantCostBudget> findByTenantId(Long tenantId);
}