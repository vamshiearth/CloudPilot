package com.cloudpilot.backend.subscriptions;

import com.cloudpilot.backend.auth.AuthenticatedUser;
import com.cloudpilot.backend.auth.JwtAuthenticationFilter;
import com.cloudpilot.backend.auth.JwtService;
import com.cloudpilot.backend.observability.TenantWorkloadTracker;
import com.cloudpilot.backend.projects.ProjectRepository;
import com.cloudpilot.backend.rbac.Role;
import com.cloudpilot.backend.rbac.RoleRepository;
import com.cloudpilot.backend.tenants.*;
import com.cloudpilot.backend.users.User;
import com.cloudpilot.backend.users.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

// Real Redis and Spring service proxies; repository writes and transaction outcomes are simulated.
@SpringJUnitConfig({SubscriptionUsageCacheIntegrationTests.Config.class, MembershipCacheIntegrationTests.Config.class})
@EnabledIfSystemProperty(named = "cloudpilot.redis.integration", matches = "true")
@SuppressWarnings("unchecked")
class MembershipCacheIntegrationTests {
    private static final AtomicLong IDS = new AtomicLong(-System.nanoTime());
    @Autowired private TenantInvitationService invitationService;
    @Autowired private TenantMemberService memberService;
    @Autowired private SubscriptionUsageService usageService;
    @Autowired private SubscriptionManagementService managementService;
    @Autowired private SubscriptionUsageCacheService cacheService;
    @Autowired private StringRedisTemplate redis;
    @Autowired private TenantRepository tenants;
    @Autowired private TenantMembershipRepository memberships;
    @Autowired private TenantInvitationRepository invitations;
    @Autowired private RoleRepository roles;
    @Autowired private UserRepository users;
    @Autowired private ProjectRepository projects;
    @Autowired private SubscriptionLimitService limits;
    @Autowired private SubscriptionFeatureService features;
    @Autowired private SubscriptionPlanRepository plans;
    @Autowired private PasswordEncoder encoder;
    @Autowired private TestTransactionManager transactions;

    private Long tenantId;
    private Long otherTenantId;
    private Tenant tenant;
    private Role memberRole;
    private User owner;
    private User employee;
    private TenantMembership employeeMembership;
    private AuthenticatedUser currentUser;
    private final List<TenantInvitation> storedInvitations = new ArrayList<>();
    private long activeMembers;
    private SubscriptionUsageResponse otherUsage;

