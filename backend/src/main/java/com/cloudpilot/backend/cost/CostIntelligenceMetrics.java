package com.cloudpilot.backend.cost;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicReference;

@Component
public class CostIntelligenceMetrics {

    private final AtomicReference<Double> tenantsWithBudget =
        new AtomicReference<>(0.0);

    private final AtomicReference<Double> overBudgetTenants =
        new AtomicReference<>(0.0);

    private final AtomicReference<Double> anomalyCandidates =
        new AtomicReference<>(0.0);

    private final AtomicReference<Double> maxCostPressure =
        new AtomicReference<>(0.0);

    private final AtomicReference<Double> maxBudgetUsage =
        new AtomicReference<>(0.0);

    public CostIntelligenceMetrics(MeterRegistry registry) {
        Gauge.builder(
                "cloudpilot_cost_tenants_with_budget",
                tenantsWithBudget,
                reference -> reference.get()
            )
            .register(registry);

        Gauge.builder(
                "cloudpilot_cost_over_budget_tenants",
                overBudgetTenants,
                reference -> reference.get()
            )
            .register(registry);

        Gauge.builder(
                "cloudpilot_cost_anomaly_candidates",
                anomalyCandidates,
                reference -> reference.get()
            )
            .register(registry);

        Gauge.builder(
                "cloudpilot_cost_max_pressure",
                maxCostPressure,
                reference -> reference.get()
            )
            .register(registry);

        Gauge.builder(
                "cloudpilot_cost_max_budget_usage_percent",
                maxBudgetUsage,
                reference -> reference.get()
            )
            .register(registry);
    }

    public void update(
        int tenantsWithBudget,
        int overBudgetTenants,
        int anomalyCandidates,
        double maxCostPressure,
        double maxBudgetUsage
    ) {
        this.tenantsWithBudget.set((double) tenantsWithBudget);
        this.overBudgetTenants.set((double) overBudgetTenants);
        this.anomalyCandidates.set((double) anomalyCandidates);
        this.maxCostPressure.set(maxCostPressure);
        this.maxBudgetUsage.set(maxBudgetUsage);
    }
}