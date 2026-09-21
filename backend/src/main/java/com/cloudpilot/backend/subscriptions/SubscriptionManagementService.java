package com.cloudpilot.backend.subscriptions;

import com.cloudpilot.backend.events.CloudPilotEvent;
import com.cloudpilot.backend.events.CloudPilotEventProducer;
import com.cloudpilot.backend.events.CloudPilotEventType;
import com.cloudpilot.backend.events.SubscriptionChangedEventData;
import com.cloudpilot.backend.tenants.Tenant;
import com.cloudpilot.backend.tenants.TenantRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionOperations;

import java.util.List;

@Service
@SuppressWarnings("null")
public class SubscriptionManagementService {

    private final SubscriptionPlanRepository planRepository;
    private final TenantRepository tenantRepository;
    private final SubscriptionUsageService usageService;
        private final SubscriptionUsageCacheService cacheService;
        private final TransactionOperations transactionOperations;
        private final CloudPilotEventProducer eventProducer;

    public SubscriptionManagementService(
            SubscriptionPlanRepository planRepository,
            TenantRepository tenantRepository,
            SubscriptionUsageService usageService,
            SubscriptionUsageCacheService cacheService,
            TransactionOperations transactionOperations,
            CloudPilotEventProducer eventProducer) {

        this.planRepository = planRepository;
        this.tenantRepository = tenantRepository;
        this.usageService = usageService;
        this.cacheService = cacheService;
        this.transactionOperations = transactionOperations;
        this.eventProducer = eventProducer;
    }

    @PreAuthorize("hasAuthority('BILLING_READ')")
    public List<SubscriptionPlanResponse> getAvailablePlans() {
        return planRepository
                .findAllByActiveTrueOrderByIdAsc()
                .stream()
                .map(SubscriptionPlanResponse::from)
                .toList();
    }

    @PreAuthorize("hasAuthority('BILLING_UPDATE')")
    public SubscriptionUsageResponse changePlan(
            Long tenantId,
            Long actorUserId,
            String requestedPlan) {

        String planName = requestedPlan.trim().toUpperCase();
        SubscriptionChangedEventData eventData =
                transactionOperations.execute(status -> {
            SubscriptionPlan targetPlan = planRepository
                    .findByName(planName)
                    .filter(SubscriptionPlan::isActive)
                    .orElseThrow(() ->
                            new IllegalArgumentException(
                                    "Active subscription plan not found: " + planName
                            )
                    );

            Tenant tenant = tenantRepository.findOneById(tenantId)
                    .orElseThrow(() -> new IllegalStateException("Tenant not found"));

                SubscriptionPlan currentPlan = tenant.getPlan();
                if (currentPlan == null) {
                throw new IllegalStateException(
                    "Tenant does not have a subscription plan"
                );
                }

                String previousPlan = currentPlan.getName();
                String newPlan = targetPlan.getName();

                if (previousPlan.equals(newPlan)) {
                return null;
                }

            tenant.setPlan(targetPlan);
            tenantRepository.save(tenant);

                return new SubscriptionChangedEventData(
                    previousPlan,
                    newPlan
                );
        });

            if (eventData == null) {
                return usageService.getUsage(tenantId);
            }

        cacheService.evict(tenantId);

            CloudPilotEvent event =
                CloudPilotEvent.create(
                    CloudPilotEventType.SUBSCRIPTION_CHANGED,
                    tenantId,
                    actorUserId,
                    eventData
                );

            eventProducer.publish(event);

        return usageService.getUsage(tenantId);
    }
}