    @BeforeEach
    void prepare() {
        reset(tenants, memberships, invitations, roles, users, projects, limits, features, plans);
        transactions.beforeCommit = () -> {};
        transactions.failCommit = false;
        tenantId = IDS.decrementAndGet();
        otherTenantId = IDS.decrementAndGet();
        storedInvitations.clear();
        activeMembers = 2;
        SubscriptionPlan plan = SubscriptionPlan.builder().name("STARTER").displayName("Starter")
                .maxMembers(10).maxProjects(20).active(true).build();
        tenant = Tenant.builder().id(tenantId).name("Test tenant").plan(plan).build();
        memberRole = Role.builder().id(1L).name("MEMBER").tenant(tenant).build();
        owner = User.builder().id(1L).email("owner@example.com").build();
        employee = User.builder().id(2L).email("employee@example.com")
                .passwordHash(encoder.encode("test-password")).firstName("Test").lastName("Member").build();
        employeeMembership = TenantMembership.builder().id(2L).tenant(tenant).user(employee)
                .role(memberRole).status("ACTIVE").build();
        currentUser = new AuthenticatedUser(owner, tenantId, "OWNER", Set.of("USER_INVITE", "USER_REMOVE", "ROLE_ASSIGN"));
        authenticate(currentUser);
        when(tenants.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(tenants.findOneById(tenantId)).thenReturn(Optional.of(tenant));
        when(roles.findByTenant_IdAndName(tenantId, "MEMBER")).thenReturn(Optional.of(memberRole));
        when(roles.findByTenant_IdAndName(tenantId, "ADMIN")).thenReturn(Optional.of(
                Role.builder().id(3L).name("ADMIN").tenant(tenant).build()));
        when(users.findByEmail(employee.getEmail())).thenReturn(Optional.of(employee));
        when(memberships.countByTenant_IdAndStatus(tenantId, "ACTIVE")).thenAnswer(call -> activeMembers);
        when(memberships.findByIdAndTenant_Id(employeeMembership.getId(), tenantId))
                .thenReturn(Optional.of(employeeMembership));
        when(memberships.countByTenant_IdAndRole_NameAndStatus(tenantId, "OWNER", "ACTIVE")).thenReturn(1L);
        when(memberships.findByUserIdAndTenantIdAndStatus(employee.getId(), tenantId, "ACTIVE"))
                .thenAnswer(call -> "ACTIVE".equals(employeeMembership.getStatus())
                        ? Optional.of(employeeMembership) : Optional.empty());
        when(memberships.save(any(TenantMembership.class))).thenAnswer(call -> {
            TenantMembership membership = call.getArgument(0);
            if (membership.getId() == null) {
                membership.setId(5L);
                activeMembers++;
            } else if ("INACTIVE".equals(membership.getStatus())) {
                activeMembers--;
            }
            return membership;
        });
        when(invitations.save(any(TenantInvitation.class))).thenAnswer(call -> {
            TenantInvitation invitation = call.getArgument(0);
            if (invitation.getId() == null) {
                invitation.setId((long) storedInvitations.size() + 1);
                storedInvitations.add(invitation);
            }
            return invitation;
        });
        when(invitations.findByToken(anyString())).thenAnswer(call -> storedInvitations.stream()
                .filter(invitation -> invitation.getToken().equals(call.getArgument(0))).findFirst());
        when(invitations.findByIdAndTenant_IdAndStatus(anyLong(), eq(tenantId), eq("PENDING")))
                .thenAnswer(call -> storedInvitations.stream().filter(invitation ->
                        invitation.getId().equals(call.getArgument(0)) && "PENDING".equals(invitation.getStatus())).findFirst());
        when(invitations.countByTenant_IdAndStatusAndExpiresAtAfter(eq(tenantId), eq("PENDING"), any()))
                .thenAnswer(call -> storedInvitations.stream().filter(invitation ->
                        "PENDING".equals(invitation.getStatus()) && invitation.getExpiresAt().isAfter(LocalDateTime.now())).count());
        otherUsage = new SubscriptionUsageResponse("FREE", "Free", 1, 3, 1, 0, 1, 3, false, false, Set.of());
        cacheService.put(otherTenantId, otherUsage);
        usageService.getUsage(tenantId);
        // Assert the cache is still present at the commit boundary, then check eviction after each call.
        transactions.beforeCommit = () -> assertNotNull(cacheService.get(tenantId));
    }

    @AfterEach
    void cleanup() {
        transactions.beforeCommit = () -> {};
        transactions.failCommit = false;
        SecurityContextHolder.clearContext();
        redis.delete(List.of(key(tenantId), key(otherTenantId)));
    }

    @Test
    void inviteCancelAcceptAndRemoveRefreshCountsOnlyForTheirTenant() {
        var first = invitationService.createInvitation(new CreateInvitationRequest(employee.getEmail(), "MEMBER"), currentUser);
        assertEvictedAndReload(2, 1);
        invitationService.revokeInvitation(first.invitation().id(), currentUser);
        assertEvictedAndReload(2, 0);
        invitationService.createInvitation(new CreateInvitationRequest(employee.getEmail(), "MEMBER"), currentUser);
        assertEvictedAndReload(2, 1);
        TenantInvitation invitation = storedInvitations.get(1);
        invitationService.acceptInvitation(invitation.getToken(), new AcceptInvitationRequest("Test", "Member", "test-password"));
        assertEquals("ACCEPTED", invitation.getStatus());
        assertNotNull(invitation.getAcceptedAt());
        assertEvictedAndReload(3, 0);
        memberService.removeMember(employeeMembership.getId(), currentUser);
        assertEquals("INACTIVE", employeeMembership.getStatus());
        assertEvictedAndReload(2, 0);
    }

    @Test
    void roleChangeKeepsUsageAndDoesNotExtendTtl() {
        SubscriptionUsageResponse original = cacheService.get(tenantId);
        long before = redis.getExpire(key(tenantId), TimeUnit.MILLISECONDS);
        memberService.updateRole(employeeMembership.getId(), "ADMIN", currentUser);
        assertEquals("ADMIN", employeeMembership.getRole().getName());
        assertEquals(original, cacheService.get(tenantId));
        assertTrue(redis.getExpire(key(tenantId), TimeUnit.MILLISECONDS) <= before);
        assertEquals(otherUsage, cacheService.get(otherTenantId));
    }

    @Test
    void failedSaveAndFailedCommitKeepTheCache() {
        SubscriptionUsageResponse original = cacheService.get(tenantId);
        doThrow(new IllegalStateException("Write failed")).when(invitations).save(any());
        assertThrows(IllegalStateException.class, () -> invitationService.createInvitation(
                new CreateInvitationRequest(employee.getEmail(), "MEMBER"), currentUser));
        assertEquals(original, cacheService.get(tenantId));
        // A successful save may still fail at commit; the queued eviction must then be discarded.
        doAnswer(call -> call.getArgument(0)).when(invitations).save(any());
        transactions.failCommit = true;
        assertThrows(TransactionSystemException.class, () -> invitationService.createInvitation(
                new CreateInvitationRequest(employee.getEmail(), "MEMBER"), currentUser));
        assertEquals(original, cacheService.get(tenantId));
        assertEquals(otherUsage, cacheService.get(otherTenantId));
    }

    @Test
    void rejectedAcceptanceKeepsCountsAndCache() {
        invitationService.createInvitation(new CreateInvitationRequest(employee.getEmail(), "MEMBER"), currentUser);
        assertEvictedAndReload(2, 1);
        SubscriptionUsageResponse original = cacheService.get(tenantId);
        assertThrows(IllegalArgumentException.class, () -> invitationService.acceptInvitation(
                storedInvitations.get(0).getToken(), new AcceptInvitationRequest("Test", "Member", "wrong-password")));
        assertEquals(original, cacheService.get(tenantId));
        verify(memberships, never()).save(any());
        assertEquals("PENDING", storedInvitations.get(0).getStatus());
    }

    @Test
    void removalRequiresPermissionTenantMatchAndOwnerProtection() {
        SubscriptionUsageResponse original = cacheService.get(tenantId);
        authenticate(new AuthenticatedUser(employee, tenantId, "MEMBER", Set.of()));
        assertThrows(AccessDeniedException.class, () -> memberService.removeMember(2L, currentUser));
        authenticate(currentUser);
        ResponseStatusException missing = assertThrows(ResponseStatusException.class,
                () -> memberService.removeMember(999L, currentUser));
        assertEquals(404, missing.getStatusCode().value());
        AuthenticatedUser self = new AuthenticatedUser(employee, tenantId, "ADMIN", Set.of("USER_REMOVE"));
        authenticate(self);
        assertThrows(AccessDeniedException.class, () -> memberService.removeMember(2L, self));
        employeeMembership.setRole(Role.builder().name("OWNER").build());
        AuthenticatedUser admin = new AuthenticatedUser(owner, tenantId, "ADMIN", Set.of("USER_REMOVE"));
        authenticate(admin);
        assertThrows(AccessDeniedException.class, () -> memberService.removeMember(2L, admin));
        authenticate(currentUser);
        assertThrows(AccessDeniedException.class, () -> memberService.removeMember(2L, currentUser));
        verify(memberships, never()).save(any());
        assertEquals(original, cacheService.get(tenantId));
        assertEquals(otherUsage, cacheService.get(otherTenantId));
    }

    @Test
    void removedMembershipNoLongerAuthenticatesWithItsExistingJwtEvenWithWarmCache() throws Exception {
        JwtService jwt = new JwtService(Base64.getEncoder().encodeToString(
                "test-only-signing-key-for-membership-cache".getBytes(StandardCharsets.UTF_8)), 60000);
        String token = jwt.generateToken(employee, tenant);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(
            jwt, users, memberships, mock(TenantWorkloadTracker.class));
        assertTrue(authenticateToken(filter, token));
        authenticate(currentUser);
        SubscriptionUsageResponse stale = cacheService.get(tenantId);
        memberService.removeMember(2L, currentUser);
        cacheService.put(tenantId, stale);
        assertFalse(authenticateToken(filter, token));
        verify(memberships, times(2)).findByUserIdAndTenantIdAndStatus(employee.getId(), tenantId, "ACTIVE");
        assertEquals(stale, cacheService.get(tenantId));
    }

        @Test
        void planChangesReplaceOnlyTheAffectedTenantCacheAndInvalidRequestsKeepIt() {
        SubscriptionPlan free = SubscriptionPlan.builder().name("FREE").displayName("Free")
            .maxMembers(3).maxProjects(3).active(true).build();
        SubscriptionPlan starter = SubscriptionPlan.builder().name("STARTER").displayName("Starter")
            .maxMembers(10).maxProjects(20).active(true)
            .features(Set.of(SubscriptionFeature.builder().name("ADVANCED_RBAC").build())).build();
        SubscriptionPlan pro = SubscriptionPlan.builder().name("PRO").displayName("Pro")
            .maxMembers(50).maxProjects(100).active(true)
            .features(Set.of(SubscriptionFeature.builder().name("ADVANCED_RBAC").build())).build();
        tenant.setPlan(free);
        cacheService.evict(tenantId);
        usageService.getUsage(tenantId);
        when(plans.findByName("STARTER")).thenReturn(Optional.of(starter));
        when(plans.findByName("PRO")).thenReturn(Optional.of(pro));
        when(plans.findByName("FREE")).thenReturn(Optional.of(free));

        authenticate(new AuthenticatedUser(owner, tenantId, "OWNER", Set.of("BILLING_UPDATE")));
        SubscriptionUsageResponse starterUsage = managementService.changePlan(tenantId, owner.getId(), "STARTER");
        assertEquals("STARTER", starterUsage.planName());
        assertEquals(20, starterUsage.maxProjects());
        assertEquals(10, starterUsage.maxMembers());
        assertTrue(starterUsage.features().contains("ADVANCED_RBAC"));
        assertEquals(starterUsage, cacheService.get(tenantId));
        assertEquals(otherUsage, cacheService.get(otherTenantId));
        assertTrue(redis.getExpire(key(tenantId)) > 0);

        SubscriptionUsageResponse proUsage = managementService.changePlan(tenantId, owner.getId(), "PRO");
        assertEquals("PRO", proUsage.planName());
        assertEquals(100, proUsage.maxProjects());
        assertEquals(50, proUsage.maxMembers());

        SubscriptionUsageResponse freeUsage = managementService.changePlan(tenantId, owner.getId(), "FREE");
        assertEquals("FREE", freeUsage.planName());
        assertEquals(3, freeUsage.maxProjects());
        assertEquals(3, freeUsage.maxMembers());
        assertTrue(freeUsage.features().isEmpty());

        SubscriptionUsageResponse cached = cacheService.get(tenantId);
        assertThrows(IllegalArgumentException.class,
            () -> managementService.changePlan(tenantId, owner.getId(), "ENTERPRISE"));
        assertEquals(cached, cacheService.get(tenantId));

        authenticate(new AuthenticatedUser(employee, tenantId, "MEMBER", Set.of()));
        assertThrows(AccessDeniedException.class,
            () -> managementService.changePlan(tenantId, owner.getId(), "STARTER"));
        assertEquals(cached, cacheService.get(tenantId));
        assertEquals(otherUsage, cacheService.get(otherTenantId));

        authenticate(new AuthenticatedUser(owner, tenantId, "OWNER", Set.of("BILLING_UPDATE")));
        transactions.failCommit = true;
        assertThrows(TransactionSystemException.class,
            () -> managementService.changePlan(tenantId, owner.getId(), "STARTER"));
        assertEquals(cached, cacheService.get(tenantId));
        assertEquals(otherUsage, cacheService.get(otherTenantId));
        }

    private boolean authenticateToken(JwtAuthenticationFilter filter, String token) throws Exception {
        SecurityContextHolder.clearContext();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/auth/me");
        request.addHeader("Authorization", "Bearer " + token);
        filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> {});
        return SecurityContextHolder.getContext().getAuthentication() != null;
    }

