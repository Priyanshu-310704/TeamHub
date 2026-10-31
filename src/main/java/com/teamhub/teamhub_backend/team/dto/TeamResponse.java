package com.teamhub.teamhub_backend.team.dto;

import com.teamhub.teamhub_backend.team.entity.Team;
import com.teamhub.teamhub_backend.team.entity.TeamRole;

import java.time.Instant;
import java.util.UUID;

public record TeamResponse(
        UUID id,
        String name,
        String description,
        UUID createdById,
        TeamRole myRole,
        Instant createdAt
) {
    public static TeamResponse from(Team team, TeamRole myRole) {
        return new TeamResponse(
                team.getId(),
                team.getName(),
                team.getDescription(),
                team.getCreatedBy().getId(),
                myRole,
                team.getCreatedAt()
        );
    }
}