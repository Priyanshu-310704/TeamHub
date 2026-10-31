package com.teamhub.teamhub_backend.project.dto;

import com.teamhub.teamhub_backend.project.entity.ProjectStatus;
import jakarta.validation.constraints.Size;

public record UpdateProjectRequest(
        @Size(min = 1, max = 120, message = "Project name must be between 1 and 120 characters")
        String name,

        @Size(max = 2000, message = "Description must be at most 2000 characters")
        String description,

        ProjectStatus status
) {}