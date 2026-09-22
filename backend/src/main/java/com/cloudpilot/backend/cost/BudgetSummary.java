package com.cloudpilot.backend.cost;

import java.math.BigDecimal;
import java.time.Instant;

public record BudgetSummary(
    BigDecimal monthlyBudget,
    String currency,
    BigDecimal monthToDateCost,
    BigDecimal usagePercentage,
    BudgetHealth health,
    Instant updatedAt
) {
}