package com.cloudpilot.backend.cost;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CostAnomalyDetector {

    private static final int MINIMUM_HISTORY_DAYS = 3;
    private static final BigDecimal ELEVATED_THRESHOLD = new BigDecimal("1.25");
    private static final BigDecimal ANOMALY_THRESHOLD = new BigDecimal("1.75");
    private static final int MONEY_SCALE = 6;
    private static final int PRESSURE_SCALE = 4;

    private final TenantCostRecordRepository costRecordRepository;

    public CostAnomalyDetector(TenantCostRecordRepository costRecordRepository) {
        this.costRecordRepository = costRecordRepository;
    }

    public CostAnomalyAssessment assess(Long tenantId, LocalDate asOfDate) {
        LocalDate historyStart = asOfDate.minusDays(7);
        LocalDate historyEnd = asOfDate.minusDays(1);
        List<TenantCostRecord> historicalRecords = costRecordRepository
            .findByTenantIdAndUsageDateBetweenOrderByUsageDateAsc(
                tenantId,
                historyStart,
                historyEnd
            );
        List<TenantCostRecord> todayRecords = costRecordRepository
            .findByTenantIdAndUsageDate(tenantId, asOfDate);

        BigDecimal todayCost = sumRecords(todayRecords);
        Map<LocalDate, BigDecimal> historicalDailyTotals = aggregateByDay(
            historicalRecords
        );
        int historicalDays = historicalDailyTotals.size();

        if (historicalDays < MINIMUM_HISTORY_DAYS) {
            return new CostAnomalyAssessment(
                asOfDate,
                scaleMoney(todayCost),
                scaleMoney(BigDecimal.ZERO),
                BigDecimal.ZERO.setScale(PRESSURE_SCALE, RoundingMode.HALF_UP),
                historicalDays,
                CostAnomalyStatus.INSUFFICIENT_DATA
            );
        }

        BigDecimal historicalTotal = historicalDailyTotals.values().stream()
            .reduce(BigDecimal.ZERO, (total, amount) -> total.add(amount));
        BigDecimal baselineAverage = historicalTotal.divide(
            BigDecimal.valueOf(historicalDays),
            MONEY_SCALE,
            RoundingMode.HALF_UP
        );

        if (baselineAverage.compareTo(BigDecimal.ZERO) == 0) {
            CostAnomalyStatus status = todayCost.compareTo(BigDecimal.ZERO) > 0
                ? CostAnomalyStatus.ANOMALY_CANDIDATE
                : CostAnomalyStatus.NORMAL;
            return new CostAnomalyAssessment(
                asOfDate,
                scaleMoney(todayCost),
                scaleMoney(baselineAverage),
                BigDecimal.ZERO.setScale(PRESSURE_SCALE, RoundingMode.HALF_UP),
                historicalDays,
                status
            );
        }

        BigDecimal costPressure = todayCost.divide(
            baselineAverage,
            PRESSURE_SCALE,
            RoundingMode.HALF_UP
        );
        return new CostAnomalyAssessment(
            asOfDate,
            scaleMoney(todayCost),
            scaleMoney(baselineAverage),
            costPressure,
            historicalDays,
            classify(costPressure)
        );
    }

    private CostAnomalyStatus classify(BigDecimal costPressure) {
        if (costPressure.compareTo(ANOMALY_THRESHOLD) >= 0) {
            return CostAnomalyStatus.ANOMALY_CANDIDATE;
        }
        if (costPressure.compareTo(ELEVATED_THRESHOLD) >= 0) {
            return CostAnomalyStatus.ELEVATED;
        }
        return CostAnomalyStatus.NORMAL;
    }

    private Map<LocalDate, BigDecimal> aggregateByDay(
        List<TenantCostRecord> records
    ) {
        Map<LocalDate, BigDecimal> totals = new HashMap<>();
        for (TenantCostRecord record : records) {
            totals.merge(
                record.getUsageDate(),
                record.getCostAmount(),
                (existing, amount) -> existing.add(amount)
            );
        }
        return totals;
    }

    private BigDecimal sumRecords(List<TenantCostRecord> records) {
        return records.stream()
            .map(record -> record.getCostAmount())
            .reduce(BigDecimal.ZERO, (total, amount) -> total.add(amount));
    }

    private BigDecimal scaleMoney(BigDecimal value) {
        return value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }
}