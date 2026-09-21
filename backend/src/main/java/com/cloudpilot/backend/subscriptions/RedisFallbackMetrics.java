package com.cloudpilot.backend.subscriptions;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class RedisFallbackMetrics {

    private final Counter subscriptionUsageFallbackCounter;

    public RedisFallbackMetrics(MeterRegistry meterRegistry) {
        this.subscriptionUsageFallbackCounter = Counter.builder("cloudpilot.redis.fallback")
                .description("Number of times CloudPilot fell back to PostgreSQL because Redis subscription usage lookup failed")
                .tag("cache", "subscription-usage")
                .register(meterRegistry);
    }

    public void recordSubscriptionUsageFallback() {
        subscriptionUsageFallbackCounter.increment();
    }
}
