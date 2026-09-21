package com.cloudpilot.backend.subscriptions;

import jakarta.validation.constraints.NotBlank;

public record ChangeSubscriptionPlanRequest(

        @NotBlank
        String plan

) {
}