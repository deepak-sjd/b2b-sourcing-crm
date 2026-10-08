package com.doctutorials.crm.quotation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record QuotationRequest(
        /** Exactly one of customerId/leadId must be given — QuotationService.create
         *  enforces this (matches the DB CHECK constraint from V16). */
        UUID customerId,
        UUID leadId,
        /** Optional - which of the lead's requirements this quotation answers. */
        UUID requirementId,
        UUID customerContactId,
        LocalDate quotationDate,
        LocalDate validUntil,
        String currency,
        BigDecimal taxPercent,
        String termsAndConditions,
        String remarks,
        @NotEmpty(message = "Add at least one line item") @Valid List<QuotationLineItemRequest> lineItems
) {}
