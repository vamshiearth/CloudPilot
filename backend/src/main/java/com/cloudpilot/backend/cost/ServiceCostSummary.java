package com.cloudpilot.backend.cost;

import java.math.BigDecimal;

public record ServiceCostSummary(
    CloudServiceCategory serviceCategory,
    BigDecimal costAmount,
    BigDecimal percentage
) {
}