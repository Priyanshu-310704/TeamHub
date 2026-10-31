package com.teamhub.teamhub_backend.ai.dto;

import com.teamhub.teamhub_backend.task.dto.TaskResponse;
import com.teamhub.teamhub_backend.task.entity.TaskPriority;
import com.teamhub.teamhub_backend.task.entity.TaskStatus;

import java.util.List;

public record AiBoardFilterResponse(
        ExtractedFilter extractedFilter,
        List<TaskResponse> tasks
) {
    public record ExtractedFilter(
            TaskStatus status,
            TaskPriority priority,
            String searchKeyword
    ) {}
}