package com.cloudpilot.backend.subscriptions;

import com.cloudpilot.backend.exception.PlanLimitExceededException;
import com.cloudpilot.backend.projects.ProjectRepository;
import com.cloudpilot.backend.tenants.Tenant;
import com.cloudpilot.backend.tenants.TenantRepository;
import com.cloudpilot.backend.tenants.InvitationStatus;
import com.cloudpilot.backend.tenants.TenantInvitationRepository;
import com.cloudpilot.backend.tenants.TenantMembershipRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class SubscriptionLimitService {

    private final TenantRepository tenantRepository;
    private final ProjectRepository projectRepository;
    private final TenantMembershipRepository membershipRepository;
    private final TenantInvitationRepository invitationRepository;

    public SubscriptionLimitService(
            TenantRepository tenantRepository,
            ProjectRepository projectRepository,
            TenantMembershipRepository membershipRepository,
            TenantInvitationRepository invitationRepository) {

        this.tenantRepository = tenantRepository;
        this.projectRepository = projectRepository;
        this.membershipRepository = membershipRepository;
        this.invitationRepository = invitationRepository;
    }

    public void validateProjectCreation(Long tenantId) {

        SubscriptionPlan plan = getActivePlan(tenantId);

        long currentProjects = projectRepository.countByTenant_Id(tenantId);
        int maxProjects = plan.getMaxProjects();

        if (currentProjects >= maxProjects) {
            throw new PlanLimitExceededException(
                    plan.getDisplayName() +
                    " plan allows a maximum of " +
                    maxProjects +
                    " projects. Upgrade your plan to create more projects."
            );
        }
    }

    public void validateMemberInvitation(Long tenantId) {

        SubscriptionPlan plan = getActivePlan(tenantId);
        long activeMembers = membershipRepository
                .countByTenant_IdAndStatus(tenantId, "ACTIVE");
        long pendingInvitations = invitationRepository
                .countByTenant_IdAndStatusAndExpiresAtAfter(
                        tenantId,
                        InvitationStatus.PENDING.name(),
                        LocalDateTime.now()
                );

        if (activeMembers + pendingInvitations >= plan.getMaxMembers()) {
            throw new PlanLimitExceededException(
                    plan.getDisplayName() +
                    " plan allows a maximum of " +
                    plan.getMaxMembers() +
                    " members. Upgrade your plan to add more members."
            );
        }
    }

    public void validateMemberAcceptance(Long tenantId) {

        SubscriptionPlan plan = getActivePlan(tenantId);
        long activeMembers = membershipRepository
                .countByTenant_IdAndStatus(tenantId, "ACTIVE");

        if (activeMembers >= plan.getMaxMembers()) {
            throw new PlanLimitExceededException(
                    plan.getDisplayName() +
                    " plan has reached its " +
                    plan.getMaxMembers() +
                    " member limit. The organization must upgrade before this invitation can be accepted."
            );
        }
    }

    private SubscriptionPlan getActivePlan(Long tenantId) {

        Tenant tenant = tenantRepository.findOneById(tenantId)
                .orElseThrow(() -> new IllegalStateException("Tenant not found"));

        SubscriptionPlan plan = tenant.getPlan();
        if (plan == null) {
            throw new IllegalStateException("Tenant does not have a subscription plan");
        }
        if (!plan.isActive()) {
            throw new PlanLimitExceededException("The subscription plan is not active");
        }
        return plan;
    }
}