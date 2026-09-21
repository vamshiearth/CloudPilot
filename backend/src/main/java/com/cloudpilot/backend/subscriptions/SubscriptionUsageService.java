package com.cloudpilot.backend.subscriptions;

import com.cloudpilot.backend.projects.ProjectRepository;
import com.cloudpilot.backend.tenants.InvitationStatus;
import com.cloudpilot.backend.tenants.Tenant;
import com.cloudpilot.backend.tenants.TenantInvitationRepository;
import com.cloudpilot.backend.tenants.TenantMembershipRepository;
import com.cloudpilot.backend.tenants.TenantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Service
@SuppressWarnings("null")
public class SubscriptionUsageService {

        private static final Logger log = LoggerFactory.getLogger(SubscriptionUsageService.class);

    private final TenantRepository tenantRepository;
    private final ProjectRepository projectRepository;
    private final TenantMembershipRepository membershipRepository;
    private final TenantInvitationRepository invitationRepository;
    private final SubscriptionUsageCacheService cacheService;

    public SubscriptionUsageService(
            TenantRepository tenantRepository,
            ProjectRepository projectRepository,
            TenantMembershipRepository membershipRepository,
            TenantInvitationRepository invitationRepository,
            SubscriptionUsageCacheService cacheService) {

        this.tenantRepository = tenantRepository;
        this.projectRepository = projectRepository;
        this.membershipRepository = membershipRepository;
        this.invitationRepository = invitationRepository;
        this.cacheService = cacheService;
    }

    public SubscriptionUsageResponse getUsage(Long tenantId) {

        SubscriptionUsageResponse cached = cacheService.get(tenantId);

        if (cached != null) {
            log.debug(
                    "Redis cache HIT for subscription usage tenant {}",
                    tenantId
            );
            return cached;
        }

        log.debug(
                "Redis cache MISS for subscription usage tenant {}. Loading from PostgreSQL.",
                tenantId
        );

        SubscriptionUsageResponse usage = calculateUsage(tenantId);
        cacheService.put(tenantId, usage);

        return usage;
    }

    private SubscriptionUsageResponse calculateUsage(Long tenantId) {

        Tenant tenant = tenantRepository.findOneById(tenantId)
                .orElseThrow(() -> new IllegalStateException("Tenant not found"));

        SubscriptionPlan plan = tenant.getPlan();
        if (plan == null) {
            throw new IllegalStateException(
                    "Tenant does not have a subscription plan"
            );
        }

        long currentProjects = projectRepository.countByTenant_Id(tenantId);
        long activeMembers = membershipRepository
                .countByTenant_IdAndStatus(tenantId, "ACTIVE");
        long pendingInvitations = invitationRepository
                .countByTenant_IdAndStatusAndExpiresAtAfter(
                        tenantId,
                        InvitationStatus.PENDING.name(),
                        LocalDateTime.now()
                );
        long reservedMemberSlots = activeMembers + pendingInvitations;
        var features = plan.getFeatures().stream()
                .map(SubscriptionFeature::getName)
                .collect(Collectors.toUnmodifiableSet());

        return new SubscriptionUsageResponse(
                plan.getName(),
                plan.getDisplayName(),
                currentProjects,
                plan.getMaxProjects(),
                activeMembers,
                pendingInvitations,
                reservedMemberSlots,
                plan.getMaxMembers(),
                currentProjects >= plan.getMaxProjects(),
                reservedMemberSlots >= plan.getMaxMembers(),
                features
        );
    }
}
