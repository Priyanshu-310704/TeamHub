package com.teamhub.teamhub_backend.team.dto;

import com.teamhub.teamhub_backend.team.entity.TeamRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AddTeamMemberRequest(
        @NotBlank(message = "Member email is required")
        @Email(message = "Provide a valid email address")
        String email,

        @NotNull(message = "Role is required (ADMIN or MEMBER)")
        TeamRole role
) {}