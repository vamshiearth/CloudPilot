package com.cloudpilot.backend.events;

public record InvitationCreatedEventData(

        Long invitationId,
        String invitedEmail,
        String role

) {

    public InvitationCreatedEventData {

        if (invitationId == null) {
            throw new IllegalArgumentException(
                    "invitationId cannot be null"
            );
        }

        if (invitedEmail == null
                || invitedEmail.isBlank()) {

            throw new IllegalArgumentException(
                    "invitedEmail cannot be blank"
            );
        }

        if (role == null
                || role.isBlank()) {

            throw new IllegalArgumentException(
                    "role cannot be blank"
            );
        }
    }
}