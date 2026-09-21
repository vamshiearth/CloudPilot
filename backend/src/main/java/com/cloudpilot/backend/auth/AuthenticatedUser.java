package com.cloudpilot.backend.auth;

import com.cloudpilot.backend.users.User;

import java.util.Set;

public record AuthenticatedUser(
        User user,
                Long tenantId,
                String role,
                Set<String> permissions
) {

        public AuthenticatedUser {
                permissions = Set.copyOf(permissions);
        }

        public boolean hasPermission(String permission) {
                return permissions.contains(permission);
        }
}