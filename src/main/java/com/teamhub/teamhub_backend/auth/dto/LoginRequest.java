package com.teamhub.teamhub_backend.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Provide a valid email address")
        String email,

        @NotBlank(message = "Password is required")
        String password
) {}