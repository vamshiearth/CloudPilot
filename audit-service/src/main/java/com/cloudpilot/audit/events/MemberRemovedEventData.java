package com.cloudpilot.audit.events;

public record MemberRemovedEventData(
        Long membershipId,
        Long removedUserId,
        String removedUserEmail,
        String role
) {
}
