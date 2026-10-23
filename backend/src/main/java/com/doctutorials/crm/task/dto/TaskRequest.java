package com.doctutorials.crm.task.dto;

import com.doctutorials.crm.lead.Priority;
import com.doctutorials.crm.task.TaskType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

/**
 * Create and update. Links (lead/customer/requirement/quotation/enquiry/activity) are only read on
 * create - they are fixed afterwards. No actor field: createdBy/completedBy come from the session (R6).
 */
public record TaskRequest(
        @NotBlank @Size(max = 255) String title,
        @Size(max = 4000) String description,
        TaskType type,
        UUID leadId,
        UUID customerId,
        UUID requirementId,
        UUID quotationId,
        UUID enquiryId,
        UUID activityId,
        UUID assignedToId,
        Instant dueDate,
        Priority priority,
        Instant reminderAt,
        @Size(max = 2000) String remarks) {
}
