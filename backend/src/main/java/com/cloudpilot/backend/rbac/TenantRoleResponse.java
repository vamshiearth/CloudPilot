package com.cloudpilot.backend.rbac;

public record TenantRoleResponse(
        Long id,
        String name,
        String description
) {

    public static TenantRoleResponse from(Role role) {
        return new TenantRoleResponse(
                role.getId(),
                role.getName(),
                role.getDescription()
        );
    }
}