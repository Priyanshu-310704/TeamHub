package com.teamhub.teamhub_backend.task.dto;

import com.teamhub.teamhub_backend.task.entity.TaskPriority;
import com.teamhub.teamhub_backend.task.entity.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public record CreateTaskRequest(
        @NotBlank(message = "Task title is required")
        @Size(max = 200, message = "Title must be at most 200 characters")
        String title,

        @Size(max = 5000, message = "Description must be at most 5000 characters")
        String description,

        TaskStatus status,

        TaskPriority priority,

        UUID assigneeId,

        LocalDate dueDate,

        Integer position
) {}