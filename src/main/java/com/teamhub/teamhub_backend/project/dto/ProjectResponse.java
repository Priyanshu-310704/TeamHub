package com.teamhub.teamhub_backend.project.dto;

import com.teamhub.teamhub_backend.project.entity.Project;
import com.teamhub.teamhub_backend.project.entity.ProjectStatus;

import java.time.Instant;
import java.util.UUID;

public record ProjectResponse(
        UUID id,
        UUID teamId,
        String name,
        String projectKey,
        String description,
        ProjectStatus status,
        UUID createdById,
        Instant createdAt,
        Instant updatedAt
) {
    public static ProjectResponse fromEntity(Project project) {
        return new ProjectResponse(
                project.getId(),
                project.getTeam().getId(),
                project.getName(),
                project.getProjectKey(),
                project.getDescription(),
                project.getStatus(),
                project.getCreatedBy().getId(),
                project.getCreatedAt(),
                project.getUpdatedAt()
        );
    }
}