package com.cloudpilot.backend.tasks;

import com.cloudpilot.backend.auth.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping("/projects/{projectId}/tasks")
    public ResponseEntity<TaskResponse> createTask(
            @PathVariable Long projectId,
            @Valid @RequestBody CreateTaskRequest request,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {

        TaskResponse task = taskService.createTask(
                projectId,
                request,
                authenticatedUser.user(),
                authenticatedUser.tenantId()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(task);
    }

    @GetMapping("/projects/{projectId}/tasks")
    public ResponseEntity<List<TaskResponse>> getTasksByProject(
            @PathVariable Long projectId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {

        List<TaskResponse> tasks = taskService.getTasksByProject(
                projectId,
                authenticatedUser.tenantId()
        );

        return ResponseEntity.ok(tasks);
    }

    @GetMapping("/tasks/{id}")
    public ResponseEntity<TaskResponse> getTaskById(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {

        TaskResponse task = taskService.getTaskById(
                id,
                authenticatedUser.tenantId()
        );

        return ResponseEntity.ok(task);
    }

    @PutMapping("/tasks/{id}")
    public ResponseEntity<TaskResponse> updateTask(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTaskRequest request,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {

        TaskResponse task = taskService.updateTask(
                id,
                request,
                authenticatedUser.tenantId()
        );

        return ResponseEntity.ok(task);
    }

    @DeleteMapping("/tasks/{id}")
    public ResponseEntity<Void> deleteTask(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {

        taskService.deleteTask(
                id,
                authenticatedUser.tenantId()
        );

        return ResponseEntity.noContent().build();
    }
}
