package com.cloudpilot.backend.subscriptions;

import com.cloudpilot.backend.auth.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tenants/current/subscription")
public class SubscriptionController {

    private final SubscriptionUsageService subscriptionUsageService;
    private final SubscriptionManagementService subscriptionManagementService;

    public SubscriptionController(
            SubscriptionUsageService subscriptionUsageService,
            SubscriptionManagementService subscriptionManagementService) {

        this.subscriptionUsageService = subscriptionUsageService;
        this.subscriptionManagementService = subscriptionManagementService;
    }

    @GetMapping
    public ResponseEntity<SubscriptionUsageResponse> getCurrentSubscription(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {

        SubscriptionUsageResponse response = subscriptionUsageService.getUsage(
                authenticatedUser.tenantId()
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/plans")
    public ResponseEntity<List<SubscriptionPlanResponse>> getAvailablePlans() {
        return ResponseEntity.ok(
                subscriptionManagementService.getAvailablePlans()
        );
    }

    @PutMapping("/plan")
    public ResponseEntity<SubscriptionUsageResponse> changePlan(
            @Valid @RequestBody ChangeSubscriptionPlanRequest request,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {

        SubscriptionUsageResponse response =
                subscriptionManagementService.changePlan(
                        authenticatedUser.tenantId(),
                    authenticatedUser.user().getId(),
                        request.plan()
                );

        return ResponseEntity.ok(response);
    }
}
