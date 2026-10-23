package com.doctutorials.crm.task.dto;

import com.doctutorials.crm.lead.Priority;
import com.doctutorials.crm.task.Task;
import com.doctutorials.crm.task.TaskStatus;
import com.doctutorials.crm.task.TaskType;
import com.doctutorials.crm.user.User;

import java.time.Instant;
import java.util.UUID;

/** Keeps the field names the existing frontend reads (lead, assignedTo, overdue...) and adds the v2 fields. */
public record TaskResponse(
        UUID id,
        String title,
        String description,
        TaskType type,
        LeadRef lead,
        CustomerRef customer,
        UUID requirementId,
        UUID quotationId,
        UUID enquiryId,
        UUID activityId,
        UserRef assignedTo,
        UserRef createdBy,
        UserRef completedBy,
        Priority priority,
        TaskStatus status,
        Instant dueDate,
        Instant reminderAt,
        Instant completedAt,
        String remarks,
        String cancelReason,
        boolean overdue,
        Instant createdAt,
        Instant updatedAt) {

    public record LeadRef(UUID id, String fullName, String phone, String email, String stage) {
    }

    public record CustomerRef(UUID id, String companyName) {
    }

    public record UserRef(UUID id, String fullName, String email) {
    }

    /** Call only inside the service's transaction (lazy associations, open-in-view is off). */
    public static TaskResponse from(Task t) {
        var lead = t.getLead();
        var customer = t.getCustomer();
        return new TaskResponse(
                t.getId(), t.getTitle(), t.getDescription(), t.getType(),
                lead == null ? null : new LeadRef(lead.getId(), lead.getFullName(), lead.getPhone(), lead.getEmail(),
                        lead.getStage() != null ? lead.getStage().name() : null),
                customer == null ? null : new CustomerRef(customer.getId(), customer.getCompanyName()),
                t.getRequirement() != null ? t.getRequirement().getId() : null,
                t.getQuotation() != null ? t.getQuotation().getId() : null,
                t.getEnquiry() != null ? t.getEnquiry().getId() : null,
                t.getActivity() != null ? t.getActivity().getId() : null,
                ref(t.getAssignedTo()), ref(t.getCreatedBy()), ref(t.getCompletedBy()),
                t.getPriority(), t.getStatus(), t.getDueDate(), t.getReminderAt(), t.getCompletedAt(),
                t.getRemarks(), t.getCancelReason(), t.isOverdue(), t.getCreatedAt(), t.getUpdatedAt());
    }

    private static UserRef ref(User u) {
        return u == null ? null : new UserRef(u.getId(), u.getFullName(), u.getEmail());
    }
}
