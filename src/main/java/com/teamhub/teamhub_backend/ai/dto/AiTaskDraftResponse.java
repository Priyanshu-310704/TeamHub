package com.teamhub.teamhub_backend.ai.dto;

import com.teamhub.teamhub_backend.task.entity.TaskPriority;

import java.util.List;

public record AiTaskDraftResponse(
        String suggestedTitle,
        String description,
        List<String> acceptanceCriteria,
        TaskPriority suggestedPriority
) {}