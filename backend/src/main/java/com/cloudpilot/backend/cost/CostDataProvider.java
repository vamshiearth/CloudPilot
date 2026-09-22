package com.cloudpilot.backend.cost;

import java.time.LocalDate;
import java.util.List;

public interface CostDataProvider {

    CostSource source();

    List<NormalizedCostRecord> fetchDailyCosts(
        Long tenantId,
        LocalDate startDate,
        LocalDate endDate
    );
}