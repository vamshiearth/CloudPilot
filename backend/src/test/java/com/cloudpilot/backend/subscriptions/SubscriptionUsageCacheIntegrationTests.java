package com.cloudpilot.backend.subscriptions;

import com.cloudpilot.backend.events.CloudPilotEventProducer;
import com.cloudpilot.backend.projects.ProjectRepository;
import com.cloudpilot.backend.projects.CreateProjectRequest;
import com.cloudpilot.backend.projects.Project;
import com.cloudpilot.backend.projects.ProjectService;
import com.cloudpilot.backend.projects.UpdateProjectRequest;
import com.cloudpilot.backend.exception.PlanLimitExceededException;
import com.cloudpilot.backend.exception.ProjectHasTasksException;
import com.cloudpilot.backend.tasks.TaskRepository;
import com.cloudpilot.backend.tenants.CurrentTenantService;
import com.cloudpilot.backend.users.User;
import com.cloudpilot.backend.tenants.Tenant;
import com.cloudpilot.backend.tenants.TenantInvitationRepository;
import com.cloudpilot.backend.tenants.TenantMembershipRepository;
import com.cloudpilot.backend.tenants.TenantRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

// Opt in with -Dcloudpilot.redis.integration=true; requires Redis on localhost:6379.
// Only synthetic negative tenant IDs are used. PostgreSQL is never accessed.
@SpringJUnitConfig(SubscriptionUsageCacheIntegrationTests.Config.class)
@EnabledIfSystemProperty(named = "cloudpilot.redis.integration", matches = "true")
class SubscriptionUsageCacheIntegrationTests {

    private static final AtomicLong TENANT_IDS = new AtomicLong(-System.currentTimeMillis());

    @Autowired
    private SubscriptionUsageCacheService cacheService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private Long firstTenantId;
    private Long secondTenantId;

    @BeforeEach
    void allocateTestTenants() {
        firstTenantId = TENANT_IDS.decrementAndGet();
        secondTenantId = TENANT_IDS.decrementAndGet();
        assertNull(cacheService.get(firstTenantId));
        assertNull(cacheService.get(secondTenantId));
    }

    @AfterEach
    void removeTestKeys() {
        redisTemplate.delete(List.of(key(firstTenantId), key(secondTenantId)));
    }

