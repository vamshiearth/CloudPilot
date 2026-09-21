package com.cloudpilot.backend.observability;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TenantWorkloadSnapshot(
        long tenantId,
        long requestCount,
        long clientErrorCount,
        long serverErrorCount,
        long totalDurationNanos,
        long maxDurationNanos,
        long lastSeenEpochMillis
) {

    @JsonProperty
    public double averageDurationMs() {
        if (requestCount == 0) {
            return 0.0;
        }

        return (totalDurationNanos / (double) requestCount) / 1_000_000.0;
    }

    @JsonProperty
    public double maxDurationMs() {
        return maxDurationNanos / 1_000_000.0;
    }
}
