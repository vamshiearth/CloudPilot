package com.cloudpilot.backend.auth;

public record LoginResponse(
        String token,
        Long id,
        String email,
        String firstName,
        String lastName,
        String status,
        Long tenantId,
        String tenantName
) {
}