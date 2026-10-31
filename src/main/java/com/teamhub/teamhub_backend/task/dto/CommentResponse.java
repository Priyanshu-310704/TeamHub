package com.teamhub.teamhub_backend.task.dto;

import com.teamhub.teamhub_backend.task.entity.TaskComment;

import java.time.Instant;
import java.util.UUID;

public record CommentResponse(
        UUID id,
        UUID taskId,
        UUID authorId,
        String authorName,
        String authorAvatarUrl,
        String content,
        Instant createdAt,
        Instant updatedAt
) {
    public static CommentResponse fromEntity(TaskComment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getTask().getId(),
                comment.getAuthor().getId(),
                comment.getAuthor().getName(),
                comment.getAuthor().getAvatarUrl(),
                comment.getContent(),
                comment.getCreatedAt(),
                comment.getUpdatedAt()
        );
    }
}