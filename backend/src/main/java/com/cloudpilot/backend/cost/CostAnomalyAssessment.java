package com.cloudpilot.backend.cost;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CostAnomalyAssessment(
    LocalDate asOfDate,
    BigDecimal todayCost,
    BigDecimal baselineAverage,
    BigDecimal costPressure,
    int historicalDays,
    CostAnomalyStatus status
) {
}