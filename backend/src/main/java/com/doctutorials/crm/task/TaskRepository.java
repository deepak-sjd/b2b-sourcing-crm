package com.doctutorials.crm.task;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task, UUID>, JpaSpecificationExecutor<Task> {
    Page<Task> findByLeadId(UUID leadId, Pageable pageable);

    Page<Task> findByAssignedToId(UUID userId, Pageable pageable);

    List<Task> findByStatusNotAndDueDateBefore(TaskStatus status, Instant now);

    // No countByStatus/countByDueDateBeforeAndStatusNotIn here on purpose - those
    // were
    // unscoped org-wide counts.
    // TaskService.countPending/countCompleted/countOverdue now
    // build the same numbers via TaskAccessService.taskVisibility() +
    // count(Specification)
    // so the /tasks/summary figures are scoped to what the caller can see.
}
