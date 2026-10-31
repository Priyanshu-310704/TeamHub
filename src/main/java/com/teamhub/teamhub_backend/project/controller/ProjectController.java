package com.teamhub.teamhub_backend.project.controller;

import com.teamhub.teamhub_backend.project.dto.CreateProjectRequest;
import com.teamhub.teamhub_backend.project.dto.ProjectResponse;
import com.teamhub.teamhub_backend.project.dto.UpdateProjectRequest;
import com.teamhub.teamhub_backend.project.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @PostMapping("/teams/{teamId}/projects")
    public ResponseEntity<ProjectResponse> createProject(
            @PathVariable UUID teamId,
            @Valid @RequestBody CreateProjectRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(projectService.createProject(teamId, request));
    }

    @GetMapping("/teams/{teamId}/projects")
    public ResponseEntity<List<ProjectResponse>> getTeamProjects(@PathVariable UUID teamId) {
        return ResponseEntity.ok(projectService.getTeamProjects(teamId));
    }

    @GetMapping("/projects/{projectId}")
    public ResponseEntity<ProjectResponse> getProjectById(@PathVariable UUID projectId) {
        return ResponseEntity.ok(projectService.getProjectById(projectId));
    }

    @PatchMapping("/projects/{projectId}")
    public ResponseEntity<ProjectResponse> updateProject(
            @PathVariable UUID projectId,
            @Valid @RequestBody UpdateProjectRequest request
    ) {
        return ResponseEntity.ok(projectService.updateProject(projectId, request));
    }

    @DeleteMapping("/projects/{projectId}")
    public ResponseEntity<Void> deleteProject(@PathVariable UUID projectId) {
        projectService.deleteProject(projectId);
        return ResponseEntity.noContent().build();
    }
}