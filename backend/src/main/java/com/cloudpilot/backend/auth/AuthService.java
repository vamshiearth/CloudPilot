package com.cloudpilot.backend.auth;

import com.cloudpilot.backend.users.User;
import com.cloudpilot.backend.users.UserRepository;
import com.cloudpilot.backend.exception.EmailAlreadyExistsException;
import com.cloudpilot.backend.exception.InvalidCredentialsException;
import com.cloudpilot.backend.tenants.Tenant;
import com.cloudpilot.backend.tenants.TenantMembership;
import com.cloudpilot.backend.tenants.TenantMembershipRepository;
import com.cloudpilot.backend.tenants.TenantRepository;
import com.cloudpilot.backend.tenants.CurrentTenantService;
import com.cloudpilot.backend.rbac.RbacProvisioningService;
import com.cloudpilot.backend.rbac.DefaultRoleName;
import com.cloudpilot.backend.rbac.Role;
import com.cloudpilot.backend.rbac.RoleRepository;
import com.cloudpilot.backend.subscriptions.SubscriptionPlan;
import com.cloudpilot.backend.subscriptions.SubscriptionPlanName;
import com.cloudpilot.backend.subscriptions.SubscriptionPlanRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final TenantRepository tenantRepository;
    private final TenantMembershipRepository membershipRepository;
    private final CurrentTenantService currentTenantService;
    private final RbacProvisioningService rbacProvisioningService;
    private final RoleRepository roleRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final BCryptPasswordEncoder passwordEncoder =
            new BCryptPasswordEncoder();

    public AuthService(
            UserRepository userRepository,
            JwtService jwtService,
            TenantRepository tenantRepository,
            TenantMembershipRepository membershipRepository,
            CurrentTenantService currentTenantService,
            RbacProvisioningService rbacProvisioningService,
            RoleRepository roleRepository,
            SubscriptionPlanRepository subscriptionPlanRepository) {

        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.tenantRepository = tenantRepository;
        this.membershipRepository = membershipRepository;
        this.currentTenantService = currentTenantService;
        this.rbacProvisioningService = rbacProvisioningService;
        this.roleRepository = roleRepository;
        this.subscriptionPlanRepository = subscriptionPlanRepository;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {

        String email = request.email().trim().toLowerCase();

        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException(
                    "An account with this email already exists"
            );
        }

                String organizationName = request.organizationName().trim();
                String slug = createUniqueSlug(organizationName);

                SubscriptionPlan freePlan = subscriptionPlanRepository
                    .findByName(SubscriptionPlanName.FREE.name())
                    .orElseThrow(() ->
                        new IllegalStateException(
                            "FREE subscription plan not found"
                        )
                    );

                Tenant tenant = Tenant.builder()
                    .name(organizationName)
                    .slug(slug)
                    .status("ACTIVE")
                    .plan(freePlan)
                    .build();

                tenant = tenantRepository.save(tenant);
                rbacProvisioningService.provisionTenant(tenant);

                Role ownerRole = roleRepository
                    .findByTenant_IdAndName(
                        tenant.getId(),
                        DefaultRoleName.OWNER.name()
                    )
                    .orElseThrow(() ->
                        new IllegalStateException(
                            "OWNER role was not provisioned"
                        )
                    );

        User user = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(request.password()))
                .firstName(request.firstName().trim())
                .lastName(request.lastName().trim())
                .status("ACTIVE")
                .build();

        user = userRepository.save(user);

        TenantMembership membership = TenantMembership.builder()
                .tenant(tenant)
                .user(user)
            .role(ownerRole)
                .status("ACTIVE")
                .build();

        membershipRepository.save(membership);

        return UserResponse.from(user);
    }

    private String createUniqueSlug(String organizationName) {

        String baseSlug = organizationName
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");

        if (baseSlug.isBlank()) {
            baseSlug = "organization";
        }

        String slug = baseSlug;
        int suffix = 2;

        while (tenantRepository.existsBySlug(slug)) {
            slug = baseSlug + "-" + suffix;
            suffix++;
        }

        return slug;
    }

        public LoginResponse login(LoginRequest request) {

        String email = request.email().trim().toLowerCase();

        User user = userRepository.findByEmail(email)
            .orElseThrow(() ->
                new InvalidCredentialsException(
                    "Invalid email or password"
                )
            );

        boolean passwordMatches =
            passwordEncoder.matches(
                request.password(),
                user.getPasswordHash()
            );

        if (!passwordMatches) {
            throw new InvalidCredentialsException(
                "Invalid email or password"
            );
        }

        Tenant tenant = currentTenantService.getTenantForUser(user);
        String token = jwtService.generateToken(user, tenant);

        return new LoginResponse(
            token,
            user.getId(),
            user.getEmail(),
            user.getFirstName(),
            user.getLastName(),
            user.getStatus(),
            tenant.getId(),
            tenant.getName()
        );
        }
}
