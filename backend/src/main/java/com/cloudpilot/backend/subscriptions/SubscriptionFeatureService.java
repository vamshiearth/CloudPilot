package com.cloudpilot.backend.subscriptions;

import com.cloudpilot.backend.exception.FeatureNotAvailableException;
import com.cloudpilot.backend.tenants.Tenant;
import com.cloudpilot.backend.tenants.TenantRepository;
import org.springframework.stereotype.Service;

@Service
public class SubscriptionFeatureService {

    private final TenantRepository tenantRepository;

    public SubscriptionFeatureService(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    public boolean hasFeature(Long tenantId, SubscriptionFeatureName featureName) {
        Tenant tenant = tenantRepository.findOneById(tenantId)
                .orElseThrow(() -> new IllegalStateException("Tenant not found"));

        SubscriptionPlan plan = tenant.getPlan();
        return plan != null && plan.getFeatures().stream()
                .anyMatch(feature -> featureName.name().equals(feature.getName()));
    }

    public void requireFeature(Long tenantId, SubscriptionFeatureName featureName) {
        if (!hasFeature(tenantId, featureName)) {
            throw new FeatureNotAvailableException(
                    featureName.name() + " is not available on your current subscription plan."
            );
        }
    }
}
