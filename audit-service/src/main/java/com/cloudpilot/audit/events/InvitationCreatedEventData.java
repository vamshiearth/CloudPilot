package com.cloudpilot.audit.events;

public record InvitationCreatedEventData(String invitationId, String invitedEmail, String role) {
}
