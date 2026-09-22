package com.cloudpilot.backend.cost;

import java.math.BigDecimal;

public record CostInsight(
    CostInsightType type,
    CostInsightSeverity severity,
    String message,
    BigDecimal measuredValue,
    BigDecimal thresholdValue
) {
}