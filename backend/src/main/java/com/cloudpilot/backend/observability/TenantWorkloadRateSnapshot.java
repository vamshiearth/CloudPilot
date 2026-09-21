package com.cloudpilot.backend.observability;

public record TenantWorkloadRateSnapshot(
        long tenantId,
        int windowSeconds,
        long requestCount,
        double requestsPerMinute,
        long clientErrorCount,
        long serverErrorCount,
        double errorRatePercent,
        double averageDurationMs,
        double maxDurationMs
) {
}
