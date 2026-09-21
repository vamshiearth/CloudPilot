package com.cloudpilot.backend.projects;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateProjectRequest(

        @NotBlank
        String name,

        String description,

        @NotBlank
        @Pattern(
                regexp = "(?i)ACTIVE|COMPLETED|ARCHIVED",
                message = "must be ACTIVE, COMPLETED, or ARCHIVED"
        )
        String status

) {
}