    @Test
    void missStoresJsonAndRepeatedHitsSkipCalculationsForEachTenant() throws InterruptedException {
        TenantRepository tenants = mock(TenantRepository.class);
        ProjectRepository projects = mock(ProjectRepository.class);
        TenantMembershipRepository memberships = mock(TenantMembershipRepository.class);
        TenantInvitationRepository invitations = mock(TenantInvitationRepository.class);

        SubscriptionPlan free = SubscriptionPlan.builder()
                .name("FREE").displayName("Free")
                .maxProjects(3).maxMembers(3).build();
        SubscriptionPlan pro = SubscriptionPlan.builder()
                .name("PRO").displayName("Pro")
                .maxProjects(100).maxMembers(50)
                .features(Set.of(SubscriptionFeature.builder().name("ADVANCED_RBAC").build()))
                .build();

        when(tenants.findOneById(firstTenantId))
                .thenReturn(Optional.of(Tenant.builder().id(firstTenantId).plan(free).build()));
        when(tenants.findOneById(secondTenantId))
                .thenReturn(Optional.of(Tenant.builder().id(secondTenantId).plan(pro).build()));
        when(projects.countByTenant_Id(firstTenantId)).thenReturn(3L);
        when(projects.countByTenant_Id(secondTenantId)).thenReturn(7L);
        when(memberships.countByTenant_IdAndStatus(firstTenantId, "ACTIVE")).thenReturn(2L);
        when(memberships.countByTenant_IdAndStatus(secondTenantId, "ACTIVE")).thenReturn(5L);
        when(invitations.countByTenant_IdAndStatusAndExpiresAtAfter(
                eq(firstTenantId), eq("PENDING"), any(LocalDateTime.class))).thenReturn(1L);
        when(invitations.countByTenant_IdAndStatusAndExpiresAtAfter(
                eq(secondTenantId), eq("PENDING"), any(LocalDateTime.class))).thenReturn(0L);

        SubscriptionUsageService service = new SubscriptionUsageService(
                tenants, projects, memberships, invitations, cacheService);
        SubscriptionUsageResponse expectedFirst = new SubscriptionUsageResponse(
                "FREE", "Free", 3, 3, 2, 1, 3, 3, true, true, Set.of());
        SubscriptionUsageResponse expectedSecond = new SubscriptionUsageResponse(
                "PRO", "Pro", 7, 100, 5, 0, 5, 50, false, false, Set.of("ADVANCED_RBAC"));

        assertEquals(expectedFirst, service.getUsage(firstTenantId));
        assertNull(cacheService.get(secondTenantId));
        assertEquals(expectedSecond, service.getUsage(secondTenantId));

        for (Long tenantId : List.of(firstTenantId, secondTenantId)) {
            verify(tenants).findOneById(tenantId);
            verify(projects).countByTenant_Id(tenantId);
            verify(memberships).countByTenant_IdAndStatus(tenantId, "ACTIVE");
            verify(invitations).countByTenant_IdAndStatusAndExpiresAtAfter(
                    eq(tenantId), eq("PENDING"), any(LocalDateTime.class));
            long ttl = redisTemplate.getExpire(key(tenantId));
            assertTrue(ttl > 0 && ttl <= 300, "New usage entries must expire within five minutes");
        }

        String firstJson = redisTemplate.opsForValue().get(key(firstTenantId));
        String secondJson = redisTemplate.opsForValue().get(key(secondTenantId));
        assertEquals(expectedFirst, objectMapper.readValue(firstJson, SubscriptionUsageResponse.class));
        assertEquals(expectedSecond, objectMapper.readValue(secondJson, SubscriptionUsageResponse.class));

        for (int request = 0; request < 4; request++) {
            assertEquals(expectedFirst, service.getUsage(firstTenantId));
            assertEquals(expectedSecond, service.getUsage(secondTenantId));
        }
        verifyNoMoreInteractions(tenants, projects, memberships, invitations);

        // Shorten only this synthetic key to test expiration without changing the five-minute default.
        assertTrue(redisTemplate.expire(key(firstTenantId), Duration.ofSeconds(2)));
        long ttlBeforeRead = redisTemplate.getExpire(key(firstTenantId), TimeUnit.MILLISECONDS);
        assertEquals(expectedFirst, service.getUsage(firstTenantId));
        long ttlAfterRead = redisTemplate.getExpire(key(firstTenantId), TimeUnit.MILLISECONDS);
        assertTrue(ttlAfterRead > 0 && ttlAfterRead <= ttlBeforeRead,
                "A cache hit must not extend the expiration");
        verifyNoMoreInteractions(tenants, projects, memberships, invitations);

        Thread.sleep(ttlAfterRead + 100);
        assertEquals(-2L, redisTemplate.getExpire(key(firstTenantId)));
        assertNull(cacheService.get(firstTenantId));

        when(projects.countByTenant_Id(firstTenantId)).thenReturn(2L);
        SubscriptionUsageResponse refreshed = new SubscriptionUsageResponse(
                "FREE", "Free", 2, 3, 2, 1, 3, 3, false, true, Set.of());
        assertEquals(refreshed, service.getUsage(firstTenantId));
        assertEquals(refreshed, cacheService.get(firstTenantId));
        verify(tenants, times(2)).findOneById(firstTenantId);
        verify(projects, times(2)).countByTenant_Id(firstTenantId);
        verify(memberships, times(2)).countByTenant_IdAndStatus(firstTenantId, "ACTIVE");
        verify(invitations, times(2)).countByTenant_IdAndStatusAndExpiresAtAfter(
                eq(firstTenantId), eq("PENDING"), any(LocalDateTime.class));
        long refreshedTtl = redisTemplate.getExpire(key(firstTenantId));
        assertTrue(refreshedTtl > 0 && refreshedTtl <= 300);

        cacheService.evict(firstTenantId);
        assertNull(cacheService.get(firstTenantId));
        assertEquals(expectedSecond, cacheService.get(secondTenantId));
    }

