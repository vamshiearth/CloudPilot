package com.cloudpilot.backend.rbac;

import com.cloudpilot.backend.tenants.Tenant;
import com.cloudpilot.backend.tenants.TenantMembership;
import com.cloudpilot.backend.tenants.TenantMembershipRepository;
import com.cloudpilot.backend.tenants.TenantRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Component
@Order(2)
@SuppressWarnings("null")
public class LegacyMembershipRoleMigration
        implements CommandLineRunner {

    private final TenantRepository tenantRepository;
    private final TenantMembershipRepository membershipRepository;
    private final RoleRepository roleRepository;

    public LegacyMembershipRoleMigration(
            TenantRepository tenantRepository,
            TenantMembershipRepository membershipRepository,
            RoleRepository roleRepository) {

        this.tenantRepository = tenantRepository;
        this.membershipRepository = membershipRepository;
        this.roleRepository = roleRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {

        for (Tenant tenant : tenantRepository.findAll()) {
            List<TenantMembership> memberships = membershipRepository
                    .findAll()
                    .stream()
                    .filter(membership -> membership.getTenant().getId()
                            .equals(tenant.getId()))
                    .sorted(Comparator.comparing(TenantMembership::getId))
                    .toList();

            if (memberships.isEmpty()) {
                continue;
            }

            Role owner = roleRepository
                    .findByTenant_IdAndName(
                            tenant.getId(),
                            DefaultRoleName.OWNER.name()
                    )
                    .orElseThrow();

            Role member = roleRepository
                    .findByTenant_IdAndName(
                            tenant.getId(),
                            DefaultRoleName.MEMBER.name()
                    )
                    .orElseThrow();

            boolean ownerAssigned = false;

            for (TenantMembership membership : memberships) {
                if (membership.getRole() != null) {
                    continue;
                }

                membership.setRole(ownerAssigned ? member : owner);
                ownerAssigned = true;
                membershipRepository.save(membership);
            }
        }

        System.out.println("Legacy membership roles assigned.");
    }
}