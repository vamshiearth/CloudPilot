package com.cloudpilot.backend.tasks;

import java.time.LocalDateTime;

public record TaskResponse(

        Long id,
        Long projectId,
        String title,
        String description,
        String status,
        String priority,
        Long assignedTo,
        Long createdBy,
        String createdByEmail,
        LocalDateTime dueDate,
        LocalDateTime createdAt,
        LocalDateTime updatedAt

) {

    public static TaskResponse from(Task task) {

        return new TaskResponse(
                task.getId(),
                task.getProject().getId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getPriority(),

                task.getAssignedTo() == null
                        ? null
                        : task.getAssignedTo().getId(),

                task.getCreatedBy().getId(),
                task.getCreatedBy().getEmail(),
                task.getDueDate(),
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }
}