package com.teamhub.teamhub_backend.task.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommentRequest(
        @NotBlank(message = "Comment content cannot be blank")
        @Size(max = 3000, message = "Comment must be at most 3000 characters")
        String content
) {}