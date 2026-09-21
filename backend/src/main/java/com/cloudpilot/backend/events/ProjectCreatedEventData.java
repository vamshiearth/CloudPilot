package com.cloudpilot.backend.events;

public record ProjectCreatedEventData(

        Long projectId,

        String projectName

) {

    public ProjectCreatedEventData {

        if (projectId == null) {
            throw new IllegalArgumentException(
                    "projectId cannot be null"
            );
        }

        if (projectName == null
                || projectName.isBlank()) {

            throw new IllegalArgumentException(
                    "projectName cannot be blank"
            );
        }
    }
}