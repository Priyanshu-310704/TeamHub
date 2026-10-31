package com.teamhub.teamhub_backend.team.dto;

import com.teamhub.teamhub_backend.team.entity.TeamMembership;
import com.teamhub.teamhub_backend.team.entity.TeamRole;

import java.time.Instant;
import java.util.UUID;

public record TeamMemberResponse(
        UUID membershipId,
        UUID userId,
        String name,
        String email,
        String avatarUrl,
        TeamRole role,
        Instant joinedAt
) {
    public static TeamMemberResponse fromEntity(TeamMembership membership) {
        return new TeamMemberResponse(
                membership.getId(),
                membership.getUser().getId(),
                membership.getUser().getName(),
                membership.getUser().getEmail(),
                membership.getUser().getAvatarUrl(),
                membership.getRole(),
                membership.getJoinedAt()
        );
    }
}