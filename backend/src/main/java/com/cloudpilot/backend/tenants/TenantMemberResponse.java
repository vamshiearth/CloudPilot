package com.cloudpilot.backend.tenants;

import java.time.LocalDateTime;

public record TenantMemberResponse(
        Long membershipId,
        Long userId,
        String email,
        String firstName,
        String lastName,
        String role,
        String status,
        LocalDateTime joinedAt
) {

    public static TenantMemberResponse from(
            TenantMembership membership) {

        return new TenantMemberResponse(
                membership.getId(),
                membership.getUser().getId(),
                membership.getUser().getEmail(),
                membership.getUser().getFirstName(),
                membership.getUser().getLastName(),
                membership.getRole().getName(),
                membership.getStatus(),
                membership.getJoinedAt()
        );
    }
}