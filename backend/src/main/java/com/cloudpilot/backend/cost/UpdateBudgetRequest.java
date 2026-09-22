package com.cloudpilot.backend.cost;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record UpdateBudgetRequest(
    @NotNull
    @DecimalMin(value = "0.01")
    BigDecimal monthlyBudget
) {
}