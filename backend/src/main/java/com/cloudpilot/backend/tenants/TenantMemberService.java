package com.cloudpilot.backend.tenants;

import com.cloudpilot.backend.auth.AuthenticatedUser;
import com.cloudpilot.backend.events.CloudPilotEvent;
import com.cloudpilot.backend.events.CloudPilotEventProducer;
import com.cloudpilot.backend.events.CloudPilotEventType;
import com.cloudpilot.backend.events.MemberRemovedEventData;
import com.cloudpilot.backend.rbac.Role;
import com.cloudpilot.backend.rbac.RoleRepository;
import com.cloudpilot.backend.rbac.TenantRoleResponse;
import com.cloudpilot.backend.subscriptions.SubscriptionFeatureName;
import com.cloudpilot.backend.subscriptions.SubscriptionFeatureService;
import com.cloudpilot.backend.subscriptions.SubscriptionUsageCacheService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TenantMemberService {

    private final TenantMembershipRepository membershipRepository;
    private final RoleRepository roleRepository;
    private final SubscriptionFeatureService subscriptionFeatureService;
    private final SubscriptionUsageCacheService subscriptionUsageCacheService;
        private final CloudPilotEventProducer eventProducer;

    public TenantMemberService(
            TenantMembershipRepository membershipRepository,
            RoleRepository roleRepository,
            SubscriptionFeatureService subscriptionFeatureService,
            SubscriptionUsageCacheService subscriptionUsageCacheService,
            CloudPilotEventProducer eventProducer) {

        this.membershipRepository = membershipRepository;
        this.roleRepository = roleRepository;
        this.subscriptionFeatureService = subscriptionFeatureService;
        this.subscriptionUsageCacheService = subscriptionUsageCacheService;
        this.eventProducer = eventProducer;
    }

    @Transactional
    @PreAuthorize("hasAuthority('USER_REMOVE')")
    public void removeMember(Long membershipId, AuthenticatedUser currentUser) {
        Long tenantId = currentUser.tenantId();
        TenantMembership membership = membershipRepository
                .findByIdAndTenant_Id(membershipId, tenantId)
                .filter(member -> "ACTIVE".equals(member.getStatus()))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Active membership not found"));

        Long removedMembershipId = membership.getId();
        Long removedUserId = membership.getUser().getId();
        String removedUserEmail = membership.getUser().getEmail();
        String removedRole = membership.getRole().getName();

        if (membership.getUser().getId().equals(currentUser.user().getId())) {
            throw new AccessDeniedException("You cannot remove your own membership");
        }

        if ("OWNER".equals(membership.getRole().getName())) {
            if (!"OWNER".equals(currentUser.role())) {
                throw new AccessDeniedException("Only an OWNER can remove another OWNER");
            }
            if (membershipRepository.countByTenant_IdAndRole_NameAndStatus(
                    tenantId, "OWNER", "ACTIVE") <= 1) {
                throw new AccessDeniedException("The organization must have at least one OWNER");
            }
        }

        membership.setStatus("INACTIVE");
        membershipRepository.save(membership);
        subscriptionUsageCacheService.evict(tenantId);

        MemberRemovedEventData eventData =
                new MemberRemovedEventData(
                        removedMembershipId,
                        removedUserId,
                        removedUserEmail,
                        removedRole
                );

        CloudPilotEvent event =
                CloudPilotEvent.create(
                        CloudPilotEventType.MEMBER_REMOVED,
                        tenantId,
                        currentUser.user().getId(),
                        eventData
                );

        eventProducer.publish(event);
    }

    @PreAuthorize("hasAuthority('USER_READ')")
    public List<TenantMemberResponse> getMembers(Long tenantId) {
        return membershipRepository
                .findAllByTenant_IdAndStatus(tenantId, "ACTIVE")
                .stream()
                .map(TenantMemberResponse::from)
                .toList();
    }

    @PreAuthorize("hasAuthority('ROLE_READ')")
    public List<TenantRoleResponse> getRoles(Long tenantId) {
        return roleRepository
                .findAllByTenant_Id(tenantId)
                .stream()
                .map(TenantRoleResponse::from)
                .toList();
    }

    @Transactional
    @PreAuthorize("hasAuthority('ROLE_ASSIGN')")
    public TenantMemberResponse updateRole(
            Long membershipId,
            String requestedRole,
            AuthenticatedUser currentUser) {

        Long tenantId = currentUser.tenantId();
        subscriptionFeatureService.requireFeature(
                tenantId,
                SubscriptionFeatureName.ADVANCED_RBAC
        );

        TenantMembership membership = membershipRepository
                .findByIdAndTenant_Id(membershipId, tenantId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Membership not found")
                );

        String roleName = requestedRole.trim().toUpperCase();
        Role targetRole = roleRepository
                .findByTenant_IdAndName(tenantId, roleName)
                .orElseThrow(() ->
                        new IllegalArgumentException("Role not found: " + roleName)
                );

        if ("OWNER".equals(roleName) &&
                !"OWNER".equals(currentUser.role())) {
            throw new AccessDeniedException(
                    "Only an OWNER can assign the OWNER role"
            );
        }

        String oldRole = membership.getRole().getName();
        if ("OWNER".equals(oldRole) && !"OWNER".equals(roleName)) {
            long ownerCount = membershipRepository
                    .countByTenant_IdAndRole_NameAndStatus(
                            tenantId,
                            "OWNER",
                            "ACTIVE"
                    );

            if (ownerCount <= 1) {
                throw new IllegalStateException(
                        "The organization must have at least one OWNER"
                );
            }
        }

        membership.setRole(targetRole);
        TenantMembership updated = membershipRepository.save(membership);

        return TenantMemberResponse.from(updated);
    }
}
