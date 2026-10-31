package com.teamhub.teamhub_backend.team.repository;

import com.teamhub.teamhub_backend.team.entity.TeamMembership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TeamMembershipRepository extends JpaRepository<TeamMembership, UUID> {

    Optional<TeamMembership> findByTeamIdAndUserId(UUID teamId, UUID userId);

    boolean existsByTeamIdAndUserId(UUID teamId, UUID userId);

    @Query("""
            SELECT tm FROM TeamMembership tm
            JOIN FETCH tm.team t
            WHERE tm.user.id = :userId
            ORDER BY t.createdAt DESC
            """)
    List<TeamMembership> findAllByUserIdWithTeam(@Param("userId") UUID userId);

    @Query("""
            SELECT tm FROM TeamMembership tm
            JOIN FETCH tm.user u
            WHERE tm.team.id = :teamId
            ORDER BY tm.joinedAt ASC
            """)
    List<TeamMembership> findAllByTeamIdWithUser(@Param("teamId") UUID teamId);
}