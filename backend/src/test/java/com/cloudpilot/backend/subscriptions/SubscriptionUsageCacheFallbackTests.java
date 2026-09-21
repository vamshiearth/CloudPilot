package com.cloudpilot.backend.subscriptions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
class SubscriptionUsageCacheFallbackTests {

    private static final Long TENANT_ID = 42L;
    private static final String KEY = "cloudpilot:tenant:42:subscription-usage";

    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private ObjectMapper objectMapper;
    private SubscriptionUsageCacheService cacheService;
    private SubscriptionUsageResponse usage;

    @BeforeEach
    void prepare() {
        redisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        objectMapper = mock(ObjectMapper.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        cacheService = new SubscriptionUsageCacheService(
            redisTemplate, objectMapper, mock(RedisFallbackMetrics.class));
        usage = new SubscriptionUsageResponse(
                "STARTER", "Starter", 2, 20, 3, 1, 4, 10,
                false, false, Set.of("ADVANCED_RBAC")
        );
    }

    @Test
    void unavailableRedisReadFallsBackToCacheMiss() {
        when(valueOperations.get(KEY)).thenThrow(redisFailure());

        assertNull(cacheService.get(TENANT_ID));
    }

    @Test
    void unavailableRedisWriteDoesNotFailTheResponse() throws JacksonException {
        when(objectMapper.writeValueAsString(usage)).thenReturn("{\"planName\":\"STARTER\"}");
        org.mockito.Mockito.doThrow(redisFailure())
                .when(valueOperations).set(eq(KEY), anyString(), eq(Duration.ofMinutes(5)));

        assertDoesNotThrow(() -> cacheService.put(TENANT_ID, usage));
    }

    @Test
    void unavailableRedisEvictionDoesNotFailTheBusinessOperation() {
        when(redisTemplate.delete(KEY)).thenThrow(redisFailure());

        assertDoesNotThrow(() -> cacheService.evict(TENANT_ID));
    }

    @Test
    void corruptCachedJsonFallsBackAndAttemptsCleanup() throws JacksonException {
        when(valueOperations.get(KEY)).thenReturn("not-json");
        when(objectMapper.readValue("not-json", SubscriptionUsageResponse.class))
                .thenThrow(mock(JacksonException.class));

        assertNull(cacheService.get(TENANT_ID));
        verify(redisTemplate).delete(KEY);
    }

    @Test
    void serializationFailureDoesNotTouchRedis() throws JacksonException {
        when(objectMapper.writeValueAsString(usage)).thenThrow(mock(JacksonException.class));

        assertDoesNotThrow(() -> cacheService.put(TENANT_ID, usage));
        verify(valueOperations, never()).set(anyString(), anyString(), any(Duration.class));
    }

    private DataAccessResourceFailureException redisFailure() {
        return new DataAccessResourceFailureException("Redis unavailable");
    }
}
