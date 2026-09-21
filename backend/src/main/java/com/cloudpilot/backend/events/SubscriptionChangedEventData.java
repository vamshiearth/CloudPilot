package com.cloudpilot.backend.events;

public record SubscriptionChangedEventData(

        String previousPlan,
        String newPlan

) {

    public SubscriptionChangedEventData {

        if (previousPlan == null
                || previousPlan.isBlank()) {

            throw new IllegalArgumentException(
                    "previousPlan cannot be blank"
            );
        }

        if (newPlan == null
                || newPlan.isBlank()) {

            throw new IllegalArgumentException(
                    "newPlan cannot be blank"
            );
        }
    }
}