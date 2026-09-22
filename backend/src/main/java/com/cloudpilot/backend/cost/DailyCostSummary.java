package com.cloudpilot.backend.cost;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyCostSummary(
    LocalDate date,
    BigDecimal costAmount
) {
}