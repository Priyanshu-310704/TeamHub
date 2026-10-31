package com.teamhub.teamhub_backend.task.dto;

import com.teamhub.teamhub_backend.task.entity.Task;
import com.teamhub.teamhub_backend.task.entity.TaskPriority;
import com.teamhub.teamhub_backend.task.entity.TaskStatus;
import com.teamhub.teamhub_backend.user.entity.User;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record TaskResponse(
        UUID id,
        UUID projectId,
        String taskKey,
        String title,
        String description,
        TaskStatus status,
        TaskPriority priority,
        TaskUserSummary assignee,
        TaskUserSummary reporter,
        LocalDate dueDate,
        Integer position,
        Instant createdAt,
        Instant updatedAt
) {
    public record TaskUserSummary(UUID id, String name, String email, String avatarUrl) {
        public static TaskUserSummary from(User user) {
            if (user == null) return null;
            return new TaskUserSummary(user.getId(), user.getName(), user.getEmail(), user.getAvatarUrl());
        }
    }

    public static TaskResponse fromEntity(Task task) {
        return new TaskResponse(
                task.getId(),
                task.getProject().getId(),
                task.getTaskKey(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getPriority(),
                TaskUserSummary.from(task.getAssignee()),
                TaskUserSummary.from(task.getReporter()),
                task.getDueDate(),
                task.getPosition(),
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }
}