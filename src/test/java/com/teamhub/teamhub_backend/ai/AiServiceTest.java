package com.teamhub.teamhub_backend.ai;

import com.teamhub.teamhub_backend.ai.dto.*;
import com.teamhub.teamhub_backend.ai.service.AiPerformanceLogger;
import com.teamhub.teamhub_backend.ai.service.AiService;
import com.teamhub.teamhub_backend.project.dto.ProjectResponse;
import com.teamhub.teamhub_backend.project.entity.ProjectStatus;
import com.teamhub.teamhub_backend.project.service.ProjectService;
import com.teamhub.teamhub_backend.task.dto.CommentResponse;
import com.teamhub.teamhub_backend.task.dto.TaskResponse;
import com.teamhub.teamhub_backend.task.entity.TaskPriority;
import com.teamhub.teamhub_backend.task.entity.TaskStatus;
import com.teamhub.teamhub_backend.task.service.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClient.CallResponseSpec;
import org.springframework.ai.chat.client.ChatClient.ChatClientRequestSpec;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AiService Tests")
class AiServiceTest {

    @Mock
    private ChatClient.Builder chatClientBuilder;

    @Mock
    private ChatClient chatClient;

    @Mock
    private ProjectService projectService;

    @Mock
    private TaskService taskService;

    private AiPerformanceLogger perfLogger;
    private AiService aiService;

    // Shared mock chain objects
    @Mock
    private ChatClientRequestSpec requestSpec;

    @Mock
    private ChatClient.UserSpec userSpec;

    @Mock
    private CallResponseSpec callResponseSpec;

    private static final UUID PROJECT_ID = UUID.randomUUID();
    private static final UUID TASK_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        perfLogger = new AiPerformanceLogger();

        when(chatClientBuilder.defaultSystem(anyString())).thenReturn(chatClientBuilder);
        when(chatClientBuilder.build()).thenReturn(chatClient);

