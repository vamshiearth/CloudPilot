package com.cloudpilot.audit.events;

public record SubscriptionChangedEventData(String previousPlan, String newPlan) {
}
