package com.cloudpilot.backend.projects;

import java.time.LocalDateTime;

public record ProjectResponse(
        Long id,
        String name,
        String description,
        String status,
        Long createdBy,
        String createdByEmail,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static ProjectResponse from(Project project) {

        return new ProjectResponse(
                project.getId(),
                project.getName(),
                project.getDescription(),
                project.getStatus(),
                project.getCreatedBy().getId(),
                project.getCreatedBy().getEmail(),
                project.getCreatedAt(),
                project.getUpdatedAt()
        );
    }
}
