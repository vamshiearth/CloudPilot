package com.cloudpilot.backend.cost;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface TenantCostRecordRepository
    extends JpaRepository<TenantCostRecord, Long> {

    @Query("""
        select distinct r.tenantId
        from TenantCostRecord r
        """)
    List<Long> findDistinctTenantIds();

    List<TenantCostRecord> findByTenantIdAndUsageDateBetweenOrderByUsageDateAsc(
        Long tenantId,
        LocalDate startDate,
        LocalDate endDate
    );

    List<TenantCostRecord> findByTenantIdAndUsageDate(
        Long tenantId,
        LocalDate usageDate
    );

    boolean existsByTenantIdAndUsageDateAndServiceCategoryAndSource(
        Long tenantId,
        LocalDate usageDate,
        CloudServiceCategory serviceCategory,
        CostSource source
    );
}