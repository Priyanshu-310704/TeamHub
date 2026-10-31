package com.teamhub.teamhub_backend.task.repository;

import com.teamhub.teamhub_backend.task.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task, UUID>, JpaSpecificationExecutor<Task> {

    long countByProjectId(UUID projectId);

    boolean existsByProjectIdAndTaskKey(UUID projectId, String taskKey);
}