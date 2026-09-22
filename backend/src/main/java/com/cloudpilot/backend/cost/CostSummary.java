package com.cloudpilot.backend.cost;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CostSummary(
    LocalDate asOfDate,
    BigDecimal todayCost,
    BigDecimal monthToDateCost,
    BigDecimal previousMonthCost,
    BigDecimal dailyAverage,
    BigDecimal projectedMonthlyCost,
    CloudServiceCategory highestCostService,
    BigDecimal highestCostServiceAmount,
    List<ServiceCostSummary> services
) {
}