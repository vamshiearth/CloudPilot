package com.cloudpilot.backend.tenants;

import com.cloudpilot.backend.auth.AuthenticatedUser;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tenants")
public class TenantController {

    private final TenantService tenantService;

    public TenantController(
            TenantService tenantService) {

        this.tenantService = tenantService;
    }

    @GetMapping("/current")
    public ResponseEntity<TenantResponse> getCurrentTenant(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {

        TenantResponse tenant = tenantService.getCurrentTenant(
                authenticatedUser.tenantId()
        );

        return ResponseEntity.ok(tenant);
    }
}
