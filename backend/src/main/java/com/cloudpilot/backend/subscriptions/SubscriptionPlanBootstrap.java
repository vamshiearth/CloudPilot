package com.cloudpilot.backend.subscriptions;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Component
@Order(1)
public class SubscriptionPlanBootstrap
        implements CommandLineRunner {

    private final SubscriptionPlanRepository planRepository;
    private final SubscriptionFeatureRepository featureRepository;

    public SubscriptionPlanBootstrap(
            SubscriptionPlanRepository planRepository,
            SubscriptionFeatureRepository featureRepository) {

        this.planRepository = planRepository;
        this.featureRepository = featureRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {

        SubscriptionFeature advancedRbac = featureRepository
                .findByName(SubscriptionFeatureName.ADVANCED_RBAC.name())
                .orElseGet(() -> featureRepository.save(
                        SubscriptionFeature.builder()
                                .name(SubscriptionFeatureName.ADVANCED_RBAC.name())
                                .displayName("Advanced RBAC")
                                .description("Allows organization administrators to change member roles.")
                                .build()
                ));

        provisionPlan(
                SubscriptionPlanName.FREE,
                "Free",
                "Basic CloudPilot plan",
                3,
                3,
                Set.of()
        );

        provisionPlan(
                SubscriptionPlanName.STARTER,
                "Starter",
                "Plan for growing teams",
                10,
                20,
                Set.of(advancedRbac)
        );

        provisionPlan(
                SubscriptionPlanName.PRO,
                "Pro",
                "Advanced CloudPilot plan",
                50,
                100,
                Set.of(advancedRbac)
        );

        System.out.println(
                "Default CloudPilot subscription plans provisioned."
        );
    }

    private void provisionPlan(
            SubscriptionPlanName planName,
            String displayName,
            String description,
            int maxMembers,
            int maxProjects,
            Set<SubscriptionFeature> features) {

        SubscriptionPlan plan = planRepository
                .findByName(planName.name())
                .orElseGet(() ->
                        SubscriptionPlan.builder()
                                .name(planName.name())
                                .build()
                );

        plan.setDisplayName(displayName);
        plan.setDescription(description);
        plan.setActive(true);
        plan.setMaxMembers(maxMembers);
        plan.setMaxProjects(maxProjects);
        plan.setFeatures(new HashSet<>(features));

        planRepository.save(plan);
    }
}
