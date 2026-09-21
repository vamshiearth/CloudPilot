package com.cloudpilot.backend.observability;

public record TenantWorkloadAssessment(
        long tenantId,
        int windowSeconds,
        long requestCount,
        double requestsPerMinute,
        double averageDurationMs,
        int activeTenantCount,
        long platformRequestCount,
        double platformAverageDurationMs,
        double requestSharePercent,
        double requestPressure,
        double latencyPressure,
        double noisyNeighborScore,
        TenantWorkloadClassification classification,
        boolean sufficientSample
) {
}
