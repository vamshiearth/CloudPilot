package com.cloudpilot.backend.tenants;

import com.cloudpilot.backend.users.User;
import org.springframework.stereotype.Service;

@Service
@SuppressWarnings("null")
public class CurrentTenantService {

    private final TenantMembershipRepository membershipRepository;

    public CurrentTenantService(
            TenantMembershipRepository membershipRepository) {

        this.membershipRepository = membershipRepository;
    }

    public Tenant getTenantForUser(User user) {

                var activeMemberships = membershipRepository
                .findByUserId(user.getId())
                .stream()
                .filter(membership ->
                        "ACTIVE".equalsIgnoreCase(
                                membership.getStatus()
                        )
                )
                .toList();

        if (activeMemberships.isEmpty()) {
            throw new IllegalStateException(
                    "User does not belong to an active tenant"
            );
        }

        if (activeMemberships.size() > 1) {
            throw new IllegalStateException(
                    "User belongs to multiple active tenants; tenant selection is required"
            );
        }

        return activeMemberships
                .get(0)
                .getTenant();
    }

    public Tenant getTenantForUser(
            User user,
            Long tenantId) {

        return membershipRepository
                .findByUserIdAndTenantId(
                        user.getId(),
                        tenantId
                )
                .filter(membership ->
                        "ACTIVE".equalsIgnoreCase(
                                membership.getStatus()
                        )
                )
                .map(TenantMembership::getTenant)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "User does not belong to the requested tenant"
                        )
                );
    }
}