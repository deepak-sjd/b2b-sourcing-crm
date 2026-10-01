package com.doctutorials.crm.requirement.dto;

import com.doctutorials.crm.lead.Priority;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Used for both create and update. leadId is required on create and ignored on update. */
public record RequirementRequest(
        UUID leadId,
        @NotBlank @Size(max = 255) String productName,
        @Size(max = 2000) String description,
        @DecimalMin(value = "0.0", inclusive = false) BigDecimal quantity,
        @Size(max = 30) String unit,
        @Size(max = 120) String expectedTimeline,
        @DecimalMin("0.0") BigDecimal budget,
        Priority priority,
        LocalDate requiredDeliveryDate,
        @Size(max = 2000) String additionalSpecifications,
        @Size(max = 2000) String remarks,
        Boolean needsSupplier,
        UUID assignedToId) {
}
