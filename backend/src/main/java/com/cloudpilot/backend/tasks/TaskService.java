package com.cloudpilot.backend.tasks;

import com.cloudpilot.backend.exception.ProjectNotFoundException;
import com.cloudpilot.backend.exception.TaskNotFoundException;
import com.cloudpilot.backend.projects.Project;
import com.cloudpilot.backend.projects.ProjectRepository;
import com.cloudpilot.backend.users.User;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;

    public TaskService(
            TaskRepository taskRepository,
            ProjectRepository projectRepository) {

        this.taskRepository = taskRepository;
        this.projectRepository = projectRepository;
    }

        @PreAuthorize("hasAuthority('TASK_CREATE')")
        public TaskResponse createTask(
            Long projectId,
            CreateTaskRequest request,
            User currentUser,
            Long tenantId) {

        Project project = projectRepository
                .findByIdAndTenant_Id(projectId, tenantId)
                .orElseThrow(() ->
                        new ProjectNotFoundException(
                                "Project not found with id: " + projectId
                        )
                );

        String priority =
                request.priority() == null ||
                request.priority().isBlank()
                        ? "MEDIUM"
                        : request.priority().trim().toUpperCase();

        Task task = Task.builder()
                .tenant(project.getTenant())
                .project(project)
                .title(request.title().trim())
                .description(request.description())
                .status("TODO")
                .priority(priority)
                .assignedTo(null)
                .createdBy(currentUser)
                .dueDate(request.dueDate())
                .build();

        Task savedTask = taskRepository.save(task);

        return TaskResponse.from(savedTask);
    }

        @PreAuthorize("hasAuthority('TASK_READ')")
        public List<TaskResponse> getTasksByProject(
            Long projectId,
            Long tenantId) {

        projectRepository
                .findByIdAndTenant_Id(projectId, tenantId)
                .orElseThrow(() ->
                        new ProjectNotFoundException(
                                "Project not found with id: " + projectId
                        )
                );

        return taskRepository
                .findAllByProject_IdAndTenant_Id(projectId, tenantId)
                .stream()
                .map(TaskResponse::from)
                .toList();
    }

        @PreAuthorize("hasAuthority('TASK_READ')")
        public TaskResponse getTaskById(
            Long id,
            Long tenantId) {

        Task task = findTaskForTenant(id, tenantId);

        return TaskResponse.from(task);
    }

        @PreAuthorize("hasAuthority('TASK_UPDATE')")
        public TaskResponse updateTask(
            Long id,
            UpdateTaskRequest request,
            Long tenantId) {

        Task task = findTaskForTenant(id, tenantId);

        task.setTitle(request.title().trim());
        task.setDescription(request.description());
        task.setStatus(request.status().trim().toUpperCase());

        if (request.priority() != null &&
                !request.priority().isBlank()) {

            task.setPriority(
                    request.priority().trim().toUpperCase()
            );
        }

        task.setDueDate(request.dueDate());

        Task updatedTask = taskRepository.save(task);

        return TaskResponse.from(updatedTask);
    }

        @PreAuthorize("hasAuthority('TASK_DELETE')")
        public void deleteTask(
            Long id,
            Long tenantId) {

        Task task = findTaskForTenant(id, tenantId);

        taskRepository.delete(task);
    }

    private Task findTaskForTenant(
            Long taskId,
            Long tenantId) {

        return taskRepository
                .findByIdAndTenant_Id(taskId, tenantId)
                .orElseThrow(() ->
                        new TaskNotFoundException(
                                "Task not found with id: " + taskId
                        )
                );
    }
}
