package com.cloudpilot.backend.tenants;

import com.cloudpilot.backend.auth.AuthenticatedUser;
import com.cloudpilot.backend.events.CloudPilotEvent;
import com.cloudpilot.backend.events.CloudPilotEventProducer;
import com.cloudpilot.backend.events.CloudPilotEventType;
import com.cloudpilot.backend.events.InvitationAcceptedEventData;
import com.cloudpilot.backend.events.InvitationCreatedEventData;
import com.cloudpilot.backend.exception.InvitationConflictException;
import com.cloudpilot.backend.rbac.Role;
import com.cloudpilot.backend.rbac.RoleRepository;
import com.cloudpilot.backend.subscriptions.SubscriptionLimitService;
import com.cloudpilot.backend.subscriptions.SubscriptionUsageCacheService;
import com.cloudpilot.backend.users.User;
import com.cloudpilot.backend.users.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class TenantInvitationService {

    private final TenantInvitationRepository invitationRepository;
    private final TenantRepository tenantRepository;
    private final TenantMembershipRepository membershipRepository;
    private final RoleRepository roleRepository;
        private final UserRepository userRepository;
        private final PasswordEncoder passwordEncoder;
        private final SubscriptionLimitService subscriptionLimitService;
    private final SubscriptionUsageCacheService subscriptionUsageCacheService;
        private final CloudPilotEventProducer eventProducer;

    public TenantInvitationService(
            TenantInvitationRepository invitationRepository,
            TenantRepository tenantRepository,
            TenantMembershipRepository membershipRepository,
            RoleRepository roleRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            SubscriptionLimitService subscriptionLimitService,
            SubscriptionUsageCacheService subscriptionUsageCacheService,
            CloudPilotEventProducer eventProducer) {

        this.invitationRepository = invitationRepository;
        this.tenantRepository = tenantRepository;
        this.membershipRepository = membershipRepository;
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.subscriptionLimitService = subscriptionLimitService;
        this.subscriptionUsageCacheService = subscriptionUsageCacheService;
        this.eventProducer = eventProducer;
    }

    @Transactional
    @PreAuthorize("hasAuthority('USER_INVITE')")
        public CreateInvitationResponse createInvitation(
            CreateInvitationRequest request,
            AuthenticatedUser currentUser) {

        Long tenantId = currentUser.tenantId();
        String email = request.email().trim().toLowerCase();
        String roleName = request.role().trim().toUpperCase();

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalStateException("Tenant not found"));

        Role role = roleRepository
                .findByTenant_IdAndName(tenantId, roleName)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Role not found: " + roleName
                ));

        if ("OWNER".equals(roleName) &&
                !"OWNER".equals(currentUser.role())) {
            throw new AccessDeniedException(
                    "Only an OWNER can invite another OWNER"
            );
        }

        if (invitationRepository.existsByTenant_IdAndEmailAndStatus(
                tenantId, email, InvitationStatus.PENDING.name())) {
            throw new InvitationConflictException(
                    "A pending invitation already exists for this email"
            );
        }

        boolean alreadyMember = membershipRepository
                .findAllByTenant_IdAndStatus(tenantId, "ACTIVE")
                .stream()
                .anyMatch(membership -> membership.getUser().getEmail()
                        .equalsIgnoreCase(email));

        if (alreadyMember) {
            throw new InvitationConflictException(
                    "This user is already a member of the organization"
            );
        }

                subscriptionLimitService.validateMemberInvitation(tenantId);

        TenantInvitation invitation = TenantInvitation.builder()
                .tenant(tenant)
                .email(email)
                .role(role)
                .invitedBy(currentUser.user())
                .token(UUID.randomUUID().toString())
                .status(InvitationStatus.PENDING.name())
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();

        TenantInvitation saved = invitationRepository.save(invitation);
        subscriptionUsageCacheService.evict(tenantId);

        InvitationCreatedEventData eventData =
                new InvitationCreatedEventData(
                        saved.getId(),
                        saved.getEmail(),
                        saved.getRole().getName()
                );

        CloudPilotEvent event =
                CloudPilotEvent.create(
                        CloudPilotEventType.INVITATION_CREATED,
                        tenantId,
                        currentUser.user().getId(),
                        eventData
                );

        eventProducer.publish(event);

        String invitationUrl =
                "http://localhost:5173/invite/" + saved.getToken();

        return new CreateInvitationResponse(
                InvitationResponse.from(saved),
                invitationUrl
        );
    }

    @PreAuthorize("hasAuthority('USER_READ')")
    public List<InvitationResponse> getPendingInvitations(Long tenantId) {
        return invitationRepository
                .findAllByTenant_IdAndStatus(
                        tenantId,
                        InvitationStatus.PENDING.name()
                )
                .stream()
                .map(InvitationResponse::from)
                .toList();
    }

    @Transactional
    @PreAuthorize("hasAuthority('USER_INVITE')")
    public void revokeInvitation(
            Long invitationId,
            AuthenticatedUser currentUser) {

        Long tenantId = currentUser.tenantId();
        TenantInvitation invitation = invitationRepository
                .findByIdAndTenant_IdAndStatus(
                        invitationId,
                        tenantId,
                        InvitationStatus.PENDING.name()
                )
                .orElseThrow(() ->
                        new InvitationConflictException(
                                "Pending invitation not found"
                        )
                );

        if ("OWNER".equals(invitation.getRole().getName()) &&
                !"OWNER".equals(currentUser.role())) {
            throw new AccessDeniedException(
                    "Only an OWNER can revoke an OWNER invitation"
            );
        }

        invitation.setStatus(InvitationStatus.REVOKED.name());
        invitationRepository.save(invitation);
        subscriptionUsageCacheService.evict(tenantId);
    }

        public PublicInvitationResponse getInvitation(String token) {

                TenantInvitation invitation = invitationRepository
                                .findByToken(token)
                                .orElseThrow(() ->
                                                new IllegalArgumentException("Invitation not found")
                                );

                validateInvitation(invitation);

                subscriptionLimitService.validateMemberAcceptance(
                        invitation.getTenant().getId()
                );
                return PublicInvitationResponse.from(invitation);
        }

        @Transactional
        public void acceptInvitation(
                        String token,
                        AcceptInvitationRequest request) {

                TenantInvitation invitation = invitationRepository
                                .findByToken(token)
                                .orElseThrow(() ->
                                                new IllegalArgumentException("Invitation not found")
                                );

                validateInvitation(invitation);

                String email = invitation.getEmail().trim().toLowerCase();
                User user = userRepository.findByEmail(email).orElse(null);

                if (user == null) {
                        user = userRepository.save(User.builder()
                                        .email(email)
                                        .passwordHash(passwordEncoder.encode(request.password()))
                                        .firstName(request.firstName().trim())
                                        .lastName(request.lastName().trim())
                                        .status("ACTIVE")
                                        .build());
                } else if (!passwordEncoder.matches(
                                request.password(),
                                user.getPasswordHash())) {
                        throw new IllegalArgumentException(
                                        "An account with this email already exists. Use the existing account password."
                        );
                }

                Long tenantId = invitation.getTenant().getId();
                if (membershipRepository.existsByUserIdAndTenantId(user.getId(), tenantId)) {
                        throw new IllegalArgumentException(
                                        "User is already a member of this organization"
                        );
                }

                TenantMembership savedMembership =
                        membershipRepository.save(TenantMembership.builder()
                                .tenant(invitation.getTenant())
                                .user(user)
                                .role(invitation.getRole())
                                .status("ACTIVE")
                                .build());

                invitation.setStatus(InvitationStatus.ACCEPTED.name());
                invitation.setAcceptedAt(LocalDateTime.now());
                invitationRepository.save(invitation);
                subscriptionUsageCacheService.evict(tenantId);

                InvitationAcceptedEventData eventData =
                                new InvitationAcceptedEventData(
                                                invitation.getId(),
                                                savedMembership.getId(),
                                                user.getId(),
                                                user.getEmail(),
                                                savedMembership.getRole().getName()
                                );

                CloudPilotEvent event =
                                CloudPilotEvent.create(
                                                CloudPilotEventType.INVITATION_ACCEPTED,
                                                tenantId,
                                                user.getId(),
                                                eventData
                                );

                eventProducer.publish(event);
        }

        private void validateInvitation(TenantInvitation invitation) {

                if (!InvitationStatus.PENDING.name().equals(invitation.getStatus())) {
                        throw new IllegalStateException("Invitation is no longer active");
                }

                if (invitation.getExpiresAt().isBefore(LocalDateTime.now())) {
                        invitation.setStatus(InvitationStatus.EXPIRED.name());
                        invitationRepository.save(invitation);
                        throw new IllegalStateException("Invitation has expired");
                }
        }
}