    @Test
    void projectChangesEvictOnlyTheAffectedTenantAfterSuccessfulWrites() {
        TenantRepository tenants = mock(TenantRepository.class);
        ProjectRepository projects = mock(ProjectRepository.class);
        TenantMembershipRepository memberships = mock(TenantMembershipRepository.class);
        TenantInvitationRepository invitations = mock(TenantInvitationRepository.class);
        TaskRepository tasks = mock(TaskRepository.class);
        CurrentTenantService currentTenant = mock(CurrentTenantService.class);
        SubscriptionPlan plan = SubscriptionPlan.builder()
                .name("STARTER").displayName("Starter").active(true)
                .maxProjects(20).maxMembers(10).build();
        Tenant tenant = Tenant.builder().id(firstTenantId).plan(plan).build();
        User user = User.builder().id(1L).email("cache-test@example.com").build();
        AtomicLong projectCount = new AtomicLong(2);

        when(tenants.findOneById(firstTenantId)).thenReturn(Optional.of(tenant));
        when(tenants.findOneById(secondTenantId)).thenReturn(Optional.of(
                Tenant.builder().id(secondTenantId).plan(plan).build()));
        when(projects.countByTenant_Id(firstTenantId)).thenAnswer(call -> projectCount.get());
        when(projects.countByTenant_Id(secondTenantId)).thenReturn(2L);
        when(currentTenant.getTenantForUser(user, firstTenantId)).thenReturn(tenant);
        when(projects.save(any(Project.class))).thenAnswer(call -> {
            // The old cache must still exist when the database write begins.
            assertNotNull(cacheService.get(firstTenantId));
            Project project = call.getArgument(0);
            if (project.getId() == null) {
                project.setId(10L);
                projectCount.incrementAndGet();
            }
            return project;
        });
        doAnswer(call -> {
            assertNotNull(cacheService.get(firstTenantId));
            projectCount.decrementAndGet();
            return null;
        }).when(projects).delete(any(Project.class));

        SubscriptionUsageService usageService = new SubscriptionUsageService(
                tenants, projects, memberships, invitations, cacheService);
        SubscriptionLimitService limitService = new SubscriptionLimitService(
                tenants, projects, memberships, invitations);
        CloudPilotEventProducer eventProducer = mock(CloudPilotEventProducer.class);
        ProjectService projectService = new ProjectService(
                projects, tasks, currentTenant, limitService, cacheService, eventProducer);

        SubscriptionUsageResponse original = usageService.getUsage(firstTenantId);
        SubscriptionUsageResponse otherTenant = usageService.getUsage(secondTenantId);
        assertEquals(2, original.currentProjects());
        assertEquals(original, usageService.getUsage(firstTenantId));

        var created = projectService.createProject(
                new CreateProjectRequest("Redis Invalidation Test", "Test"), user, firstTenantId);
        assertNull(cacheService.get(firstTenantId));
        assertEquals(otherTenant, cacheService.get(secondTenantId));
        SubscriptionUsageResponse afterCreate = usageService.getUsage(firstTenantId);
        assertEquals(3, afterCreate.currentProjects());
        assertEquals(afterCreate, usageService.getUsage(firstTenantId));

        Project project = Project.builder().id(created.id()).tenant(tenant)
                .name(created.name()).status("ACTIVE").createdBy(user).build();
        when(projects.findByIdAndTenant_Id(created.id(), firstTenantId))
                .thenReturn(Optional.of(project));
        long ttlBeforeUpdate = redisTemplate.getExpire(key(firstTenantId), TimeUnit.MILLISECONDS);
        projectService.updateProject(created.id(),
                new UpdateProjectRequest("Renamed", "Updated", "ARCHIVED"), firstTenantId);
        assertEquals(afterCreate, cacheService.get(firstTenantId));
        long ttlAfterUpdate = redisTemplate.getExpire(key(firstTenantId), TimeUnit.MILLISECONDS);
        assertTrue(ttlAfterUpdate > 0 && ttlAfterUpdate <= ttlBeforeUpdate);
        assertEquals(afterCreate, usageService.getUsage(firstTenantId));

        when(tasks.existsByProjectId(created.id())).thenReturn(true);
        assertThrows(ProjectHasTasksException.class,
                () -> projectService.deleteProject(created.id(), firstTenantId, user));
        assertEquals(afterCreate, cacheService.get(firstTenantId));
        verify(projects, never()).delete(any(Project.class));
        when(tasks.existsByProjectId(created.id())).thenReturn(false);

        projectService.deleteProject(created.id(), firstTenantId, user);
        assertNull(cacheService.get(firstTenantId));
        assertEquals(otherTenant, cacheService.get(secondTenantId));
        assertEquals(original, usageService.getUsage(firstTenantId));
        assertEquals(original, usageService.getUsage(firstTenantId));
        // Initial load, create quota check, and two reloads; hits and updates do not count again.
        verify(projects, times(4)).countByTenant_Id(firstTenantId);

        doThrow(new IllegalStateException("Database write failed"))
                .when(projects).save(any(Project.class));
        assertThrows(IllegalStateException.class, () -> projectService.createProject(
                new CreateProjectRequest("Failed", "Test"), user, firstTenantId));
        assertEquals(original, cacheService.get(firstTenantId));
        doThrow(new IllegalStateException("Database delete failed")).when(projects).delete(project);
        assertThrows(IllegalStateException.class,
                () -> projectService.deleteProject(created.id(), firstTenantId, user));
        assertEquals(original, cacheService.get(firstTenantId));

        // Even a stale cache showing spare capacity cannot bypass the database quota check.
        projectCount.set(20);
        assertThrows(PlanLimitExceededException.class, () -> projectService.createProject(
                new CreateProjectRequest("Over quota", "Test"), user, firstTenantId));
        assertEquals(original, cacheService.get(firstTenantId));
        assertEquals(otherTenant, cacheService.get(secondTenantId));
    }

