package com.cloudpilot.backend.subscriptions;

import com.cloudpilot.backend.events.CloudPilotEventProducer;
import com.cloudpilot.backend.tenants.Tenant;
import com.cloudpilot.backend.tenants.TenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.support.TransactionOperations;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SubscriptionManagementServiceTests {

    private SubscriptionPlanRepository plans;
    private TenantRepository tenants;
    private SubscriptionUsageService usage;
    private SubscriptionUsageCacheService cache;
    private TransactionOperations transactions;
    private CloudPilotEventProducer eventProducer;
    private SubscriptionManagementService service;

    @BeforeEach
    void prepare() {
        plans = mock(SubscriptionPlanRepository.class);
        tenants = mock(TenantRepository.class);
        usage = mock(SubscriptionUsageService.class);
        cache = mock(SubscriptionUsageCacheService.class);
        transactions = mock(TransactionOperations.class);
        eventProducer = mock(CloudPilotEventProducer.class);
        doAnswer(invocation -> {
            return invocation.<java.util.function.Function<TransactionStatus, ?> >getArgument(0)
                .apply(mock(TransactionStatus.class));
        }).when(transactions).execute(any());
        service = new SubscriptionManagementService(plans, tenants, usage, cache, transactions, eventProducer);
    }

    @Test
    void committedPlanChangeEvictsThenReturnsFreshUsage() {
        Long tenantId = 1L;
        SubscriptionPlan starter = SubscriptionPlan.builder()
                .name("STARTER")
                .displayName("Starter")
                .maxProjects(20)
                .maxMembers(10)
                .active(true)
                .features(Set.of(SubscriptionFeature.builder().name("ADVANCED_RBAC").build()))
                .build();
        Tenant tenant = Tenant.builder().id(tenantId).build();
        SubscriptionUsageResponse refreshed = new SubscriptionUsageResponse(
                "STARTER", "Starter", 3, 20, 2, 0, 2, 10,
                false, false, Set.of("ADVANCED_RBAC"));
        when(plans.findByName("STARTER")).thenReturn(Optional.of(starter));
        when(tenants.findOneById(tenantId)).thenReturn(Optional.of(tenant));
        when(usage.getUsage(tenantId)).thenReturn(refreshed);

        SubscriptionUsageResponse result = service.changePlan(tenantId, 2L, "starter");

        assertSame(refreshed, result);
        assertSame(starter, tenant.getPlan());
        verify(tenants).save(tenant);
        var order = inOrder(transactions, cache, usage);
        order.verify(transactions).executeWithoutResult(any());
        order.verify(cache).evict(tenantId);
        order.verify(usage).getUsage(tenantId);
    }

    @Test
    void invalidPlanKeepsCacheAndSkipsUsageRefresh() {
        when(plans.findByName("ENTERPRISE")).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.changePlan(1L, 2L, "ENTERPRISE")
        );

        assertEquals("Active subscription plan not found: ENTERPRISE", exception.getMessage());
        verifyNoInteractions(cache, usage, tenants);
    }

    @Test
    void failedSaveKeepsCacheAndSkipsUsageRefresh() {
        Long tenantId = 1L;
        SubscriptionPlan starter = SubscriptionPlan.builder().name("STARTER").active(true).build();
        Tenant tenant = Tenant.builder().id(tenantId).build();
        when(plans.findByName("STARTER")).thenReturn(Optional.of(starter));
        when(tenants.findOneById(tenantId)).thenReturn(Optional.of(tenant));
        doThrow(new IllegalStateException("Write failed")).when(tenants).save(tenant);

        assertThrows(IllegalStateException.class, () -> service.changePlan(tenantId, 2L, "STARTER"));

        verify(cache, never()).evict(any());
        verifyNoInteractions(usage);
    }

    @Test
    void failedCommitKeepsCacheAndSkipsUsageRefresh() {
        doThrow(new TransactionSystemException("Commit failed"))
                .when(transactions).executeWithoutResult(any());

        assertThrows(TransactionSystemException.class, () -> service.changePlan(1L, 2L, "STARTER"));

        verify(cache, never()).evict(any());
        verifyNoInteractions(usage);
    }
}
