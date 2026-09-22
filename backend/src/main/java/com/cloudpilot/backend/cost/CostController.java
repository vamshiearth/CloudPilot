package com.cloudpilot.backend.cost;

import com.cloudpilot.backend.auth.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.List;

@RestController
@RequestMapping("/api/costs")
public class CostController {

    private static final long MAX_HISTORY_DAYS = 366;

    private final CostAggregationService aggregationService;
    private final CostBudgetService budgetService;
    private final CostAnomalyDetector anomalyDetector;
    private final CostInsightService insightService;

    public CostController(
        CostAggregationService aggregationService,
        CostBudgetService budgetService,
        CostAnomalyDetector anomalyDetector,
        CostInsightService insightService
    ) {
        this.aggregationService = aggregationService;
        this.budgetService = budgetService;
        this.anomalyDetector = anomalyDetector;
        this.insightService = insightService;
    }

    @GetMapping("/summary")
    public CostSummary summary(
        @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        return aggregationService.summarize(
            authenticatedUser.tenantId(),
            LocalDate.now()
        );
    }

    @GetMapping("/daily")
    public List<DailyCostSummary> daily(
        @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
        @RequestParam(required = false) LocalDate start,
        @RequestParam(required = false) LocalDate end
    ) {
        LocalDate resolvedEnd = end != null ? end : LocalDate.now();
        LocalDate resolvedStart = start != null
            ? start
            : resolvedEnd.minusDays(29);
        validateDateRange(resolvedStart, resolvedEnd);
        return aggregationService.dailyHistory(
            authenticatedUser.tenantId(),
            resolvedStart,
            resolvedEnd
        );
    }

    @GetMapping("/services")
    public List<ServiceCostSummary> services(
        @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        LocalDate today = LocalDate.now();
        return aggregationService.serviceBreakdown(
            authenticatedUser.tenantId(),
            YearMonth.from(today).atDay(1),
            today
        );
    }

    @GetMapping("/insights")
    public List<CostInsight> insights(
        @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        return insightService.generate(
            authenticatedUser.tenantId(),
            LocalDate.now()
        );
    }

    @GetMapping("/anomaly")
    public CostAnomalyAssessment anomaly(
        @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        return anomalyDetector.assess(
            authenticatedUser.tenantId(),
            LocalDate.now()
        );
    }

    @GetMapping("/budget")
    public BudgetSummary budget(
        @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        return budgetService.getBudgetSummary(
            authenticatedUser.tenantId(),
            LocalDate.now()
        );
    }

    @PutMapping("/budget")
    @PreAuthorize("hasRole('OWNER')")
    public BudgetSummary updateBudget(
        @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
        @Valid @RequestBody UpdateBudgetRequest request
    ) {
        return budgetService.updateBudget(
            authenticatedUser.tenantId(),
            request.monthlyBudget(),
            LocalDate.now()
        );
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "start date must be on or before end date"
            );
        }
        long requestedDays = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        if (requestedDays > MAX_HISTORY_DAYS) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "date range must not exceed 366 days"
            );
        }
    }
}