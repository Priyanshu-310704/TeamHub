package com.teamhub.teamhub_backend.project.service;

import com.teamhub.teamhub_backend.common.exception.ConflictException;
import com.teamhub.teamhub_backend.common.exception.ResourceNotFoundException;
import com.teamhub.teamhub_backend.project.dto.CreateProjectRequest;
import com.teamhub.teamhub_backend.project.dto.ProjectResponse;
import com.teamhub.teamhub_backend.project.dto.UpdateProjectRequest;
import com.teamhub.teamhub_backend.project.entity.Project;
import com.teamhub.teamhub_backend.project.entity.ProjectStatus;
import com.teamhub.teamhub_backend.project.repository.ProjectRepository;
import com.teamhub.teamhub_backend.team.entity.Team;
import com.teamhub.teamhub_backend.team.service.TeamService;
import com.teamhub.teamhub_backend.user.entity.User;
import com.teamhub.teamhub_backend.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final TeamService teamService;
    private final UserService userService;

    @Transactional
    public ProjectResponse createProject(UUID teamId, CreateProjectRequest request) {
        User currentUser = userService.getCurrentAuthenticatedUser();
        Team team = teamService.getTeamEntityById(teamId);

        // Only team ADMIN or OWNER can create a new project
        teamService.verifyAdminOrOwner(teamId, currentUser.getId());

        String normalizedKey = request.projectKey().trim().toUpperCase();
        if (projectRepository.existsByTeamIdAndProjectKey(teamId, normalizedKey)) {
            throw new ConflictException("Project key '" + normalizedKey + "' already exists in this team");
        }

        Project project = Project.builder()
                .team(team)
                .name(request.name().trim())
                .projectKey(normalizedKey)
                .description(request.description() != null ? request.description().trim() : null)
                .status(request.status() != null ? request.status() : ProjectStatus.ACTIVE)
                .createdBy(currentUser)
                .build();

        Project savedProject = projectRepository.save(project);
        return ProjectResponse.fromEntity(savedProject);
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> getTeamProjects(UUID teamId) {
        User currentUser = userService.getCurrentAuthenticatedUser();
        teamService.getTeamEntityById(teamId);
        teamService.verifyMembership(teamId, currentUser.getId());

        return projectRepository.findAllByTeamIdOrderByCreatedAtDesc(teamId)
                .stream()
                .map(ProjectResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProjectResponse getProjectById(UUID projectId) {
        User currentUser = userService.getCurrentAuthenticatedUser();
        Project project = getProjectAndVerifyMembership(projectId, currentUser.getId());
        return ProjectResponse.fromEntity(project);
    }

    @Transactional
    public ProjectResponse updateProject(UUID projectId, UpdateProjectRequest request) {
        User currentUser = userService.getCurrentAuthenticatedUser();
        Project project = getProjectEntityById(projectId);

        // Only ADMIN or OWNER of the project's team can update project settings
        teamService.verifyAdminOrOwner(project.getTeam().getId(), currentUser.getId());

        if (request.name() != null && !request.name().isBlank()) {
            project.setName(request.name().trim());
        }
        if (request.description() != null) {
            project.setDescription(request.description().trim());
        }
        if (request.status() != null) {
            project.setStatus(request.status());
        }

        Project updated = projectRepository.save(project);
        return ProjectResponse.fromEntity(updated);
    }

    @Transactional
    public void deleteProject(UUID projectId) {
        User currentUser = userService.getCurrentAuthenticatedUser();
        Project project = getProjectEntityById(projectId);

        teamService.verifyAdminOrOwner(project.getTeam().getId(), currentUser.getId());
        projectRepository.delete(project);
    }

    // =========================================================================
    // REUSABLE HELPERS FOR TASK MODULE (PHASE 5)
    // =========================================================================

    @Transactional(readOnly = true)
    public Project getProjectEntityById(UUID projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + projectId));
    }

    @Transactional(readOnly = true)
    public Project getProjectAndVerifyMembership(UUID projectId, UUID userId) {
        Project project = getProjectEntityById(projectId);
        teamService.verifyMembership(project.getTeam().getId(), userId);
        return project;
    }
}