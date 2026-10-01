package com.doctutorials.crm.requirement.dto;

import com.doctutorials.crm.lead.Priority;
import com.doctutorials.crm.requirement.Requirement;
import com.doctutorials.crm.requirement.RequirementStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record RequirementResponse(
        UUID id,
        UUID leadId,
        String leadName,
        String productName,
        String description,
        BigDecimal quantity,
        String unit,
        String expectedTimeline,
        BigDecimal budget,
        Priority priority,
        LocalDate requiredDeliveryDate,
        String additionalSpecifications,
        String remarks,
        boolean needsSupplier,
        RequirementStatus status,
        UUID assignedToId,
        String assignedToName,
        Instant createdAt,
        Instant updatedAt) {

    public static RequirementResponse from(Requirement r) {
        return new RequirementResponse(
                r.getId(),
                r.getLead().getId(),
                r.getLead().getFullName(),
                r.getProductName(),
                r.getDescription(),
                r.getQuantity(),
                r.getUnit(),
                r.getExpectedTimeline(),
                r.getBudget(),
                r.getPriority(),
                r.getRequiredDeliveryDate(),
                r.getAdditionalSpecifications(),
                r.getRemarks(),
                r.isNeedsSupplier(),
                r.getStatus(),
                r.getAssignedTo() != null ? r.getAssignedTo().getId() : null,
                r.getAssignedTo() != null ? r.getAssignedTo().getFullName() : null,
                r.getCreatedAt(),
                r.getUpdatedAt());
    }
}
