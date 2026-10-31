package com.teamhub.teamhub_backend.ai.controller;

import com.teamhub.teamhub_backend.ai.dto.*;
import com.teamhub.teamhub_backend.ai.service.AiService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    @PostMapping("/projects/{projectId}/ai/task-draft")
    public ResponseEntity<AiTaskDraftResponse> generateTaskDraft(
            @PathVariable UUID projectId,
            @Valid @RequestBody AiTaskDraftRequest request
    ) {
        return ResponseEntity.ok(aiService.generateTaskDraft(projectId, request));
    }

    @PostMapping("/tasks/{taskId}/ai/summarize")
    public ResponseEntity<AiTaskSummaryResponse> summarizeTaskDiscussion(@PathVariable UUID taskId) {
        return ResponseEntity.ok(aiService.summarizeTaskDiscussion(taskId));
    }

    @PostMapping("/projects/{projectId}/ai/filter-tasks")
    public ResponseEntity<AiBoardFilterResponse> filterBoardByNaturalLanguage(
            @PathVariable UUID projectId,
            @Valid @RequestBody AiBoardFilterRequest request
    ) {
        return ResponseEntity.ok(aiService.filterBoardByNaturalLanguage(projectId, request));
    }
}