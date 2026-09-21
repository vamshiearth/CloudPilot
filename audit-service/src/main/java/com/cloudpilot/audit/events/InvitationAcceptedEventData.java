package com.cloudpilot.audit.events;

public record InvitationAcceptedEventData(
        Long invitationId,
        Long membershipId,
        Long acceptedUserId,
        String email,
        String role
) {
}
