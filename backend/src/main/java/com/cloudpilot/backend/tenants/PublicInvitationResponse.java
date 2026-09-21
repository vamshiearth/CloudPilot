package com.cloudpilot.backend.tenants;

import java.time.LocalDateTime;

public record PublicInvitationResponse(
        String email,
        String organizationName,
        String role,
        String status,
        LocalDateTime expiresAt
) {

    public static PublicInvitationResponse from(
            TenantInvitation invitation) {

        return new PublicInvitationResponse(
                invitation.getEmail(),
                invitation.getTenant().getName(),
                invitation.getRole().getName(),
                invitation.getStatus(),
                invitation.getExpiresAt()
        );
    }
}