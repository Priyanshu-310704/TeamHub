package com.teamhub.teamhub_backend.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AiBoardFilterRequest(
        @NotBlank(message = "Natural language query is required")
        @Size(max = 500, message = "Query must be at most 500 characters")
        String query
) {}