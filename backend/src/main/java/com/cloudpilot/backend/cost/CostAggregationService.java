package com.cloudpilot.backend.cost;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
public class CostAggregationService {

    private static final int MONEY_SCALE = 6;
    private static final int PERCENT_SCALE = 2;

    private final TenantCostRecordRepository costRecordRepository;

    public CostAggregationService(
        TenantCostRecordRepository costRecordRepository
    ) {
        this.costRecordRepository = costRecordRepository;
    }

    public CostSummary summarize(Long tenantId, LocalDate asOfDate) {
        YearMonth currentMonth = YearMonth.from(asOfDate);
        LocalDate currentMonthStart = currentMonth.atDay(1);
        LocalDate previousMonthStart = currentMonth.minusMonths(1).atDay(1);
        LocalDate previousMonthEnd = currentMonth.minusMonths(1).atEndOfMonth();

        List<TenantCostRecord> currentMonthRecords = costRecordRepository
            .findByTenantIdAndUsageDateBetweenOrderByUsageDateAsc(
                tenantId,
                currentMonthStart,
                asOfDate
            );
        List<TenantCostRecord> previousMonthRecords = costRecordRepository
            .findByTenantIdAndUsageDateBetweenOrderByUsageDateAsc(
                tenantId,
                previousMonthStart,
                previousMonthEnd
            );

        BigDecimal todayCost = sumForDate(currentMonthRecords, asOfDate);
        BigDecimal monthToDateCost = sumRecords(currentMonthRecords);
        BigDecimal previousMonthCost = sumRecords(previousMonthRecords);
        int elapsedDays = asOfDate.getDayOfMonth();
        BigDecimal dailyAverage = elapsedDays == 0
            ? BigDecimal.ZERO
            : monthToDateCost.divide(
                BigDecimal.valueOf(elapsedDays),
                MONEY_SCALE,
                RoundingMode.HALF_UP
            );
        BigDecimal projectedMonthlyCost = dailyAverage.multiply(
            BigDecimal.valueOf(currentMonth.lengthOfMonth())
        ).setScale(MONEY_SCALE, RoundingMode.HALF_UP);

        Map<CloudServiceCategory, BigDecimal> serviceTotals =
            aggregateByService(currentMonthRecords);
        List<ServiceCostSummary> serviceSummaries = buildServiceSummaries(
            serviceTotals,
            monthToDateCost
        );

        CloudServiceCategory highestCostService = null;
        BigDecimal highestCostServiceAmount = BigDecimal.ZERO.setScale(
            MONEY_SCALE,
            RoundingMode.HALF_UP
        );
        for (Map.Entry<CloudServiceCategory, BigDecimal> entry
            : serviceTotals.entrySet()) {
            if (entry.getValue().compareTo(highestCostServiceAmount) > 0) {
                highestCostService = entry.getKey();
                highestCostServiceAmount = entry.getValue();
            }
        }

        return new CostSummary(
            asOfDate,
            scaleMoney(todayCost),
            scaleMoney(monthToDateCost),
            scaleMoney(previousMonthCost),
            scaleMoney(dailyAverage),
            scaleMoney(projectedMonthlyCost),
            highestCostService,
            scaleMoney(highestCostServiceAmount),
            serviceSummaries
        );
    }

    public List<DailyCostSummary> dailyHistory(
        Long tenantId,
        LocalDate startDate,
        LocalDate endDate
    ) {
        Map<LocalDate, BigDecimal> totals = new TreeMap<>();
        costRecordRepository
            .findByTenantIdAndUsageDateBetweenOrderByUsageDateAsc(
                tenantId,
                startDate,
                endDate
            )
            .forEach(record -> totals.merge(
                record.getUsageDate(),
                record.getCostAmount(),
                (existing, amount) -> existing.add(amount)
            ));

        return totals.entrySet().stream()
            .map(entry -> new DailyCostSummary(
                entry.getKey(),
                scaleMoney(entry.getValue())
            ))
            .toList();
    }

    public List<ServiceCostSummary> serviceBreakdown(
        Long tenantId,
        LocalDate startDate,
        LocalDate endDate
    ) {
        List<TenantCostRecord> records = costRecordRepository
            .findByTenantIdAndUsageDateBetweenOrderByUsageDateAsc(
                tenantId,
                startDate,
                endDate
            );
        return buildServiceSummaries(
            aggregateByService(records),
            sumRecords(records)
        );
    }

    private BigDecimal sumForDate(
        List<TenantCostRecord> records,
        LocalDate date
    ) {
        return records.stream()
            .filter(record -> record.getUsageDate().equals(date))
            .map(record -> record.getCostAmount())
            .reduce(BigDecimal.ZERO, (total, amount) -> total.add(amount));
    }

    private BigDecimal sumRecords(List<TenantCostRecord> records) {
        return records.stream()
            .map(record -> record.getCostAmount())
            .reduce(BigDecimal.ZERO, (total, amount) -> total.add(amount));
    }

    private Map<CloudServiceCategory, BigDecimal> aggregateByService(
        List<TenantCostRecord> records
    ) {
        Map<CloudServiceCategory, BigDecimal> totals = new EnumMap<>(
            CloudServiceCategory.class
        );
        for (TenantCostRecord record : records) {
            totals.merge(
                record.getServiceCategory(),
                record.getCostAmount(),
                (existing, amount) -> existing.add(amount)
            );
        }
        return totals;
    }

    private List<ServiceCostSummary> buildServiceSummaries(
        Map<CloudServiceCategory, BigDecimal> totals,
        BigDecimal totalCost
    ) {
        List<ServiceCostSummary> summaries = new ArrayList<>();
        for (Map.Entry<CloudServiceCategory, BigDecimal> entry
            : totals.entrySet()) {
            BigDecimal percentage = totalCost.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO.setScale(PERCENT_SCALE, RoundingMode.HALF_UP)
                : entry.getValue()
                    .multiply(BigDecimal.valueOf(100))
                    .divide(totalCost, PERCENT_SCALE, RoundingMode.HALF_UP);
            summaries.add(new ServiceCostSummary(
                entry.getKey(),
                scaleMoney(entry.getValue()),
                percentage
            ));
        }
        summaries.sort((a, b) -> b.costAmount().compareTo(a.costAmount()));
        return summaries;
    }

    private BigDecimal scaleMoney(BigDecimal value) {
        return value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }
}