        aiService = new AiService(chatClientBuilder, projectService, taskService, perfLogger);
    }

    private ProjectResponse sampleProject() {
        return new ProjectResponse(
                PROJECT_ID,
                UUID.randomUUID(),
                "Test Project",
                "TP",
                "A test project for unit testing",
                ProjectStatus.ACTIVE,
                UUID.randomUUID(),
                Instant.now(),
                Instant.now()
        );
    }

    private TaskResponse sampleTask() {
        return new TaskResponse(
                TASK_ID,
                PROJECT_ID,
                "TP-1",
                "Sample task",
                "A test task description",
                TaskStatus.IN_PROGRESS,
                TaskPriority.HIGH,
                null,
                null,
                null,
                0,
                Instant.now(),
                Instant.now()
        );
    }

    // =========================================================================
    // TASK DRAFT GENERATION TESTS
    // =========================================================================

    @Nested
    @DisplayName("generateTaskDraft")
    class GenerateTaskDraftTests {

        @Test
        @DisplayName("should generate task draft successfully")
        void shouldGenerateTaskDraft() {
            AiTaskDraftRequest request = new AiTaskDraftRequest("Add JWT authentication");
            AiTaskDraftResponse expectedResponse = new AiTaskDraftResponse(
                    "Implement JWT Authentication",
                    "Add JWT-based authentication to secure API endpoints",
                    List.of("JWT tokens are issued on login", "Protected endpoints require valid token"),
                    TaskPriority.HIGH
            );

            when(projectService.getProjectById(PROJECT_ID)).thenReturn(sampleProject());
            when(chatClient.prompt()).thenReturn(requestSpec);
            when(requestSpec.user(any(java.util.function.Consumer.class))).thenReturn(requestSpec);
            when(requestSpec.call()).thenReturn(callResponseSpec);
            when(callResponseSpec.entity(AiTaskDraftResponse.class)).thenReturn(expectedResponse);

            AiTaskDraftResponse result = aiService.generateTaskDraft(PROJECT_ID, request);

            assertThat(result).isNotNull();
            assertThat(result.suggestedTitle()).isEqualTo("Implement JWT Authentication");
            assertThat(result.suggestedPriority()).isEqualTo(TaskPriority.HIGH);
            assertThat(result.acceptanceCriteria()).hasSize(2);
            verify(projectService).getProjectById(PROJECT_ID);
        }

        @Test
        @DisplayName("should propagate exception when project not found")
        void shouldPropagateExceptionOnProjectNotFound() {
            AiTaskDraftRequest request = new AiTaskDraftRequest("Add feature");
            when(projectService.getProjectById(PROJECT_ID))
                    .thenThrow(new com.teamhub.teamhub_backend.common.exception.ResourceNotFoundException("Project not found"));

            assertThatThrownBy(() -> aiService.generateTaskDraft(PROJECT_ID, request))
                    .isInstanceOf(com.teamhub.teamhub_backend.common.exception.ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("should propagate exception on Gemini API failure")
        void shouldPropagateExceptionOnGeminiFailure() {
            AiTaskDraftRequest request = new AiTaskDraftRequest("Add feature");
            when(projectService.getProjectById(PROJECT_ID)).thenReturn(sampleProject());
            when(chatClient.prompt()).thenReturn(requestSpec);
            when(requestSpec.user(any(java.util.function.Consumer.class))).thenReturn(requestSpec);
            when(requestSpec.call()).thenThrow(new RuntimeException("Gemini API timeout"));

            assertThatThrownBy(() -> aiService.generateTaskDraft(PROJECT_ID, request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Gemini API timeout");
        }
    }

    // =========================================================================
    // TASK SUMMARY TESTS
    // =========================================================================

    @Nested
    @DisplayName("summarizeTaskDiscussion")
    class SummarizeTaskDiscussionTests {

        @Test
        @DisplayName("should summarize task with comments")
        void shouldSummarizeTaskWithComments() {
            AiTaskSummaryResponse expectedResponse = new AiTaskSummaryResponse(
                    "Task is in progress with JWT implementation",
                    List.of("Use RS256 signing"),
                    List.of(),
                    List.of("Complete token refresh flow")
            );

            List<CommentResponse> comments = List.of(
                    new CommentResponse(UUID.randomUUID(), TASK_ID, UUID.randomUUID(),
                            "Alice", null, "Let's use RS256", Instant.now(), Instant.now())
            );

            when(taskService.getTaskById(TASK_ID)).thenReturn(sampleTask());
            when(taskService.getTaskComments(TASK_ID)).thenReturn(comments);
            when(chatClient.prompt()).thenReturn(requestSpec);
            when(requestSpec.user(any(java.util.function.Consumer.class))).thenReturn(requestSpec);
            when(requestSpec.call()).thenReturn(callResponseSpec);
            when(callResponseSpec.entity(AiTaskSummaryResponse.class)).thenReturn(expectedResponse);

            AiTaskSummaryResponse result = aiService.summarizeTaskDiscussion(TASK_ID);

            assertThat(result.tldr()).contains("JWT");
            assertThat(result.keyDecisions()).isNotEmpty();
            verify(taskService).getTaskById(TASK_ID);
            verify(taskService).getTaskComments(TASK_ID);
        }

        @Test
        @DisplayName("should handle task with no comments")
        void shouldHandleNoComments() {
            AiTaskSummaryResponse expectedResponse = new AiTaskSummaryResponse(
                    "No discussion yet", Collections.emptyList(), Collections.emptyList(),
                    List.of("Start the discussion")
            );

            when(taskService.getTaskById(TASK_ID)).thenReturn(sampleTask());
            when(taskService.getTaskComments(TASK_ID)).thenReturn(Collections.emptyList());
            when(chatClient.prompt()).thenReturn(requestSpec);
            when(requestSpec.user(any(java.util.function.Consumer.class))).thenReturn(requestSpec);
            when(requestSpec.call()).thenReturn(callResponseSpec);
            when(callResponseSpec.entity(AiTaskSummaryResponse.class)).thenReturn(expectedResponse);

            AiTaskSummaryResponse result = aiService.summarizeTaskDiscussion(TASK_ID);

            assertThat(result).isNotNull();
            assertThat(result.tldr()).isEqualTo("No discussion yet");
        }
    }

    // =========================================================================
    // BOARD FILTER TESTS
    // =========================================================================

    @Nested
    @DisplayName("filterBoardByNaturalLanguage")
    class FilterBoardTests {

        @Test
        @DisplayName("should extract filters and return matching tasks")
        void shouldExtractFiltersAndReturnTasks() {
            AiBoardFilterRequest request = new AiBoardFilterRequest("show me high priority in-progress tasks");
            AiBoardFilterResponse.ExtractedFilter filter = new AiBoardFilterResponse.ExtractedFilter(
                    TaskStatus.IN_PROGRESS, TaskPriority.HIGH, null
            );

            when(projectService.getProjectById(PROJECT_ID)).thenReturn(sampleProject());
            when(chatClient.prompt()).thenReturn(requestSpec);
            when(requestSpec.user(any(java.util.function.Consumer.class))).thenReturn(requestSpec);
            when(requestSpec.call()).thenReturn(callResponseSpec);
            when(callResponseSpec.entity(AiBoardFilterResponse.ExtractedFilter.class)).thenReturn(filter);
            when(taskService.getProjectTasks(PROJECT_ID, TaskStatus.IN_PROGRESS, TaskPriority.HIGH, null, null))
                    .thenReturn(List.of(sampleTask()));

            AiBoardFilterResponse result = aiService.filterBoardByNaturalLanguage(PROJECT_ID, request);

            assertThat(result.extractedFilter().status()).isEqualTo(TaskStatus.IN_PROGRESS);
            assertThat(result.extractedFilter().priority()).isEqualTo(TaskPriority.HIGH);
            assertThat(result.tasks()).hasSize(1);
        }
    }

    // =========================================================================
    // STREAMING TESTS
    // =========================================================================

    @Nested
    @DisplayName("Streaming Endpoints")
    class StreamingTests {

        @Mock
        private ChatClient.StreamResponseSpec streamResponseSpec;

        @Test
        @DisplayName("streamTaskDraft should emit chunks and complete")
        void shouldStreamTaskDraftChunks() {
            when(projectService.getProjectById(PROJECT_ID)).thenReturn(sampleProject());
            when(chatClient.prompt()).thenReturn(requestSpec);
            when(requestSpec.user(any(java.util.function.Consumer.class))).thenReturn(requestSpec);
            when(requestSpec.stream()).thenReturn(streamResponseSpec);
            when(streamResponseSpec.content()).thenReturn(Flux.just("## Title\n", "Implement ", "JWT auth"));

            Flux<String> result = aiService.streamTaskDraft(PROJECT_ID, new AiTaskDraftRequest("Add JWT"));

            StepVerifier.create(result)
                    .expectNext("## Title\n")
                    .expectNext("Implement ")
                    .expectNext("JWT auth")
                    .verifyComplete();
        }

        @Test
        @DisplayName("streamTaskDraft should handle Gemini stream error")
        void shouldHandleStreamError() {
            when(projectService.getProjectById(PROJECT_ID)).thenReturn(sampleProject());
            when(chatClient.prompt()).thenReturn(requestSpec);
            when(requestSpec.user(any(java.util.function.Consumer.class))).thenReturn(requestSpec);
            when(requestSpec.stream()).thenReturn(streamResponseSpec);
            when(streamResponseSpec.content())
                    .thenReturn(Flux.just("partial").concatWith(Flux.error(new RuntimeException("Stream interrupted"))));

            Flux<String> result = aiService.streamTaskDraft(PROJECT_ID, new AiTaskDraftRequest("Add JWT"));

            StepVerifier.create(result)
                    .expectNext("partial")
                    .expectErrorMatches(e -> e.getMessage().contains("Stream interrupted"))
                    .verify();
        }

        @Test
        @DisplayName("streamTaskDraft should handle empty stream")
        void shouldHandleEmptyStream() {
            when(projectService.getProjectById(PROJECT_ID)).thenReturn(sampleProject());
            when(chatClient.prompt()).thenReturn(requestSpec);
            when(requestSpec.user(any(java.util.function.Consumer.class))).thenReturn(requestSpec);
            when(requestSpec.stream()).thenReturn(streamResponseSpec);
            when(streamResponseSpec.content()).thenReturn(Flux.empty());

            Flux<String> result = aiService.streamTaskDraft(PROJECT_ID, new AiTaskDraftRequest("Add JWT"));

            StepVerifier.create(result)
                    .verifyComplete();
        }

        @Test
        @DisplayName("streamTaskSummary should emit chunks and complete")
        void shouldStreamTaskSummaryChunks() {
            when(taskService.getTaskById(TASK_ID)).thenReturn(sampleTask());
            when(taskService.getTaskComments(TASK_ID)).thenReturn(Collections.emptyList());
            when(chatClient.prompt()).thenReturn(requestSpec);
            when(requestSpec.user(any(java.util.function.Consumer.class))).thenReturn(requestSpec);
            when(requestSpec.stream()).thenReturn(streamResponseSpec);
            when(streamResponseSpec.content()).thenReturn(Flux.just("## TL;DR\n", "Task is progressing"));

            Flux<String> result = aiService.streamTaskSummary(TASK_ID);

            StepVerifier.create(result)
                    .expectNext("## TL;DR\n")
                    .expectNext("Task is progressing")
                    .verifyComplete();
        }

        @Test
        @DisplayName("streamTaskDraft should handle cancellation gracefully")
        void shouldHandleCancellation() {
            when(projectService.getProjectById(PROJECT_ID)).thenReturn(sampleProject());
            when(chatClient.prompt()).thenReturn(requestSpec);
            when(requestSpec.user(any(java.util.function.Consumer.class))).thenReturn(requestSpec);
            when(requestSpec.stream()).thenReturn(streamResponseSpec);
            when(streamResponseSpec.content())
                    .thenReturn(Flux.just("chunk1", "chunk2", "chunk3", "chunk4", "chunk5"));

            Flux<String> result = aiService.streamTaskDraft(PROJECT_ID, new AiTaskDraftRequest("Add JWT"));

            // Take only 2 chunks (simulates client disconnection)
            StepVerifier.create(result.take(2))
                    .expectNext("chunk1")
                    .expectNext("chunk2")
                    .verifyComplete();
        }
    }

    // =========================================================================
    // PERFORMANCE LOGGER TESTS
    // =========================================================================

    @Nested
    @DisplayName("AiPerformanceLogger")
    class PerformanceLoggerTests {

        @Test
        @DisplayName("should create context and log checkpoints without error")
        void shouldCreateAndLogCheckpoints() {
            AiPerformanceLogger.PerfContext ctx = perfLogger.start("testOperation");
            ctx.checkpoint("Step 1");
            ctx.checkpoint("Step 2");
            ctx.complete();
            // No assertion needed - verifying no exception is thrown
        }

        @Test
        @DisplayName("should log failure without error")
        void shouldLogFailure() {
            AiPerformanceLogger.PerfContext ctx = perfLogger.start("failingOperation");
            ctx.checkpoint("Before Failure");
            ctx.fail("Simulated timeout");
            // No assertion needed - verifying no exception is thrown
        }

        @Test
        @DisplayName("request ID should be non-null")
        void shouldGenerateRequestId() {
            AiPerformanceLogger.PerfContext ctx = perfLogger.start("idTest");
            assertThat(ctx.getRequestId()).isNotNull();
            assertThat(ctx.getRequestId()).hasSize(8);
        }
    }
}
