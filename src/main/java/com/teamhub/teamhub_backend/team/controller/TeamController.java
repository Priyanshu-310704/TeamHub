package com.teamhub.teamhub_backend.team.controller;

import com.teamhub.teamhub_backend.team.dto.AddTeamMemberRequest;
import com.teamhub.teamhub_backend.team.dto.CreateTeamRequest;
import com.teamhub.teamhub_backend.team.dto.TeamMemberResponse;
import com.teamhub.teamhub_backend.team.dto.TeamResponse;
import com.teamhub.teamhub_backend.team.service.TeamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/teams")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;

    @PostMapping
    public ResponseEntity<TeamResponse> createTeam(@Valid @RequestBody CreateTeamRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(teamService.createTeam(request));
    }

    @GetMapping
    public ResponseEntity<List<TeamResponse>> getMyTeams() {
        return ResponseEntity.ok(teamService.getMyTeams());
    }

    @GetMapping("/{teamId}")
    public ResponseEntity<TeamResponse> getTeamById(@PathVariable UUID teamId) {
        return ResponseEntity.ok(teamService.getTeamById(teamId));
    }

    @PostMapping("/{teamId}/members")
    public ResponseEntity<TeamMemberResponse> addMember(
            @PathVariable UUID teamId,
            @Valid @RequestBody AddTeamMemberRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(teamService.addMember(teamId, request));
    }

    @GetMapping("/{teamId}/members")
    public ResponseEntity<List<TeamMemberResponse>> getTeamMembers(@PathVariable UUID teamId) {
        return ResponseEntity.ok(teamService.getTeamMembers(teamId));
    }

    @DeleteMapping("/{teamId}/members/{userId}")
    public ResponseEntity<Void> removeMember(
            @PathVariable UUID teamId,
            @PathVariable UUID userId
    ) {
        teamService.removeMember(teamId, userId);
        return ResponseEntity.noContent().build();
    }
}