    private void assertEvictedAndReload(long active, long pending) {
        assertNull(cacheService.get(tenantId));
        assertEquals(otherUsage, cacheService.get(otherTenantId));
        SubscriptionUsageResponse usage = usageService.getUsage(tenantId);
        assertEquals(active, usage.activeMembers());
        assertEquals(pending, usage.pendingInvitations());
        assertEquals(active + pending, usage.reservedMemberSlots());
        clearInvocations(tenants, projects, memberships, invitations);
        assertEquals(usage, usageService.getUsage(tenantId));
        verifyNoInteractions(tenants, projects, memberships, invitations);
    }

    private void authenticate(AuthenticatedUser principal) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                principal, null, principal.permissions().stream().map(SimpleGrantedAuthority::new).toList()));
    }

    private static String key(Long id) { return "cloudpilot:tenant:" + id + ":subscription-usage"; }

    static class TestTransactionManager extends AbstractPlatformTransactionManager {
        Runnable beforeCommit = () -> {};
        boolean failCommit;
        @Override protected Object doGetTransaction() { return new Object(); }
        @Override protected void doBegin(Object transaction, TransactionDefinition definition) {}
        @Override protected void doCommit(DefaultTransactionStatus status) {
            beforeCommit.run();
            if (failCommit) throw new TransactionSystemException("Commit failed");
        }
        @Override protected void doRollback(DefaultTransactionStatus status) {}
    }

    @TestConfiguration(proxyBeanMethods = false)
    @EnableTransactionManagement
    @EnableMethodSecurity
        @Import({TenantInvitationService.class, TenantMemberService.class, SubscriptionUsageService.class,
            SubscriptionManagementService.class})
    static class Config {
        @Bean TenantRepository tenants() { return mock(TenantRepository.class); }
        @Bean TenantMembershipRepository memberships() { return mock(TenantMembershipRepository.class); }
        @Bean TenantInvitationRepository invitations() { return mock(TenantInvitationRepository.class); }
        @Bean RoleRepository roles() { return mock(RoleRepository.class); }
        @Bean UserRepository users() { return mock(UserRepository.class); }
        @Bean ProjectRepository projects() { return mock(ProjectRepository.class); }
        @Bean SubscriptionLimitService limits() { return mock(SubscriptionLimitService.class); }
        @Bean SubscriptionFeatureService features() { return mock(SubscriptionFeatureService.class); }
        @Bean SubscriptionPlanRepository plans() { return mock(SubscriptionPlanRepository.class); }
        @Bean PasswordEncoder encoder() { return new BCryptPasswordEncoder(4); }
        @Bean TestTransactionManager transactionManager() {
            TestTransactionManager manager = new TestTransactionManager();
            manager.setRollbackOnCommitFailure(true);
            return manager;
        }
        @Bean TransactionOperations transactionOperations(TestTransactionManager manager) {
            return new TransactionTemplate(manager);
        }
    }
}
