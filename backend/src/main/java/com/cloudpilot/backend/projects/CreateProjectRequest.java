package com.cloudpilot.backend.projects;

import jakarta.validation.constraints.NotBlank;

public record CreateProjectRequest(

        @NotBlank
        String name,

        String description

) {
}
