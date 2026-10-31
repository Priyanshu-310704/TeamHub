package com.teamhub.teamhub_backend.ai.service;

import com.teamhub.teamhub_backend.ai.dto.*;
import com.teamhub.teamhub_backend.project.dto.ProjectResponse;
import com.teamhub.teamhub_backend.project.service.ProjectService;
import com.teamhub.teamhub_backend.task.dto.CommentResponse;
import com.teamhub.teamhub_backend.task.dto.TaskResponse;
import com.teamhub.teamhub_backend.task.service.TaskService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AiService {

    private final ChatClient chatClient;
    private final ProjectService projectService;
    private final TaskService taskService;

    public AiService(
            ChatClient.Builder chatClientBuilder,
            ProjectService projectService,
            TaskService taskService
    ) {
        this.chatClient = chatClientBuilder
                .defaultSystem("""
                        You are TeamHub AI, an expert technical project management assistant integrated into a Jira-like engineering platform.
                        Always return concise, practical, engineering-grade outputs matching the requested schema.
                        """)
                .build();
        this.projectService = projectService;
        this.taskService = taskService;
    }

    /**
     * 1. Smart Task Spec Generator: turns a one-liner into a ready-to-save Jira ticket draft.
     */
    @Transactional(readOnly = true)
    public AiTaskDraftResponse generateTaskDraft(UUID projectId, AiTaskDraftRequest request) {
        // Also verifies the current user is a member of the project's team
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
                        """)
                        .param("projectName", project.name())
                        .param("projectKey", project.projectKey())
                        .param("projectDesc", project.description() != null ? project.description() : "N/A")
                        .param("prompt", request.prompt())
                )
                .call()
                .entity(AiTaskDraftResponse.class);
    }

    /**
     * 2. Task Thread Summarizer: summarizes task details + all comments into decisions, blockers, and next steps.
     */
    @Transactional(readOnly = true)
    public AiTaskSummaryResponse summarizeTaskDiscussion(UUID taskId) {
        // Both methods verify that the logged-in user belongs to the task's team
        TaskResponse task = taskService.getTaskById(taskId);
        List<CommentResponse> comments = taskService.getTaskComments(taskId);

        String formattedComments = comments.isEmpty()
                ? "No comments yet."
                : comments.stream()
                .map(c -> "[" + c.authorName() + " at " + c.createdAt() + "]: " + c.content())
                .collect(Collectors.joining("\n"));

        return chatClient.prompt()
                .user(u -> u.text("""
                        Summarize the following task and its comment discussion thread for an engineering team:
                        
                        Task Key: {taskKey}
                        Title: {title}
                        Current Status: {status} | Priority: {priority}
                        Description: {description}
                        
                        Comment Thread:
                        {comments}
                        
                        Provide a brief TL;DR, key decisions made, any open blockers, and clear next steps.
                        """)
                        .param("taskKey", task.taskKey())
                        .param("title", task.title())
                        .param("status", task.status().name())
                        .param("priority", task.priority().name())
                        .param("description", task.description() != null ? task.description() : "No description provided.")
                        .param("comments", formattedComments)
                )
                .call()
                .entity(AiTaskSummaryResponse.class);
    }

    /**
     * 3. Natural Language Board Filter: translates English queries into TaskSpecification parameters.
     */
    @Transactional(readOnly = true)
    public AiBoardFilterResponse filterBoardByNaturalLanguage(UUID projectId, AiBoardFilterRequest request) {
        // Verify access first
        projectService.getProjectById(projectId);

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

        List<TaskResponse> matchingTasks = taskService.getProjectTasks(
                projectId,
                extracted.status(),
                extracted.priority(),
                null,
                extracted.searchKeyword()
        );

        return new AiBoardFilterResponse(extracted, matchingTasks);
    }
}