package com.cloudpilot.backend.tenants;

import com.cloudpilot.backend.auth.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tenants/current/invitations")
public class TenantInvitationController {

    private final TenantInvitationService invitationService;

    public TenantInvitationController(
            TenantInvitationService invitationService) {
        this.invitationService = invitationService;
    }

    @PostMapping
        public ResponseEntity<CreateInvitationResponse> createInvitation(
            @Valid @RequestBody CreateInvitationRequest request,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {

        CreateInvitationResponse invitation = invitationService.createInvitation(
                request,
                authenticatedUser
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(invitation);
    }

    @GetMapping
    public ResponseEntity<List<InvitationResponse>> getPendingInvitations(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {

        return ResponseEntity.ok(
                invitationService.getPendingInvitations(
                        authenticatedUser.tenantId()
                )
        );
    }

    @DeleteMapping("/{invitationId}")
    public ResponseEntity<Void> revokeInvitation(
            @PathVariable Long invitationId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {

        invitationService.revokeInvitation(
                invitationId,
                authenticatedUser
        );

        return ResponseEntity.noContent().build();
    }
}