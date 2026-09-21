package com.cloudpilot.backend.subscriptions;

import com.cloudpilot.backend.tenants.Tenant;
import com.cloudpilot.backend.tenants.TenantRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(2)
public class ExistingTenantPlanMigration
        implements CommandLineRunner {

    private final TenantRepository tenantRepository;
    private final SubscriptionPlanRepository planRepository;

    public ExistingTenantPlanMigration(
            TenantRepository tenantRepository,
            SubscriptionPlanRepository planRepository) {

        this.tenantRepository = tenantRepository;
        this.planRepository = planRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {

        SubscriptionPlan freePlan = planRepository
                .findByName(SubscriptionPlanName.FREE.name())
                .orElseThrow(() ->
                        new IllegalStateException(
                                "FREE subscription plan not found"
                        )
                );

        int updated = 0;
        for (Tenant tenant : tenantRepository.findAll()) {
            if (tenant.getPlan() == null) {
                tenant.setPlan(freePlan);
                tenantRepository.save(tenant);
                updated++;
            }
        }

        System.out.println(
                "Tenant subscription migration complete: " +
                updated +
                " tenants assigned to FREE."
        );
    }
}