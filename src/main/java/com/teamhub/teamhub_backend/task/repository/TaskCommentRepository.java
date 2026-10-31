package com.teamhub.teamhub_backend.task.repository;

import com.teamhub.teamhub_backend.task.entity.TaskComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface TaskCommentRepository extends JpaRepository<TaskComment, UUID> {

    @Query("""
            SELECT tc FROM TaskComment tc
            JOIN FETCH tc.author a
            WHERE tc.task.id = :taskId
            ORDER BY tc.createdAt ASC
            """)
    List<TaskComment> findAllByTaskIdWithAuthor(@Param("taskId") UUID taskId);
}