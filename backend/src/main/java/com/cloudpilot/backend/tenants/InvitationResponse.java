package com.cloudpilot.backend.tenants;

import java.time.LocalDateTime;

public record InvitationResponse(
        Long id,
        String email,
        String role,
        String status,
        String invitedBy,
        LocalDateTime expiresAt,
        LocalDateTime createdAt
) {

    public static InvitationResponse from(
            TenantInvitation invitation) {

        return new InvitationResponse(
                invitation.getId(),
                invitation.getEmail(),
                invitation.getRole().getName(),
                invitation.getStatus(),
                invitation.getInvitedBy().getEmail(),
                invitation.getExpiresAt(),
                invitation.getCreatedAt()
        );
    }
}