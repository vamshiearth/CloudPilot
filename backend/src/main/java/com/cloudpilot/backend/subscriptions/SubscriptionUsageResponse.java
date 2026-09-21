package com.cloudpilot.backend.subscriptions;

public record SubscriptionUsageResponse(
        String planName,
        String displayName,
        long currentProjects,
        int maxProjects,
        long activeMembers,
        long pendingInvitations,
        long reservedMemberSlots,
        int maxMembers,
        boolean projectLimitReached,
        boolean memberLimitReached,
        java.util.Set<String> features
) {
}
