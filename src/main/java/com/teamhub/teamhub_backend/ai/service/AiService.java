package com.teamhub.teamhub_backend.ai.service;

import com.teamhub.teamhub_backend.ai.dto.*;
import com.teamhub.teamhub_backend.project.dto.ProjectResponse;
import com.teamhub.teamhub_backend.project.service.ProjectService;
import com.teamhub.teamhub_backend.task.dto.CommentResponse;
import com.teamhub.teamhub_backend.task.dto.TaskResponse;
import com.teamhub.teamhub_backend.task.service.TaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AiService {

    private final ChatClient chatClient;
    private final ProjectService projectService;
    private final TaskService taskService;
    private final AiPerformanceLogger perfLogger;

    public AiService(
            ChatClient.Builder chatClientBuilder,
            ProjectService projectService,
            TaskService taskService,
            AiPerformanceLogger perfLogger
    ) {
        this.chatClient = chatClientBuilder
                .defaultSystem("""
                        You are TeamHub AI, an expert technical project management assistant integrated into a Jira-like engineering platform.
                        Always return concise, practical, engineering-grade outputs matching the requested schema.
                        """)
                .build();
        this.projectService = projectService;
        this.taskService = taskService;
        this.perfLogger = perfLogger;
    }

    // =========================================================================
    // 1. TASK DRAFT GENERATOR (synchronous - returns structured JSON)
    // =========================================================================

    @Transactional(readOnly = true)
    public AiTaskDraftResponse generateTaskDraft(UUID projectId, AiTaskDraftRequest request) {
        AiPerformanceLogger.PerfContext perf = perfLogger.start("generateTaskDraft");

        try {
            ProjectResponse project = projectService.getProjectById(projectId);
            perf.checkpoint("Database Query");

            AiTaskDraftResponse response = chatClient.prompt()
                    .user(u -> u.text("""
                            Generate a structured software engineering task specification for the following project:
                            Project Name: {projectName} ({projectKey})
                            Project Context: {projectDesc}

                            User's rough task idea: "{prompt}"

                            Provide:
                            1. A clear, action-oriented title (under 120 chars)
                            2. A concise description explaining the goal and technical context
                            3. 3 to 5 testable acceptance criteria
                            4. Recommended priority (LOW, MEDIUM, HIGH, or URGENT)
                            """)
                            .param("projectName", project.name())
                            .param("projectKey", project.projectKey())
                            .param("projectDesc", project.description() != null ? project.description() : "N/A")
                            .param("prompt", request.prompt())
                    )
                    .call()
                    .entity(AiTaskDraftResponse.class);

            perf.checkpoint("Gemini Generation").complete();
            return response;
        } catch (Exception ex) {
            perf.fail(ex.getClass().getSimpleName() + ": " + ex.getMessage());
            throw ex;
        }
    }

    // =========================================================================
    // 2. TASK DISCUSSION SUMMARIZER (synchronous - returns structured JSON)
    // =========================================================================

    @Transactional(readOnly = true)
    public AiTaskSummaryResponse summarizeTaskDiscussion(UUID taskId) {
        AiPerformanceLogger.PerfContext perf = perfLogger.start("summarizeTaskDiscussion");

        try {
            TaskResponse task = taskService.getTaskById(taskId);
            perf.checkpoint("Task Query");

            List<CommentResponse> comments = taskService.getTaskComments(taskId);
            perf.checkpoint("Comments Query");

            // Limit comment history to last 50 comments to avoid oversized prompts
            List<CommentResponse> boundedComments = comments.size() > 50
                    ? comments.subList(comments.size() - 50, comments.size())
                    : comments;

            String formattedComments = boundedComments.isEmpty()
                    ? "No comments yet."
                    : boundedComments.stream()
                    .map(c -> "[" + c.authorName() + " at " + c.createdAt() + "]: " + c.content())
                    .collect(Collectors.joining("\n"));
            perf.checkpoint("Prompt Construction");

            AiTaskSummaryResponse response = chatClient.prompt()
                    .user(u -> u.text("""
                            Summarize the following task and its comment discussion thread for an engineering team:

                            Task Key: {taskKey}
                            Title: {title}
                            Current Status: {status} | Priority: {priority}
                            Description: {description}

                            Comment Thread ({commentCount} comments, showing last {shownCount}):
                            {comments}

                            Provide a brief TL;DR, key decisions made, any open blockers, and clear next steps.
                            """)
                            .param("taskKey", task.taskKey())
                            .param("title", task.title())
                            .param("status", task.status().name())
                            .param("priority", task.priority().name())
                            .param("description", task.description() != null ? task.description() : "No description provided.")
                            .param("commentCount", String.valueOf(comments.size()))
                            .param("shownCount", String.valueOf(boundedComments.size()))
                            .param("comments", formattedComments)
                    )
                    .call()
                    .entity(AiTaskSummaryResponse.class);

            perf.checkpoint("Gemini Generation").complete();
            return response;
        } catch (Exception ex) {
            perf.fail(ex.getClass().getSimpleName() + ": " + ex.getMessage());
            throw ex;
        }
    }

    // =========================================================================
    // 3. NATURAL LANGUAGE BOARD FILTER (synchronous - returns structured JSON)
    // =========================================================================

    @Transactional(readOnly = true)
    public AiBoardFilterResponse filterBoardByNaturalLanguage(UUID projectId, AiBoardFilterRequest request) {
        AiPerformanceLogger.PerfContext perf = perfLogger.start("filterBoardByNaturalLanguage");

        try {
            projectService.getProjectById(projectId);
            perf.checkpoint("Access Check");

            AiBoardFilterResponse.ExtractedFilter extracted = chatClient.prompt()
                    .user(u -> u.text("""
                            Convert the user's natural language Kanban board search query into filter criteria.
                            Allowed Status values: TODO, IN_PROGRESS, IN_REVIEW, DONE (or null if not specified).
                            Allowed Priority values: LOW, MEDIUM, HIGH, URGENT (or null if not specified).
                            searchKeyword: A single core keyword or task key (e.g. "JWT", "TH-1", "redis") or null if none.

                            User query: "{query}"
                            """)
                            .param("query", request.query())
                    )
                    .call()
                    .entity(AiBoardFilterResponse.ExtractedFilter.class);

            perf.checkpoint("Gemini Generation");

            List<TaskResponse> matchingTasks = taskService.getProjectTasks(
                    projectId,
                    extracted.status(),
                    extracted.priority(),
                    null,
                    extracted.searchKeyword()
            );
            perf.checkpoint("Task Filter Query").complete();

            return new AiBoardFilterResponse(extracted, matchingTasks);
        } catch (Exception ex) {
            perf.fail(ex.getClass().getSimpleName() + ": " + ex.getMessage());
            throw ex;
        }
    }

    // =========================================================================
    // 4. STREAMING: Task Draft (SSE - progressive text delivery)
    // =========================================================================

    /**
     * Streams the task draft generation as text chunks via SSE.
     * This allows the React frontend to display content progressively
     * instead of waiting for the full Gemini response.
     *
     * Returns a Flux<String> that Spring MVC's SSE support can consume.
     */
    @Transactional(readOnly = true)
    public Flux<String> streamTaskDraft(UUID projectId, AiTaskDraftRequest request) {
        // Perform DB query synchronously before entering the reactive stream
        ProjectResponse project = projectService.getProjectById(projectId);

        return chatClient.prompt()
                .user(u -> u.text("""
                        Generate a structured software engineering task specification for the following project:
                        Project Name: {projectName} ({projectKey})
                        Project Context: {projectDesc}

                        User's rough task idea: "{prompt}"

                        Provide:
                        1. A clear, action-oriented title (under 120 chars)
                        2. A concise description explaining the goal and technical context
                        3. 3 to 5 testable acceptance criteria
                        4. Recommended priority (LOW, MEDIUM, HIGH, or URGENT)

                        Format the output as readable markdown.
                        """)
                        .param("projectName", project.name())
                        .param("projectKey", project.projectKey())
                        .param("projectDesc", project.description() != null ? project.description() : "N/A")
                        .param("prompt", request.prompt())
                )
                .stream()
                .content();
    }

    // =========================================================================
    // 5. STREAMING: Task Summarizer (SSE - progressive text delivery)
    // =========================================================================

    @Transactional(readOnly = true)
    public Flux<String> streamTaskSummary(UUID taskId) {
        // Perform DB queries synchronously before entering the reactive stream
        TaskResponse task = taskService.getTaskById(taskId);
        List<CommentResponse> comments = taskService.getTaskComments(taskId);

        // Bound to last 50 comments
        List<CommentResponse> boundedComments = comments.size() > 50
                ? comments.subList(comments.size() - 50, comments.size())
                : comments;

        String formattedComments = boundedComments.isEmpty()
                ? "No comments yet."
                : boundedComments.stream()
                .map(c -> "[" + c.authorName() + " at " + c.createdAt() + "]: " + c.content())
                .collect(Collectors.joining("\n"));

        return chatClient.prompt()
                .user(u -> u.text("""
                        Summarize the following task and its comment discussion thread for an engineering team:

                        Task Key: {taskKey}
                        Title: {title}
                        Current Status: {status} | Priority: {priority}
                        Description: {description}

                        Comment Thread ({commentCount} comments, showing last {shownCount}):
                        {comments}

                        Provide a brief TL;DR, key decisions made, any open blockers, and clear next steps.
                        Format the output as readable markdown.
                        """)
                        .param("taskKey", task.taskKey())
                        .param("title", task.title())
                        .param("status", task.status().name())
                        .param("priority", task.priority().name())
                        .param("description", task.description() != null ? task.description() : "No description provided.")
                        .param("commentCount", String.valueOf(comments.size()))
                        .param("shownCount", String.valueOf(boundedComments.size()))
                        .param("comments", formattedComments)
                )
                .stream()
                .content();
    }
}
