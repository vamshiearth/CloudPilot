package com.cloudpilot.backend.projects;

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
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @PostMapping
    public ResponseEntity<ProjectResponse> createProject(
            @Valid @RequestBody CreateProjectRequest request,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {

        ProjectResponse project = projectService.createProject(
                request,
                authenticatedUser.user(),
                authenticatedUser.tenantId()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(project);
    }

    @GetMapping
    public ResponseEntity<List<ProjectResponse>> getAllProjects(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {

        List<ProjectResponse> projects = projectService.getAllProjects(
                authenticatedUser.tenantId()
        );

        return ResponseEntity.ok(projects);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProjectResponse> getProjectById(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {

        ProjectResponse project = projectService.getProjectById(
                id,
                authenticatedUser.tenantId()
        );

        return ResponseEntity.ok(project);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProjectResponse> updateProject(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProjectRequest request,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {

        ProjectResponse project = projectService.updateProject(
                id,
                request,
                authenticatedUser.tenantId()
        );

        return ResponseEntity.ok(project);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProject(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {

        projectService.deleteProject(
                id,
                authenticatedUser.tenantId(),
                authenticatedUser.user()
        );

        return ResponseEntity.noContent().build();
    }
}