    @Test
    void readsExistingPlainJsonAndEvictsOnlyTheMalformedTenantEntry() {
        String json = """
                {"planName":"FREE","displayName":"Free","currentProjects":3,
                 "maxProjects":3,"activeMembers":2,"pendingInvitations":1,
                 "reservedMemberSlots":3,"maxMembers":3,"projectLimitReached":true,
                 "memberLimitReached":true,"features":[]}
                """;
        redisTemplate.opsForValue().set(key(secondTenantId), json);
        assertEquals(new SubscriptionUsageResponse(
                "FREE", "Free", 3, 3, 2, 1, 3, 3, true, true, Set.of()),
                cacheService.get(secondTenantId));

        redisTemplate.opsForValue().set(key(firstTenantId), "{invalid json");
        assertNull(cacheService.get(firstTenantId));
        assertFalse(redisTemplate.hasKey(key(firstTenantId)));
        assertEquals(json, redisTemplate.opsForValue().get(key(secondTenantId)));
    }

    private static String key(Long tenantId) {
        return "cloudpilot:tenant:" + tenantId + ":subscription-usage";
    }

    @TestConfiguration(proxyBeanMethods = false)
    @ImportAutoConfiguration(JacksonAutoConfiguration.class)
    @Import(SubscriptionUsageCacheService.class)
    static class Config {

        @Bean
        LettuceConnectionFactory connectionFactory() {
            return new LettuceConnectionFactory("localhost", 6379);
        }

        @Bean
        StringRedisTemplate redisTemplate(LettuceConnectionFactory connectionFactory) {
            return new StringRedisTemplate(connectionFactory);
        }
    }
}
