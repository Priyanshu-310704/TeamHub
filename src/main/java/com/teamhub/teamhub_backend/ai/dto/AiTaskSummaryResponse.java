package com.teamhub.teamhub_backend.ai.dto;

import java.util.List;

public record AiTaskSummaryResponse(
        String tldr,
        List<String> keyDecisions,
        List<String> openBlockers,
        List<String> nextSteps
) {}