package com.cloudpilot.backend.events;

public record MemberRemovedEventData(

        Long membershipId,
        Long removedUserId,
        String removedUserEmail,
        String role

) {

    public MemberRemovedEventData {

        if (membershipId == null) {
            throw new IllegalArgumentException(
                    "membershipId cannot be null"
            );
        }

        if (removedUserId == null) {
            throw new IllegalArgumentException(
                    "removedUserId cannot be null"
            );
        }

        if (removedUserEmail == null
                || removedUserEmail.isBlank()) {

            throw new IllegalArgumentException(
                    "removedUserEmail cannot be blank"
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