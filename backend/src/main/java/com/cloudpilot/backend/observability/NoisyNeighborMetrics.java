package com.cloudpilot.backend.observability;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@SuppressWarnings("null")
public class NoisyNeighborMetrics {

    private final TenantNoisyNeighborDetector detector;

    public NoisyNeighborMetrics(
            MeterRegistry meterRegistry,
            TenantNoisyNeighborDetector detector) {
        this.detector = detector;

        Gauge.builder(
                        "cloudpilot.tenant.observability.active.tenants",
                        this,
                        metrics -> metrics.activeTenantCount())
                .description("Number of tenants active in the rolling workload window")
                .register(meterRegistry);

        Gauge.builder(
                        "cloudpilot.tenant.observability.noisy.candidates",
                        this,
                        metrics -> metrics.noisyCandidateCount())
                .description("Number of tenants currently classified as noisy-neighbor candidates")
                .register(meterRegistry);

        Gauge.builder(
                        "cloudpilot.tenant.observability.elevated.tenants",
                        this,
                        metrics -> metrics.elevatedTenantCount())
                .description("Number of tenants currently classified as elevated")
                .register(meterRegistry);

        Gauge.builder(
                        "cloudpilot.tenant.observability.max.request.pressure",
                        this,
                        metrics -> metrics.maxRequestPressure())
                .description("Highest request-pressure ratio among active tenants")
                .register(meterRegistry);

        Gauge.builder(
                        "cloudpilot.tenant.observability.max.score",
                        this,
                        metrics -> metrics.maxScore())
                .description("Highest noisy-neighbor score among active tenants")
                .register(meterRegistry);

        Gauge.builder(
                        "cloudpilot.tenant.observability.sufficient.sample",
                        this,
                        metrics -> metrics.hasSufficientSample() ? 1.0 : 0.0)
                .description("Whether current tenant workload has sufficient data for noisy-neighbor classification")
                .register(meterRegistry);
    }

    private List<TenantWorkloadAssessment> assessments() {
        return detector.assessAll();
    }

    private double activeTenantCount() {
        return assessments().size();
    }

    private double noisyCandidateCount() {
        return assessments().stream()
                .filter(assessment -> assessment.classification()
                        == TenantWorkloadClassification.NOISY_CANDIDATE)
                .count();
    }

    private double elevatedTenantCount() {
        return assessments().stream()
                .filter(assessment -> assessment.classification()
                        == TenantWorkloadClassification.ELEVATED)
                .count();
    }

    private double maxRequestPressure() {
        return assessments().stream()
                .mapToDouble(TenantWorkloadAssessment::requestPressure)
                .max()
                .orElse(0.0);
    }

    private double maxScore() {
        return assessments().stream()
                .mapToDouble(TenantWorkloadAssessment::noisyNeighborScore)
                .max()
                .orElse(0.0);
    }

    private boolean hasSufficientSample() {
        return assessments().stream()
                .anyMatch(TenantWorkloadAssessment::sufficientSample);
    }
}
