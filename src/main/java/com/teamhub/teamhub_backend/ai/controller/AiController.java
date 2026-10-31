package com.teamhub.teamhub_backend.ai.controller;

import com.teamhub.teamhub_backend.ai.dto.*;
import com.teamhub.teamhub_backend.ai.service.AiService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    // =========================================================================
    // EXISTING ENDPOINTS (synchronous, structured JSON responses - preserved)
    // =========================================================================

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

    // =========================================================================
    // NEW STREAMING ENDPOINTS (SSE - progressive text delivery for React)
    // =========================================================================

    /**
     * Streams task draft generation as Server-Sent Events.
     * React frontend can consume via EventSource or fetch() with ReadableStream.
     *
     * Usage: POST /api/projects/{id}/ai/task-draft/stream
     * Response: text/event-stream with data chunks as they are generated.
     */
    @PostMapping(value = "/projects/{projectId}/ai/task-draft/stream",
                 produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamTaskDraft(
            @PathVariable UUID projectId,
            @Valid @RequestBody AiTaskDraftRequest request
    ) {
        log.info("[AI-STREAM] Starting streaming task draft for project: {}", projectId);
        return aiService.streamTaskDraft(projectId, request)
                .doOnComplete(() -> log.info("[AI-STREAM] Task draft stream completed for project: {}", projectId))
                .doOnCancel(() -> log.info("[AI-STREAM] Task draft stream cancelled by client for project: {}", projectId))
                .doOnError(e -> log.warn("[AI-STREAM] Task draft stream error for project {}: {}", projectId, e.getMessage()));
    }

    /**
     * Streams task discussion summary as Server-Sent Events.
     *
     * Usage: POST /api/tasks/{id}/ai/summarize/stream
     * Response: text/event-stream with data chunks as they are generated.
     */
    @PostMapping(value = "/tasks/{taskId}/ai/summarize/stream",
                 produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamTaskSummary(@PathVariable UUID taskId) {
        log.info("[AI-STREAM] Starting streaming task summary for task: {}", taskId);
        return aiService.streamTaskSummary(taskId)
                .doOnComplete(() -> log.info("[AI-STREAM] Task summary stream completed for task: {}", taskId))
                .doOnCancel(() -> log.info("[AI-STREAM] Task summary stream cancelled by client for task: {}", taskId))
                .doOnError(e -> log.warn("[AI-STREAM] Task summary stream error for task {}: {}", taskId, e.getMessage()));
    }
}
