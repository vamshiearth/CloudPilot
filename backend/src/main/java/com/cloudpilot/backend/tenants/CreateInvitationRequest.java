package com.cloudpilot.backend.tenants;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateInvitationRequest(

        @NotBlank
        @Email
        String email,

        @NotBlank
        String role

) {
}