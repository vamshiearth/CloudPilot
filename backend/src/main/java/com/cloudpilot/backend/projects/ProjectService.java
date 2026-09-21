package com.cloudpilot.backend.projects;

import com.cloudpilot.backend.events.CloudPilotEvent;
import com.cloudpilot.backend.events.CloudPilotEventProducer;
import com.cloudpilot.backend.events.CloudPilotEventType;
import com.cloudpilot.backend.events.ProjectCreatedEventData;
import com.cloudpilot.backend.events.ProjectDeletedEventData;
import com.cloudpilot.backend.exception.ProjectHasTasksException;
import com.cloudpilot.backend.exception.ProjectNotFoundException;
import com.cloudpilot.backend.tasks.TaskRepository;
import com.cloudpilot.backend.tenants.CurrentTenantService;
import com.cloudpilot.backend.tenants.Tenant;
import com.cloudpilot.backend.users.User;
import com.cloudpilot.backend.subscriptions.SubscriptionLimitService;
import com.cloudpilot.backend.subscriptions.SubscriptionUsageCacheService;
import org.springframework.stereotype.Service;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final CurrentTenantService currentTenantService;
    private final SubscriptionLimitService subscriptionLimitService;
    private final SubscriptionUsageCacheService subscriptionUsageCacheService;
    private final CloudPilotEventProducer eventProducer;

    public ProjectService(
            ProjectRepository projectRepository,
            TaskRepository taskRepository,
            CurrentTenantService currentTenantService,
            SubscriptionLimitService subscriptionLimitService,
            SubscriptionUsageCacheService subscriptionUsageCacheService,
            CloudPilotEventProducer eventProducer) {

        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
        this.currentTenantService = currentTenantService;
        this.subscriptionLimitService = subscriptionLimitService;
        this.subscriptionUsageCacheService = subscriptionUsageCacheService;
        this.eventProducer = eventProducer;
    }

    @PreAuthorize("hasAuthority('PROJECT_CREATE')")
    public ProjectResponse createProject(
            CreateProjectRequest request,
            User currentUser,
            Long tenantId) {

        subscriptionLimitService.validateProjectCreation(tenantId);

            Tenant tenant =
            currentTenantService.getTenantForUser(
                currentUser,
                tenantId
            );

        Project project = Project.builder()
                .tenant(tenant)
                .name(request.name().trim())
                .description(request.description())
                .status("ACTIVE")
                .createdBy(currentUser)
                .build();

        Project savedProject =
                projectRepository.save(project);

        subscriptionUsageCacheService.evict(tenantId);

        ProjectCreatedEventData eventData =
            new ProjectCreatedEventData(
                savedProject.getId(),
                savedProject.getName()
            );

        CloudPilotEvent event =
            CloudPilotEvent.create(
                CloudPilotEventType.PROJECT_CREATED,
                tenantId,
                currentUser.getId(),
                eventData
            );

        eventProducer.publish(event);

        return ProjectResponse.from(savedProject);
    }

    @PreAuthorize("hasAuthority('PROJECT_READ')")
    public List<ProjectResponse> getAllProjects(Long tenantId) {

        return projectRepository.findAllByTenant_Id(tenantId)
                .stream()
                .map(ProjectResponse::from)
                .toList();
    }

    @PreAuthorize("hasAuthority('PROJECT_READ')")
    public ProjectResponse getProjectById(
            Long id,
            Long tenantId) {

        Project project = findProjectForTenant(id, tenantId);

        return ProjectResponse.from(project);
    }

    @PreAuthorize("hasAuthority('PROJECT_UPDATE')")
    public ProjectResponse updateProject(
        Long id,
        UpdateProjectRequest request,
        Long tenantId) {

    Project project = findProjectForTenant(id, tenantId);

    project.setName(request.name().trim());
    project.setDescription(request.description());
    project.setStatus(request.status().trim().toUpperCase());

    Project updatedProject =
        projectRepository.save(project);

    return ProjectResponse.from(updatedProject);
    }

    @PreAuthorize("hasAuthority('PROJECT_DELETE')")
    public void deleteProject(
            Long id,
            Long tenantId,
            User currentUser) {

        Project project = findProjectForTenant(id, tenantId);

        Long deletedProjectId = project.getId();
        String deletedProjectName = project.getName();

                if (taskRepository.existsByProjectId(id)) {
                    throw new ProjectHasTasksException(
                        "Project cannot be deleted while it still contains tasks"
                    );
                }

        projectRepository.delete(project);
        subscriptionUsageCacheService.evict(tenantId);

        ProjectDeletedEventData eventData =
            new ProjectDeletedEventData(
                deletedProjectId,
                deletedProjectName
            );

        CloudPilotEvent event =
            CloudPilotEvent.create(
                CloudPilotEventType.PROJECT_DELETED,
                tenantId,
                currentUser.getId(),
                eventData
            );

        eventProducer.publish(event);
    }

        private Project findProjectForTenant(
            Long projectId,
            Long tenantId) {

        return projectRepository
            .findByIdAndTenant_Id(projectId, tenantId)
            .orElseThrow(() ->
                new ProjectNotFoundException(
                    "Project not found with id: " + projectId
                )
            );
        }
}
