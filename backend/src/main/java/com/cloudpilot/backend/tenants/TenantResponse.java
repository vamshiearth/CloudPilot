package com.cloudpilot.backend.tenants;

public record TenantResponse(
        Long id,
        String name,
        String slug,
        String status
) {

    public static TenantResponse from(Tenant tenant) {
        return new TenantResponse(
                tenant.getId(),
                tenant.getName(),
                tenant.getSlug(),
                tenant.getStatus()
        );
    }
}
