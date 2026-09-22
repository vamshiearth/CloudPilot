package com.cloudpilot.backend.cost;

import java.math.BigDecimal;
import java.time.LocalDate;

public record NormalizedCostRecord(
    CloudProvider provider,
    CloudServiceCategory serviceCategory,
    String providerServiceName,
    LocalDate usageDate,
    BigDecimal costAmount,
    String currency,
    CostRecordStatus recordStatus
) {
}