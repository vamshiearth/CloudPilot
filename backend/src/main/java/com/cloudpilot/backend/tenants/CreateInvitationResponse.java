package com.cloudpilot.backend.tenants;

public record CreateInvitationResponse(
        InvitationResponse invitation,
        String invitationUrl
) {
}