package com.cloudpilot.backend.cost;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
public class CostInsightService {

    private static final BigDecimal KUBERNETES_SHARE_THRESHOLD =
        new BigDecimal("50.00");
    private static final BigDecimal BUDGET_PROJECTION_THRESHOLD =
        new BigDecimal("100.00");
    private static final BigDecimal NETWORK_PRESSURE_THRESHOLD =
        new BigDecimal("1.50");
    private static final BigDecimal STORAGE_GROWTH_THRESHOLD =
        new BigDecimal("25.00");

    private final CostAggregationService aggregationService;
    private final CostBudgetService budgetService;
    private final CostAnomalyDetector anomalyDetector;
    private final TenantCostRecordRepository costRecordRepository;

    public CostInsightService(
        CostAggregationService aggregationService,
        CostBudgetService budgetService,
        CostAnomalyDetector anomalyDetector,
        TenantCostRecordRepository costRecordRepository
    ) {
        this.aggregationService = aggregationService;
        this.budgetService = budgetService;
        this.anomalyDetector = anomalyDetector;
        this.costRecordRepository = costRecordRepository;
    }

    public List<CostInsight> generate(Long tenantId, LocalDate asOfDate) {
        List<CostInsight> insights = new ArrayList<>();
        CostSummary summary = aggregationService.summarize(tenantId, asOfDate);
        BudgetSummary budget = budgetService.getBudgetSummary(tenantId, asOfDate);
        CostAnomalyAssessment anomaly = anomalyDetector.assess(
            tenantId,
            asOfDate
        );

        addKubernetesInsight(summary, insights);
        addBudgetRiskInsight(summary, budget, insights);
        addAnomalyInsight(anomaly, insights);
        addNetworkSpikeInsight(tenantId, asOfDate, insights);
        addStorageGrowthInsight(tenantId, asOfDate, insights);
        return insights;
    }

    private void addKubernetesInsight(
        CostSummary summary,
        List<CostInsight> insights
    ) {
        summary.services().stream()
            .filter(service -> service.serviceCategory()
                == CloudServiceCategory.KUBERNETES)
            .findFirst()
            .filter(service -> service.percentage()
                .compareTo(KUBERNETES_SHARE_THRESHOLD) >= 0)
            .ifPresent(service -> insights.add(new CostInsight(
                CostInsightType.HIGH_KUBERNETES_COST,
                CostInsightSeverity.WARNING,
                "Kubernetes represents " + service.percentage()
                    + "% of month-to-date spending.",
                service.percentage(),
                KUBERNETES_SHARE_THRESHOLD
            )));
    }

    private void addBudgetRiskInsight(
        CostSummary summary,
        BudgetSummary budget,
        List<CostInsight> insights
    ) {
        if (budget.health() == BudgetHealth.NO_BUDGET) {
            return;
        }

        BigDecimal projectedUsage = summary.projectedMonthlyCost()
            .multiply(BigDecimal.valueOf(100))
            .divide(budget.monthlyBudget(), 2, RoundingMode.HALF_UP);
        if (projectedUsage.compareTo(BUDGET_PROJECTION_THRESHOLD) >= 0) {
            insights.add(new CostInsight(
                CostInsightType.BUDGET_RISK,
                CostInsightSeverity.HIGH,
                "Projected monthly spending is " + projectedUsage
                    + "% of the configured budget.",
                projectedUsage,
                BUDGET_PROJECTION_THRESHOLD
            ));
        }
    }

    private void addAnomalyInsight(
        CostAnomalyAssessment anomaly,
        List<CostInsight> insights
    ) {
        if (anomaly.status() == CostAnomalyStatus.ANOMALY_CANDIDATE) {
            insights.add(new CostInsight(
                CostInsightType.COST_ANOMALY,
                CostInsightSeverity.HIGH,
                "Today's cost is " + anomaly.costPressure()
                    + "x the recent daily baseline.",
                anomaly.costPressure(),
                new BigDecimal("1.75")
            ));
        }
    }

    private void addNetworkSpikeInsight(
        Long tenantId,
        LocalDate asOfDate,
        List<CostInsight> insights
    ) {
        BigDecimal today = categoryCost(
            tenantId,
            asOfDate,
            CloudServiceCategory.NETWORK
        );
        BigDecimal baseline = categoryAverage(
            tenantId,
            asOfDate.minusDays(7),
            asOfDate.minusDays(1),
            CloudServiceCategory.NETWORK
        );
        if (baseline.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        BigDecimal pressure = today.divide(
            baseline,
            4,
            RoundingMode.HALF_UP
        );
        if (pressure.compareTo(NETWORK_PRESSURE_THRESHOLD) >= 0) {
            insights.add(new CostInsight(
                CostInsightType.NETWORK_COST_SPIKE,
                CostInsightSeverity.WARNING,
                "Network spending is " + pressure
                    + "x its recent baseline.",
                pressure,
                NETWORK_PRESSURE_THRESHOLD
            ));
        }
    }

    private void addStorageGrowthInsight(
        Long tenantId,
        LocalDate asOfDate,
        List<CostInsight> insights
    ) {
        BigDecimal recent = categoryTotal(
            tenantId,
            asOfDate.minusDays(6),
            asOfDate,
            CloudServiceCategory.STORAGE
        );
        BigDecimal prior = categoryTotal(
            tenantId,
            asOfDate.minusDays(13),
            asOfDate.minusDays(7),
            CloudServiceCategory.STORAGE
        );
        if (prior.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        BigDecimal growth = recent.subtract(prior)
            .multiply(BigDecimal.valueOf(100))
            .divide(prior, 2, RoundingMode.HALF_UP);
        if (growth.compareTo(STORAGE_GROWTH_THRESHOLD) >= 0) {
            insights.add(new CostInsight(
                CostInsightType.STORAGE_GROWTH,
                CostInsightSeverity.WARNING,
                "Storage spending increased by " + growth
                    + "% compared with the prior 7-day period.",
                growth,
                STORAGE_GROWTH_THRESHOLD
            ));
        }
    }

    private BigDecimal categoryCost(
        Long tenantId,
        LocalDate date,
        CloudServiceCategory category
    ) {
        return costRecordRepository.findByTenantIdAndUsageDate(tenantId, date)
            .stream()
            .filter(record -> record.getServiceCategory() == category)
            .map(record -> record.getCostAmount())
            .reduce(BigDecimal.ZERO, (total, amount) -> total.add(amount));
    }

    private BigDecimal categoryAverage(
        Long tenantId,
        LocalDate start,
        LocalDate end,
        CloudServiceCategory category
    ) {
        BigDecimal total = categoryTotal(tenantId, start, end, category);
        long days = ChronoUnit.DAYS.between(start, end) + 1;
        return total.divide(
            BigDecimal.valueOf(days),
            6,
            RoundingMode.HALF_UP
        );
    }

    private BigDecimal categoryTotal(
        Long tenantId,
        LocalDate start,
        LocalDate end,
        CloudServiceCategory category
    ) {
        return costRecordRepository
            .findByTenantIdAndUsageDateBetweenOrderByUsageDateAsc(
                tenantId,
                start,
                end
            )
            .stream()
            .filter(record -> record.getServiceCategory() == category)
            .map(record -> record.getCostAmount())
            .reduce(BigDecimal.ZERO, (total, amount) -> total.add(amount));
    }
}
