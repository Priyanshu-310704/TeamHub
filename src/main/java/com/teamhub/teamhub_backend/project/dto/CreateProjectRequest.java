package com.teamhub.teamhub_backend.project.dto;

import com.teamhub.teamhub_backend.project.entity.ProjectStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateProjectRequest(
        @NotBlank(message = "Project name is required")
        @Size(max = 120, message = "Project name must be at most 120 characters")
        String name,

        @NotBlank(message = "Project key is required (e.g., TH, CORE)")
        @Pattern(
                regexp = "^[A-Z][A-Z0-9]{1,9}$",
                message = "Project key must be 2-10 uppercase alphanumeric characters starting with a letter"
        )
        String projectKey,

        @Size(max = 2000, message = "Description must be at most 2000 characters")
        String description,

        ProjectStatus status
) {}