package com.cloudpilot.backend.rbac;

import com.cloudpilot.backend.tenants.Tenant;
import com.cloudpilot.backend.tenants.TenantRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(1)
public class RbacBootstrap implements CommandLineRunner {

    private final TenantRepository tenantRepository;
    private final RbacProvisioningService rbacProvisioningService;

    public RbacBootstrap(
            TenantRepository tenantRepository,
            RbacProvisioningService rbacProvisioningService) {

        this.tenantRepository = tenantRepository;
        this.rbacProvisioningService = rbacProvisioningService;
    }

    @Override
    public void run(String... args) {

        for (Tenant tenant : tenantRepository.findAll()) {
            rbacProvisioningService.provisionTenant(tenant);
        }

        System.out.println("Default CloudPilot RBAC provisioned.");
    }
}