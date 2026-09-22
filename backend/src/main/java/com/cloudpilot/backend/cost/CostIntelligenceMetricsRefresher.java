package com.cloudpilot.backend.cost;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Component
public class CostIntelligenceMetricsRefresher {

    private final TenantCostRecordRepository costRecordRepository;
    private final CostBudgetService budgetService;
    private final CostAnomalyDetector anomalyDetector;
    private final CostIntelligenceMetrics metrics;

    public CostIntelligenceMetricsRefresher(
        TenantCostRecordRepository costRecordRepository,
        CostBudgetService budgetService,
        CostAnomalyDetector anomalyDetector,
        CostIntelligenceMetrics metrics
    ) {
        this.costRecordRepository = costRecordRepository;
        this.budgetService = budgetService;
        this.anomalyDetector = anomalyDetector;
        this.metrics = metrics;
    }

    @Scheduled(
        fixedDelayString = "${cloudpilot.cost.metrics.refresh-ms:30000}"
    )
    public void refresh() {
        List<Long> tenantIds = costRecordRepository.findDistinctTenantIds();
        LocalDate today = LocalDate.now();

        int tenantsWithBudget = 0;
        int overBudgetTenants = 0;
        int anomalyCandidates = 0;
        BigDecimal maxPressure = BigDecimal.ZERO;
        BigDecimal maxBudgetUsage = BigDecimal.ZERO;

        for (Long tenantId : tenantIds) {
            BudgetSummary budget = budgetService.getBudgetSummary(tenantId, today);

            if (budget.health() != BudgetHealth.NO_BUDGET) {
                tenantsWithBudget++;
                maxBudgetUsage = max(maxBudgetUsage, budget.usagePercentage());
            }
            if (budget.health() == BudgetHealth.OVER_BUDGET) {
                overBudgetTenants++;
            }

            CostAnomalyAssessment anomaly = anomalyDetector.assess(tenantId, today);
            if (anomaly.status() == CostAnomalyStatus.ANOMALY_CANDIDATE) {
                anomalyCandidates++;
            }
            maxPressure = max(maxPressure, anomaly.costPressure());
        }

        metrics.update(
            tenantsWithBudget,
            overBudgetTenants,
            anomalyCandidates,
            maxPressure.doubleValue(),
            maxBudgetUsage.doubleValue()
        );
    }

    private BigDecimal max(BigDecimal left, BigDecimal right) {
        return left.compareTo(right) >= 0 ? left : right;
    }
}