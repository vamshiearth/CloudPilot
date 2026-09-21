package com.cloudpilot.backend.observability;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@SuppressWarnings("null")
public class TenantNoisyNeighborDetector {

    private static final long MINIMUM_PLATFORM_REQUESTS = 10;
    private static final int MINIMUM_ACTIVE_TENANTS = 2;
    private static final double ELEVATED_SCORE = 1.5;
    private static final double NOISY_SCORE = 2.0;
        private static final double NOISY_REQUEST_PRESSURE = 1.75;
        private static final double MINIMUM_PRESSURE_FOR_SCORE_CLASSIFICATION = 1.0;

    private final TenantWorkloadTracker tenantWorkloadTracker;

        public Optional<TenantWorkloadAssessment> assessTenant(long tenantId) {
                return assessAll()
                                .stream()
                                .filter(assessment -> assessment.tenantId() == tenantId)
                                .findFirst();
        }

    public List<TenantWorkloadAssessment> assessAll() {
        Map<Long, TenantWorkloadRateSnapshot> snapshots =
                tenantWorkloadTracker.rollingSnapshotAll();

        List<TenantWorkloadRateSnapshot> activeTenants = snapshots.values()
                .stream()
                .filter(snapshot -> snapshot.requestCount() > 0)
                .toList();

        if (activeTenants.isEmpty()) {
            return List.of();
        }

        int activeTenantCount = activeTenants.size();
        long platformRequestCount = activeTenants.stream()
                .mapToLong(TenantWorkloadRateSnapshot::requestCount)
                .sum();
        double platformAverageDurationMs = activeTenants.stream()
                .mapToDouble(snapshot ->
                        snapshot.averageDurationMs() * snapshot.requestCount())
                .sum() / platformRequestCount;
        double averageRequestsPerTenant =
                platformRequestCount / (double) activeTenantCount;
        boolean sufficientSample = activeTenantCount >= MINIMUM_ACTIVE_TENANTS
                && platformRequestCount >= MINIMUM_PLATFORM_REQUESTS;

        return activeTenants.stream()
                .map(snapshot -> assessTenant(
                        snapshot,
                        activeTenantCount,
                        platformRequestCount,
                        platformAverageDurationMs,
                        averageRequestsPerTenant,
                        sufficientSample
                ))
                .sorted(Comparator.comparingDouble(
                        TenantWorkloadAssessment::noisyNeighborScore
                ).reversed())
                .toList();
    }

    private TenantWorkloadAssessment assessTenant(
            TenantWorkloadRateSnapshot tenant,
            int activeTenantCount,
            long platformRequestCount,
            double platformAverageDurationMs,
            double averageRequestsPerTenant,
            boolean sufficientSample) {
        double requestPressure = tenant.requestCount()
                / averageRequestsPerTenant;
        double requestSharePercent =
                (tenant.requestCount() * 100.0) / platformRequestCount;
        double latencyPressure = platformAverageDurationMs == 0.0
                ? 0.0
                : tenant.averageDurationMs() / platformAverageDurationMs;
        double score = (requestPressure * 0.70) + (latencyPressure * 0.30);

        return new TenantWorkloadAssessment(
                tenant.tenantId(),
                tenant.windowSeconds(),
                tenant.requestCount(),
                tenant.requestsPerMinute(),
                tenant.averageDurationMs(),
                activeTenantCount,
                platformRequestCount,
                platformAverageDurationMs,
                requestSharePercent,
                requestPressure,
                latencyPressure,
                score,
                classify(score, requestPressure, sufficientSample),
                sufficientSample
        );
    }

    private TenantWorkloadClassification classify(
            double score,
            double requestPressure,
            boolean sufficientSample) {
        if (!sufficientSample) {
            return TenantWorkloadClassification.NORMAL;
        }
                if (requestPressure >= NOISY_REQUEST_PRESSURE) {
            return TenantWorkloadClassification.NOISY_CANDIDATE;
        }
                if (score >= NOISY_SCORE
                                && requestPressure >= MINIMUM_PRESSURE_FOR_SCORE_CLASSIFICATION) {
                        return TenantWorkloadClassification.NOISY_CANDIDATE;
                }
                if (score >= ELEVATED_SCORE
                                && requestPressure >= MINIMUM_PRESSURE_FOR_SCORE_CLASSIFICATION) {
            return TenantWorkloadClassification.ELEVATED;
        }
        return TenantWorkloadClassification.NORMAL;
    }
}
