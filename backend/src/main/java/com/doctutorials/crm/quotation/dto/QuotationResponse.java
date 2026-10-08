package com.doctutorials.crm.quotation.dto;

import com.doctutorials.crm.quotation.Quotation;
import com.doctutorials.crm.quotation.QuotationStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record QuotationResponse(
        UUID id,
        String quotationNumber,
        UUID customerId,
        String customerName,
        UUID leadId,
        String leadName,
        UUID requirementId,
        String requirementProductName,
        /** customerName if there's a customer, else leadName - one field the UI can
         *  always show without branching on which of the two is set. */
        String displayName,
        UUID customerContactId,
        String customerContactName,
        UUID preparedById,
        String preparedByName,
        LocalDate quotationDate,
        LocalDate validUntil,
        QuotationStatus status,
        String currency,
        BigDecimal taxPercent,
        String termsAndConditions,
        String remarks,
        List<QuotationLineItemResponse> lineItems,
        BigDecimal subtotal,
        BigDecimal taxAmount,
        BigDecimal grandTotal,
        BigDecimal totalMarginAmount,
        /** True when status is ACCEPTED and this quotation is linked to a lead - the
         *  UI uses this to prompt "mark the lead as Won" without the server ever
         *  doing that automatically (QuotationService.changeStatus never touches
         *  LeadStage.WON - that stays a deliberate, human action). */
        boolean canMarkLeadWon,
        Instant createdAt,
        Instant updatedAt
) {
    public static QuotationResponse from(Quotation q) {
        return new QuotationResponse(
                q.getId(), q.getQuotationNumber(),
                q.getCustomer() != null ? q.getCustomer().getId() : null,
                q.getCustomer() != null ? q.getCustomer().getCompanyName() : null,
                q.getLead() != null ? q.getLead().getId() : null,
                q.getLead() != null ? q.getLead().getFullName() : null,
                q.getRequirement() != null ? q.getRequirement().getId() : null,
                q.getRequirement() != null ? q.getRequirement().getProductName() : null,
                q.getCustomer() != null ? q.getCustomer().getCompanyName()
                        : (q.getLead() != null ? q.getLead().getFullName() : null),
                q.getCustomerContact() != null ? q.getCustomerContact().getId() : null,
                q.getCustomerContact() != null ? q.getCustomerContact().getName() : null,
                q.getPreparedBy() != null ? q.getPreparedBy().getId() : null,
                q.getPreparedBy() != null ? q.getPreparedBy().getFullName() : null,
                q.getQuotationDate(), q.getValidUntil(), q.getStatus(), q.getCurrency(), q.getTaxPercent(),
                q.getTermsAndConditions(), q.getRemarks(),
                q.getLineItems().stream().map(QuotationLineItemResponse::from).toList(),
                q.getSubtotal(), q.getTaxAmount(), q.getGrandTotal(), q.getTotalMarginAmount(),
                q.getStatus() == QuotationStatus.ACCEPTED && q.getLead() != null,
                q.getCreatedAt(), q.getUpdatedAt());
    }
}
