package com.cloudpilot.backend.cost;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;

@Service
public class CostBudgetService {

    private static final String CURRENCY = "USD";

    private final TenantCostBudgetRepository budgetRepository;
    private final CostAggregationService aggregationService;

    public CostBudgetService(
        TenantCostBudgetRepository budgetRepository,
        CostAggregationService aggregationService
    ) {
        this.budgetRepository = budgetRepository;
        this.aggregationService = aggregationService;
    }

    public BudgetSummary getBudgetSummary(Long tenantId, LocalDate asOfDate) {
        CostSummary costSummary = aggregationService.summarize(tenantId, asOfDate);

        return budgetRepository.findByTenantId(tenantId)
            .map(budget -> buildSummary(budget, costSummary.monthToDateCost()))
            .orElseGet(() -> new BudgetSummary(
                null,
                CURRENCY,
                costSummary.monthToDateCost(),
                BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                BudgetHealth.NO_BUDGET,
                null
            ));
    }

    @Transactional
    public BudgetSummary updateBudget(
        Long tenantId,
        BigDecimal monthlyBudget,
        LocalDate asOfDate
    ) {
        Instant now = Instant.now();
        BigDecimal normalizedBudget = monthlyBudget.setScale(
            2,
            RoundingMode.HALF_UP
        );

        TenantCostBudget budget = budgetRepository.findByTenantId(tenantId)
            .map(existing -> {
                existing.update(normalizedBudget, CURRENCY, now);
                return existing;
            })
            .orElseGet(() -> new TenantCostBudget(
                tenantId,
                normalizedBudget,
                CURRENCY,
                now
            ));

        TenantCostBudget saved = budgetRepository.save(budget);
        CostSummary costSummary = aggregationService.summarize(tenantId, asOfDate);
        return buildSummary(saved, costSummary.monthToDateCost());
    }

    private BudgetSummary buildSummary(
        TenantCostBudget budget,
        BigDecimal monthToDateCost
    ) {
        BigDecimal usagePercentage = monthToDateCost
            .multiply(BigDecimal.valueOf(100))
            .divide(budget.getMonthlyBudget(), 2, RoundingMode.HALF_UP);

        return new BudgetSummary(
            budget.getMonthlyBudget(),
            budget.getCurrency(),
            monthToDateCost,
            usagePercentage,
            classify(usagePercentage),
            budget.getUpdatedAt()
        );
    }

    private BudgetHealth classify(BigDecimal usagePercentage) {
        if (usagePercentage.compareTo(new BigDecimal("100")) >= 0) {
            return BudgetHealth.OVER_BUDGET;
        }
        if (usagePercentage.compareTo(new BigDecimal("90")) >= 0) {
            return BudgetHealth.HIGH;
        }
        if (usagePercentage.compareTo(new BigDecimal("75")) >= 0) {
            return BudgetHealth.WATCH;
        }
        return BudgetHealth.HEALTHY;
    }
}