package com.cloudpilot.backend.tasks;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDateTime;

public record CreateTaskRequest(

        @NotBlank
        String title,

        String description,

        @Pattern(
                regexp = "(?i)LOW|MEDIUM|HIGH",
                message = "must be LOW, MEDIUM, or HIGH"
        )
        String priority,

        LocalDateTime dueDate

) {
}