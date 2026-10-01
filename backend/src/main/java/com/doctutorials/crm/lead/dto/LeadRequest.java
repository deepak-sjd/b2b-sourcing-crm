package com.doctutorials.crm.lead.dto;

import com.doctutorials.crm.lead.Priority;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.util.UUID;

public record LeadRequest(
        @NotBlank String fullName,
        String phone,
        String email,
        String city,
        String interest,
        BigDecimal budget,
        Priority priority,
        String source,
        String campaign,
        UUID ownerId
) {}
