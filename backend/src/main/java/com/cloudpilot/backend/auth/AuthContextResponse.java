package com.cloudpilot.backend.auth;

import java.util.Set;

public record AuthContextResponse(
        Long userId,
        String email,
        Long tenantId,
        String role,
        Set<String> permissions
) {
}