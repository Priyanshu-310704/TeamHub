package com.teamhub.teamhub_backend.team.service;

import com.teamhub.teamhub_backend.common.exception.ConflictException;
import com.teamhub.teamhub_backend.common.exception.ForbiddenAccessException;
import com.teamhub.teamhub_backend.common.exception.ResourceNotFoundException;
import com.teamhub.teamhub_backend.team.dto.AddTeamMemberRequest;
import com.teamhub.teamhub_backend.team.dto.CreateTeamRequest;
import com.teamhub.teamhub_backend.team.dto.TeamMemberResponse;
import com.teamhub.teamhub_backend.team.dto.TeamResponse;
import com.teamhub.teamhub_backend.team.entity.Team;
import com.teamhub.teamhub_backend.team.entity.TeamMembership;
import com.teamhub.teamhub_backend.team.entity.TeamRole;
import com.teamhub.teamhub_backend.team.repository.TeamMembershipRepository;
import com.teamhub.teamhub_backend.team.repository.TeamRepository;
import com.teamhub.teamhub_backend.user.entity.User;
import com.teamhub.teamhub_backend.user.repository.UserRepository;
import com.teamhub.teamhub_backend.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TeamService {

    private final TeamRepository teamRepository;
    private final TeamMembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final UserService userService;

    @Transactional
    public TeamResponse createTeam(CreateTeamRequest request) {
        User currentUser = userService.getCurrentAuthenticatedUser();

        Team team = Team.builder()
                .name(request.name().trim())
                .description(request.description() != null ? request.description().trim() : null)
                .createdBy(currentUser)
                .build();

        Team savedTeam = teamRepository.save(team);

        TeamMembership ownerMembership = TeamMembership.builder()
                .team(savedTeam)
                .user(currentUser)
                .role(TeamRole.OWNER)
                .build();

        membershipRepository.save(ownerMembership);

        return TeamResponse.from(savedTeam, TeamRole.OWNER);
    }

    @Transactional(readOnly = true)
    public List<TeamResponse> getMyTeams() {
        User currentUser = userService.getCurrentAuthenticatedUser();
        return membershipRepository.findAllByUserIdWithTeam(currentUser.getId())
                .stream()
                .map(tm -> TeamResponse.from(tm.getTeam(), tm.getRole()))
                .toList();
    }

    @Transactional(readOnly = true)
    public TeamResponse getTeamById(UUID teamId) {
        User currentUser = userService.getCurrentAuthenticatedUser();
        Team team = getTeamEntityById(teamId);
        TeamMembership membership = verifyMembership(teamId, currentUser.getId());
        return TeamResponse.from(team, membership.getRole());
    }

    @Transactional
    public TeamMemberResponse addMember(UUID teamId, AddTeamMemberRequest request) {
        User currentUser = userService.getCurrentAuthenticatedUser();
        Team team = getTeamEntityById(teamId);

        // Only OWNER or ADMIN can add members
        verifyAdminOrOwner(teamId, currentUser.getId());

        if (request.role() == TeamRole.OWNER) {
            throw new ConflictException("Cannot assign OWNER role when adding a member");
        }

        String normalizedEmail = request.email().trim().toLowerCase();
        User userToAdd = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ResourceNotFoundException("No user found with email: " + normalizedEmail));

        if (membershipRepository.existsByTeamIdAndUserId(teamId, userToAdd.getId())) {
            throw new ConflictException("User '" + normalizedEmail + "' is already a member of this team");
        }

        TeamMembership membership = TeamMembership.builder()
                .team(team)
                .user(userToAdd)
                .role(request.role())
                .build();

        TeamMembership saved = membershipRepository.save(membership);
        return TeamMemberResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<TeamMemberResponse> getTeamMembers(UUID teamId) {
        User currentUser = userService.getCurrentAuthenticatedUser();
        getTeamEntityById(teamId);
        verifyMembership(teamId, currentUser.getId());

        return membershipRepository.findAllByTeamIdWithUser(teamId)
                .stream()
                .map(TeamMemberResponse::fromEntity)
                .toList();
    }

    @Transactional
    public void removeMember(UUID teamId, UUID targetUserId) {
        User currentUser = userService.getCurrentAuthenticatedUser();
        getTeamEntityById(teamId);
        verifyAdminOrOwner(teamId, currentUser.getId());

        TeamMembership targetMembership = membershipRepository.findByTeamIdAndUserId(teamId, targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User is not a member of this team"));

        if (targetMembership.getRole() == TeamRole.OWNER) {
            throw new ConflictException("The team OWNER cannot be removed from the team");
        }

        membershipRepository.delete(targetMembership);
    }

    // =========================================================================
    // REUSABLE PERMISSION & ENTITY HELPERS FOR PROJECT & TASK MODULES
    // =========================================================================

    @Transactional(readOnly = true)
    public Team getTeamEntityById(UUID teamId) {
        return teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team not found with id: " + teamId));
    }

    @Transactional(readOnly = true)
    public TeamMembership verifyMembership(UUID teamId, UUID userId) {
        return membershipRepository.findByTeamIdAndUserId(teamId, userId)
                .orElseThrow(() -> new ForbiddenAccessException("You are not a member of this team"));
    }

    @Transactional(readOnly = true)
    public TeamMembership verifyAdminOrOwner(UUID teamId, UUID userId) {
        TeamMembership membership = verifyMembership(teamId, userId);
        if (membership.getRole() != TeamRole.OWNER && membership.getRole() != TeamRole.ADMIN) {
            throw new ForbiddenAccessException("Only team OWNER or ADMIN can perform this action");
        }
        return membership;
    }
}