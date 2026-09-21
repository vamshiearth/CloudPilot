package com.cloudpilot.backend.auth;

import com.cloudpilot.backend.users.User;
import com.cloudpilot.backend.tenants.Tenant;

public record UserResponse(
        Long id,
        String email,
        String firstName,
        String lastName,
        String status,
        Long tenantId,
        String tenantName
) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getStatus(),
                null,
                null
        );
    }

    public static UserResponse from(User user, Tenant tenant) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getStatus(),
                tenant.getId(),
                tenant.getName()
        );
    }
}
