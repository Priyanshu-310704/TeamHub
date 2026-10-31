package com.teamhub.teamhub_backend.task.service;

import com.teamhub.teamhub_backend.common.exception.ConflictException;
import com.teamhub.teamhub_backend.common.exception.ForbiddenAccessException;
import com.teamhub.teamhub_backend.common.exception.ResourceNotFoundException;
import com.teamhub.teamhub_backend.project.entity.Project;
import com.teamhub.teamhub_backend.project.service.ProjectService;
import com.teamhub.teamhub_backend.task.dto.CommentRequest;
import com.teamhub.teamhub_backend.task.dto.CommentResponse;
import com.teamhub.teamhub_backend.task.dto.CreateTaskRequest;
import com.teamhub.teamhub_backend.task.dto.TaskResponse;
import com.teamhub.teamhub_backend.task.dto.UpdateTaskRequest;
import com.teamhub.teamhub_backend.task.entity.Task;
import com.teamhub.teamhub_backend.task.entity.TaskComment;
import com.teamhub.teamhub_backend.task.entity.TaskPriority;
import com.teamhub.teamhub_backend.task.entity.TaskStatus;
import com.teamhub.teamhub_backend.task.repository.TaskCommentRepository;
import com.teamhub.teamhub_backend.task.repository.TaskRepository;
import com.teamhub.teamhub_backend.team.service.TeamService;
import com.teamhub.teamhub_backend.user.entity.User;
import com.teamhub.teamhub_backend.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskCommentRepository commentRepository;
    private final ProjectService projectService;
    private final TeamService teamService;
    private final UserService userService;

    @Transactional
    public TaskResponse createTask(UUID projectId, CreateTaskRequest request) {
        User currentUser = userService.getCurrentAuthenticatedUser();
        Project project = projectService.getProjectAndVerifyMembership(projectId, currentUser.getId());

        User assignee = null;
        if (request.assigneeId() != null) {
            assignee = resolveAndValidateAssignee(project.getTeam().getId(), request.assigneeId());
        }

        String generatedTaskKey = generateNextTaskKey(project);

        Task task = Task.builder()
                .project(project)
                .taskKey(generatedTaskKey)
                .title(request.title().trim())
                .description(request.description() != null ? request.description().trim() : null)
                .status(request.status() != null ? request.status() : TaskStatus.TODO)
                .priority(request.priority() != null ? request.priority() : TaskPriority.MEDIUM)
                .assignee(assignee)
                .reporter(currentUser)
                .dueDate(request.dueDate())
                .position(request.position() != null ? request.position() : 0)
                .build();

        Task savedTask = taskRepository.save(task);
        return TaskResponse.fromEntity(savedTask);
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> getProjectTasks(
            UUID projectId,
            TaskStatus status,
            TaskPriority priority,
            UUID assigneeId,
            String search
    ) {
        User currentUser = userService.getCurrentAuthenticatedUser();
        projectService.getProjectAndVerifyMembership(projectId, currentUser.getId());

        Sort sort = Sort.by(Sort.Order.asc("position"), Sort.Order.desc("createdAt"));

        return taskRepository.findAll(
                        TaskSpecification.filterTasks(projectId, status, priority, assigneeId, search),
                        sort
                )
                .stream()
                .map(TaskResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public TaskResponse getTaskById(UUID taskId) {
        User currentUser = userService.getCurrentAuthenticatedUser();
        Task task = getTaskAndVerifyMembership(taskId, currentUser.getId());
        return TaskResponse.fromEntity(task);
    }

    @Transactional
    public TaskResponse updateTask(UUID taskId, UpdateTaskRequest request) {
        User currentUser = userService.getCurrentAuthenticatedUser();
        Task task = getTaskAndVerifyMembership(taskId, currentUser.getId());

        if (request.title() != null && !request.title().isBlank()) {
            task.setTitle(request.title().trim());
        }
        if (request.description() != null) {
            task.setDescription(request.description().trim());
        }
        if (request.status() != null) {
            task.setStatus(request.status());
        }
        if (request.priority() != null) {
            task.setPriority(request.priority());
        }
        if (Boolean.TRUE.equals(request.unassign())) {
            task.setAssignee(null);
        } else if (request.assigneeId() != null) {
            User newAssignee = resolveAndValidateAssignee(
                    task.getProject().getTeam().getId(),
                    request.assigneeId()
            );
            task.setAssignee(newAssignee);
        }
        if (request.dueDate() != null) {
            task.setDueDate(request.dueDate());
        }
        if (request.position() != null) {
            task.setPosition(request.position());
        }

        Task updated = taskRepository.save(task);
        return TaskResponse.fromEntity(updated);
    }

    @Transactional
    public void deleteTask(UUID taskId) {
        User currentUser = userService.getCurrentAuthenticatedUser();
        Task task = getTaskAndVerifyMembership(taskId, currentUser.getId());
        taskRepository.delete(task);
    }

    // =========================================================================
    // TASK COMMENTS (MATCHES V5__create_task_comments.sql)
    // =========================================================================

    @Transactional
    public CommentResponse addComment(UUID taskId, CommentRequest request) {
        User currentUser = userService.getCurrentAuthenticatedUser();
        Task task = getTaskAndVerifyMembership(taskId, currentUser.getId());

        TaskComment comment = TaskComment.builder()
                .task(task)
                .author(currentUser)
                .content(request.content().trim())
                .build();

        TaskComment saved = commentRepository.save(comment);
        return CommentResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> getTaskComments(UUID taskId) {
        User currentUser = userService.getCurrentAuthenticatedUser();
        getTaskAndVerifyMembership(taskId, currentUser.getId());

        return commentRepository.findAllByTaskIdWithAuthor(taskId)
                .stream()
                .map(CommentResponse::fromEntity)
                .toList();
    }

    @Transactional
    public void deleteComment(UUID commentId) {
        User currentUser = userService.getCurrentAuthenticatedUser();
        TaskComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found with id: " + commentId));

        UUID teamId = comment.getTask().getProject().getTeam().getId();
        teamService.verifyMembership(teamId, currentUser.getId());

        // Only the comment author or a team ADMIN/OWNER can delete a comment
        boolean isAuthor = comment.getAuthor().getId().equals(currentUser.getId());
        if (!isAuthor) {
            teamService.verifyAdminOrOwner(teamId, currentUser.getId());
        }

        commentRepository.delete(comment);
    }

    // =========================================================================
    // INTERNAL HELPERS
    // =========================================================================

    private Task getTaskAndVerifyMembership(UUID taskId, UUID userId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + taskId));
        teamService.verifyMembership(task.getProject().getTeam().getId(), userId);
        return task;
    }

    private User resolveAndValidateAssignee(UUID teamId, UUID assigneeId) {
        User assignee = userService.getUserEntityById(assigneeId);
        try {
            teamService.verifyMembership(teamId, assigneeId);
        } catch (ForbiddenAccessException ex) {
            throw new ConflictException("Assignee must be a member of the project's team");
        }
        return assignee;
    }

    private String generateNextTaskKey(Project project) {
        long sequence = taskRepository.countByProjectId(project.getId()) + 1;
        String candidateKey = project.getProjectKey() + "-" + sequence;
        while (taskRepository.existsByProjectIdAndTaskKey(project.getId(), candidateKey)) {
            sequence++;
            candidateKey = project.getProjectKey() + "-" + sequence;
        }
        return candidateKey;
    }
}