package com.cloudpilot.backend.tasks;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDateTime;

public record UpdateTaskRequest(

        @NotBlank
        String title,

        String description,

        @NotBlank
        @Pattern(
                regexp = "(?i)TODO|IN_PROGRESS|DONE",
                message = "must be TODO, IN_PROGRESS, or DONE"
        )
        String status,

        @Pattern(
                regexp = "(?i)LOW|MEDIUM|HIGH",
                message = "must be LOW, MEDIUM, or HIGH"
        )
        String priority,

        LocalDateTime dueDate

) {
}