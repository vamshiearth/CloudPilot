package com.cloudpilot.backend.tenants;

import com.cloudpilot.backend.auth.AuthenticatedUser;
import com.cloudpilot.backend.rbac.TenantRoleResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tenants/current")
public class TenantMemberController {

    private final TenantMemberService tenantMemberService;

    public TenantMemberController(
            TenantMemberService tenantMemberService) {
        this.tenantMemberService = tenantMemberService;
    }

    @GetMapping("/members")
    public ResponseEntity<List<TenantMemberResponse>> getMembers(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {

        return ResponseEntity.ok(
                tenantMemberService.getMembers(
                        authenticatedUser.tenantId()
                )
        );
    }

    @GetMapping("/roles")
    public ResponseEntity<List<TenantRoleResponse>> getRoles(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {

        return ResponseEntity.ok(
                tenantMemberService.getRoles(
                        authenticatedUser.tenantId()
                )
        );
    }

    @DeleteMapping("/members/{membershipId}")
    public ResponseEntity<Void> removeMember(
            @PathVariable Long membershipId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        tenantMemberService.removeMember(membershipId, authenticatedUser);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/members/{membershipId}/role")
    public ResponseEntity<TenantMemberResponse> updateMemberRole(
            @PathVariable Long membershipId,
            @Valid @RequestBody UpdateMemberRoleRequest request,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {

        return ResponseEntity.ok(
                tenantMemberService.updateRole(
                        membershipId,
                        request.role(),
                        authenticatedUser
                )
        );
    }
}
