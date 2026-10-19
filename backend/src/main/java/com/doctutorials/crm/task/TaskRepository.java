package com.doctutorials.crm.task;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Every user-facing list goes through TaskService + TaskAccessService specifications.
 * The two methods below are for the system sweep only (no caller, so no visibility scope).
 */
public interface TaskRepository extends JpaRepository<Task, UUID>, JpaSpecificationExecutor<Task> {

    interface OverdueRow {
        UUID getId();

        UUID getAssigneeId();

        UUID getLeadId();

        Instant getDueDate();
    }

    /** Ids only, backed by the partial index on open tasks - small in steady state. */
    @Query("select t.id as id, a.id as assigneeId, l.id as leadId, t.dueDate as dueDate "
            + "from Task t left join t.assignedTo a left join t.lead l "
            + "where t.status in :statuses and t.dueDate < :now")
    List<OverdueRow> findOverdueCandidates(@Param("statuses") Collection<TaskStatus> statuses,
            @Param("now") Instant now);

    /** One statement for the whole backlog. Bulk JPQL skips auditing/versioning, so both are set here. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Task t set t.status = com.doctutorials.crm.task.TaskStatus.OVERDUE, "
            + "t.updatedAt = :now, t.version = t.version + 1 "
            + "where t.status in :statuses and t.dueDate < :now")
    int bulkMarkOverdue(@Param("statuses") Collection<TaskStatus> statuses, @Param("now") Instant now);
}
