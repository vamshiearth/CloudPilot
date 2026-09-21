package com.cloudpilot.backend.tenants;

import org.springframework.stereotype.Service;

@Service
public class TenantService {

    private final TenantRepository tenantRepository;

    public TenantService(
            TenantRepository tenantRepository) {

        this.tenantRepository = tenantRepository;
    }

    public TenantResponse getCurrentTenant(
            Long tenantId) {

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Authenticated tenant not found"
                        )
                );

        return TenantResponse.from(tenant);
    }
}
