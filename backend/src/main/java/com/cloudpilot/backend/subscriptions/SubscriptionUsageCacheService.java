package com.cloudpilot.backend.subscriptions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;

@Service
public class SubscriptionUsageCacheService {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionUsageCacheService.class);
    private static final String KEY_PREFIX = "cloudpilot:tenant:";
    private static final String KEY_SUFFIX = ":subscription-usage";
    private static final Duration CACHE_TTL = Duration.ofMinutes(5);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final RedisFallbackMetrics redisFallbackMetrics;

    public SubscriptionUsageCacheService(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            RedisFallbackMetrics redisFallbackMetrics) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.redisFallbackMetrics = redisFallbackMetrics;
    }

    public SubscriptionUsageResponse get(Long tenantId) {

        String key = buildKey(tenantId);

        try {
            String cachedJson = redisTemplate.opsForValue().get(key);

            if (cachedJson == null) {
                return null;
            }

            try {
                return objectMapper.readValue(cachedJson, SubscriptionUsageResponse.class);
            } catch (JacksonException exception) {
                log.warn(
                        "Invalid subscription cache for tenant {}. Falling back to PostgreSQL.",
                        tenantId
                );

                try {
                    redisTemplate.delete(key);
                } catch (DataAccessException deleteException) {
                    log.warn(
                            "Could not delete invalid Redis cache for tenant {}.",
                            tenantId
                    );
                }

                return null;
            }
        } catch (DataAccessException exception) {
            redisFallbackMetrics.recordSubscriptionUsageFallback();
            log.warn(
                    "Redis unavailable while reading subscription usage for tenant {}. Falling back to PostgreSQL.",
                    tenantId
            );

            return null;
        }
    }

    public void put(Long tenantId, SubscriptionUsageResponse usage) {

        try {
            String json = objectMapper.writeValueAsString(usage);

            try {
                redisTemplate.opsForValue().set(buildKey(tenantId), json, CACHE_TTL);
            } catch (DataAccessException exception) {
                log.warn(
                        "Redis unavailable while caching subscription usage for tenant {}. Response will not be cached.",
                        tenantId
                );
            }
        } catch (JacksonException exception) {
            log.warn(
                    "Could not serialize subscription usage for tenant {}. Response will not be cached.",
                    tenantId
            );
        }
    }

    public void evict(Long tenantId) {
        if (TransactionSynchronizationManager.isActualTransactionActive()
                && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    evictNow(tenantId);
                }
            });
            return;
        }
        evictNow(tenantId);
    }

    private void evictNow(Long tenantId) {
        try {
            redisTemplate.delete(buildKey(tenantId));
            log.debug(
                    "Redis subscription usage cache evicted for tenant {}",
                    tenantId
            );
        } catch (DataAccessException exception) {
            log.warn(
                    "Redis unavailable while evicting subscription cache for tenant {}. Continuing without cache invalidation.",
                    tenantId
            );
        }
    }

    private String buildKey(Long tenantId) {
        return KEY_PREFIX + tenantId + KEY_SUFFIX;
    }
}
