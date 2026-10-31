package com.teamhub.teamhub_backend.team.repository;

import com.teamhub.teamhub_backend.team.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TeamRepository extends JpaRepository<Team, UUID> {
}