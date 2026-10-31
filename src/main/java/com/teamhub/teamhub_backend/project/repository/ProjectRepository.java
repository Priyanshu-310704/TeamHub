package com.teamhub.teamhub_backend.project.repository;

import com.teamhub.teamhub_backend.project.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProjectRepository extends JpaRepository<Project, UUID> {

    boolean existsByTeamIdAndProjectKey(UUID teamId, String projectKey);

    List<Project> findAllByTeamIdOrderByCreatedAtDesc(UUID teamId);
}