package com.cloudpilot.backend.subscriptions;

public record SubscriptionPlanResponse(
        Long id,
        String name,
        String displayName,
        String description,
        Integer maxMembers,
        Integer maxProjects
) {

    public static SubscriptionPlanResponse from(
            SubscriptionPlan plan) {

        return new SubscriptionPlanResponse(
                plan.getId(),
                plan.getName(),
                plan.getDisplayName(),
                plan.getDescription(),
                plan.getMaxMembers(),
                plan.getMaxProjects()
        );
    }
}