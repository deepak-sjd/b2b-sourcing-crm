package com.doctutorials.crm.task;

import com.doctutorials.crm.activity.Activity;
import com.doctutorials.crm.common.BaseEntity;
import com.doctutorials.crm.customer.Customer;
import com.doctutorials.crm.lead.Lead;
import com.doctutorials.crm.lead.Priority;
import com.doctutorials.crm.quotation.Quotation;
import com.doctutorials.crm.requirement.Requirement;
import com.doctutorials.crm.supplier.enquiry.SupplierEnquiry;
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

    @Column(length = 4000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "task_type", nullable = false)
    private TaskType type = TaskType.INTERNAL;

    /** Lead OR customer (DB CHECK: at least one). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lead_id")
    private Lead lead;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requirement_id")
    private Requirement requirement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quotation_id")
    private Quotation quotation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enquiry_id")
    private SupplierEnquiry enquiry;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_id")
    private Activity activity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_id")
    private User assignedTo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "completed_by_id")
    private User completedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Priority priority = Priority.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskStatus status = TaskStatus.PENDING;

    private Instant dueDate;
    private Instant reminderAt;
    private Instant completedAt;

    @Column(length = 2000)
    private String remarks;

    /** Soft cancel (R1): the row stays, status = CANCELLED. */
    @Column(length = 500)
    private String cancelReason;

    @Transient
    public boolean isOverdue() {
        return status != TaskStatus.COMPLETED
                && status != TaskStatus.CANCELLED
                && dueDate != null
                && dueDate.isBefore(Instant.now());
    }
}
