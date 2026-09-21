package com.cloudpilot.backend.observability;

import com.cloudpilot.backend.auth.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tenant/workload")
@RequiredArgsConstructor
public class TenantWorkloadController {

    private final TenantWorkloadTracker tenantWorkloadTracker;
    private final TenantNoisyNeighborDetector tenantNoisyNeighborDetector;

    @GetMapping("/me")
    public TenantWorkloadSnapshot currentTenantWorkload(
            @AuthenticationPrincipal AuthenticatedUser user) {
        return tenantWorkloadTracker.snapshot(user.tenantId());
    }

    @GetMapping("/me/rate")
    public TenantWorkloadRateSnapshot currentTenantRate(
            @AuthenticationPrincipal AuthenticatedUser user) {
        return tenantWorkloadTracker.rollingSnapshot(user.tenantId());
    }

    @GetMapping("/me/assessment")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<TenantWorkloadAssessment> currentTenantAssessment(
            @AuthenticationPrincipal AuthenticatedUser user) {
        return tenantNoisyNeighborDetector
                .assessTenant(user.tenantId())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
