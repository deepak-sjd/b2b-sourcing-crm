package com.doctutorials.crm.task;

import com.doctutorials.crm.common.BaseEntity;
import com.doctutorials.crm.lead.Lead;
import com.doctutorials.crm.lead.Priority;
import com.doctutorials.crm.user.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "tasks", indexes = {
        @Index(name = "idx_tasks_due_date", columnList = "dueDate"),
        @Index(name = "idx_tasks_status", columnList = "status"),
        @Index(name = "idx_tasks_lead_id", columnList = "lead_id"),
        @Index(name = "idx_tasks_assigned_to_id", columnList = "assigned_to_id")
})
@Getter
@Setter
public class Task extends BaseEntity {

    @NotBlank
    private String title;

    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lead_id")
    private Lead lead;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_id")
    private User assignedTo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Priority priority = Priority.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskStatus status = TaskStatus.PENDING;

    private Instant dueDate;
    private Instant completedAt;

    /** Computed, not persisted: drives the "Jun 10 (overdue)" red label in the UI. */
    @Transient
    public boolean isOverdue() {
        return status != TaskStatus.COMPLETED
                && status != TaskStatus.CANCELLED
                && dueDate != null
                && dueDate.isBefore(Instant.now());
    }
}
