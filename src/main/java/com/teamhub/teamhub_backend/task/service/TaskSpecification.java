package com.teamhub.teamhub_backend.task.service;

import com.teamhub.teamhub_backend.task.entity.Task;
import com.teamhub.teamhub_backend.task.entity.TaskPriority;
import com.teamhub.teamhub_backend.task.entity.TaskStatus;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class TaskSpecification {

    private TaskSpecification() {}

    public static Specification<Task> filterTasks(
            UUID projectId,
            TaskStatus status,
            TaskPriority priority,
            UUID assigneeId,
            String search
    ) {
        return (root, query, cb) -> {
            // Avoid N+1 queries when fetching task lists
            if (query != null && Long.class != query.getResultType() && long.class != query.getResultType()) {
                root.fetch("assignee", JoinType.LEFT);
                root.fetch("reporter", JoinType.INNER);
                root.fetch("project", JoinType.INNER);
            }

            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("project").get("id"), projectId));

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (priority != null) {
                predicates.add(cb.equal(root.get("priority"), priority));
            }
            if (assigneeId != null) {
                predicates.add(cb.equal(root.get("assignee").get("id"), assigneeId));
            }
            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                Predicate titleMatch = cb.like(cb.lower(root.get("title")), pattern);
                Predicate keyMatch = cb.like(cb.lower(root.get("taskKey")), pattern);
                predicates.add(cb.or(titleMatch, keyMatch));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}