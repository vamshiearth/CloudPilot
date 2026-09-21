package com.cloudpilot.backend.events;

public record InvitationAcceptedEventData(

        Long invitationId,
        Long membershipId,
        Long acceptedUserId,
        String email,
        String role

) {

    public InvitationAcceptedEventData {

        if (invitationId == null) {
            throw new IllegalArgumentException(
                    "invitationId cannot be null"
            );
        }

        if (membershipId == null) {
            throw new IllegalArgumentException(
                    "membershipId cannot be null"
            );
        }

        if (acceptedUserId == null) {
            throw new IllegalArgumentException(
                    "acceptedUserId cannot be null"
            );
        }

        if (email == null
                || email.isBlank()) {

            throw new IllegalArgumentException(
                    "email cannot be blank"
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