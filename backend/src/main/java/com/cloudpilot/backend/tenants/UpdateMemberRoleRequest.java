package com.cloudpilot.backend.tenants;

import jakarta.validation.constraints.NotBlank;

public record UpdateMemberRoleRequest(

        @NotBlank
        String role

) {
}