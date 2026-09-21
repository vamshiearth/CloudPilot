package com.cloudpilot.backend.observability;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;

@Component
@SuppressWarnings("null")
public class TenantWorkloadTracker {

    private static final long ROLLING_WINDOW_NANOS =
            Duration.ofSeconds(60).toNanos();

    private final ConcurrentHashMap<Long, TenantCounters> counters =
            new ConcurrentHashMap<>();

    public void record(long tenantId, long durationNanos, int statusCode) {
        TenantCounters tenantCounters = counters.computeIfAbsent(
                tenantId,
                ignored -> new TenantCounters()
        );

        tenantCounters.requestCount.increment();
        tenantCounters.totalDurationNanos.add(durationNanos);
        tenantCounters.maxDurationNanos.accumulateAndGet(durationNanos, Math::max);

        if (statusCode >= 500) {
            tenantCounters.serverErrorCount.increment();
        } else if (statusCode >= 400) {
            tenantCounters.clientErrorCount.increment();
        }

        tenantCounters.lastSeenEpochMillis.set(System.currentTimeMillis());

        long now = System.nanoTime();
        tenantCounters.recentRequests.addLast(
            new RequestSample(now, durationNanos, statusCode)
        );
        pruneExpiredSamples(tenantCounters, now);
    }

    public TenantWorkloadSnapshot snapshot(long tenantId) {
        TenantCounters tenantCounters = counters.get(tenantId);
        if (tenantCounters == null) {
            return new TenantWorkloadSnapshot(tenantId, 0, 0, 0, 0, 0, 0);
        }

        return toSnapshot(tenantId, tenantCounters);
    }

    public Map<Long, TenantWorkloadSnapshot> snapshotAll() {
        return counters.entrySet()
                .stream()
                .collect(Collectors.toUnmodifiableMap(
                        Map.Entry::getKey,
                        entry -> toSnapshot(entry.getKey(), entry.getValue())
                ));
    }

    public TenantWorkloadRateSnapshot rollingSnapshot(long tenantId) {
        TenantCounters tenantCounters = counters.get(tenantId);
        if (tenantCounters == null) {
            return emptyRollingSnapshot(tenantId);
        }

        long now = System.nanoTime();
        pruneExpiredSamples(tenantCounters, now);
        long cutoff = now - ROLLING_WINDOW_NANOS;
        long requestCount = 0;
        long clientErrors = 0;
        long serverErrors = 0;
        long totalDurationNanos = 0;
        long maxDurationNanos = 0;

        for (RequestSample sample : tenantCounters.recentRequests) {
            if (sample.timestampNanos() < cutoff) {
                continue;
            }

            requestCount++;
            totalDurationNanos += sample.durationNanos();
            maxDurationNanos = Math.max(maxDurationNanos, sample.durationNanos());

            if (sample.statusCode() >= 500) {
                serverErrors++;
            } else if (sample.statusCode() >= 400) {
                clientErrors++;
            }
        }

        double averageDurationMs = requestCount == 0
                ? 0.0
                : (totalDurationNanos / (double) requestCount) / 1_000_000.0;
        double maxDurationMs = maxDurationNanos / 1_000_000.0;
        double errorRatePercent = requestCount == 0
                ? 0.0
                : ((clientErrors + serverErrors) * 100.0) / requestCount;

        return new TenantWorkloadRateSnapshot(
                tenantId,
                60,
                requestCount,
                requestCount,
                clientErrors,
                serverErrors,
                errorRatePercent,
                averageDurationMs,
                maxDurationMs
        );
    }

    public Map<Long, TenantWorkloadRateSnapshot> rollingSnapshotAll() {
        return counters.keySet()
                .stream()
                .collect(Collectors.toUnmodifiableMap(
                        tenantId -> tenantId,
                        this::rollingSnapshot
                ));
    }

    private TenantWorkloadRateSnapshot emptyRollingSnapshot(long tenantId) {
        return new TenantWorkloadRateSnapshot(
                tenantId, 60, 0, 0.0, 0, 0, 0.0, 0.0, 0.0
        );
    }

    private void pruneExpiredSamples(TenantCounters counters, long nowNanos) {
        long cutoff = nowNanos - ROLLING_WINDOW_NANOS;
        while (true) {
            RequestSample oldest = counters.recentRequests.peekFirst();
            if (oldest == null || oldest.timestampNanos() >= cutoff) {
                return;
            }
            counters.recentRequests.pollFirst();
        }
    }

    private TenantWorkloadSnapshot toSnapshot(long tenantId, TenantCounters counters) {
        return new TenantWorkloadSnapshot(
                tenantId,
                counters.requestCount.sum(),
                counters.clientErrorCount.sum(),
                counters.serverErrorCount.sum(),
                counters.totalDurationNanos.sum(),
                counters.maxDurationNanos.get(),
                counters.lastSeenEpochMillis.get()
        );
    }

    private static final class TenantCounters {
        private final LongAdder requestCount = new LongAdder();
        private final LongAdder clientErrorCount = new LongAdder();
        private final LongAdder serverErrorCount = new LongAdder();
        private final LongAdder totalDurationNanos = new LongAdder();
        private final AtomicLong maxDurationNanos = new AtomicLong();
        private final AtomicLong lastSeenEpochMillis = new AtomicLong();
        private final ConcurrentLinkedDeque<RequestSample> recentRequests =
            new ConcurrentLinkedDeque<>();
    }

        private record RequestSample(
            long timestampNanos,
            long durationNanos,
            int statusCode
        ) {
        }
}
