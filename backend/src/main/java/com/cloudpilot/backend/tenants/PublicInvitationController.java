package com.cloudpilot.backend.tenants;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/invitations")
public class PublicInvitationController {

    private final TenantInvitationService invitationService;

    public PublicInvitationController(
            TenantInvitationService invitationService) {
        this.invitationService = invitationService;
    }

    @GetMapping("/{token}")
    public ResponseEntity<PublicInvitationResponse> getInvitation(
            @PathVariable String token) {
        return ResponseEntity.ok(invitationService.getInvitation(token));
    }

    @PostMapping("/{token}/accept")
    public ResponseEntity<Void> acceptInvitation(
            @PathVariable String token,
            @Valid @RequestBody AcceptInvitationRequest request) {
        invitationService.acceptInvitation(token, request);
        return ResponseEntity.noContent().build();
    }
}