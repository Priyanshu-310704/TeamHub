package com.teamhub.teamhub_backend.task.dto;

import com.teamhub.teamhub_backend.task.entity.TaskPriority;
import com.teamhub.teamhub_backend.task.entity.TaskStatus;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public record UpdateTaskRequest(
        @Size(min = 1, max = 200, message = "Title must be between 1 and 200 characters")
        String title,

        @Size(max = 5000, message = "Description must be at most 5000 characters")
        String description,

        TaskStatus status,

        TaskPriority priority,

        UUID assigneeId,

        Boolean unassign,

        LocalDate dueDate,

        Integer position
) {}