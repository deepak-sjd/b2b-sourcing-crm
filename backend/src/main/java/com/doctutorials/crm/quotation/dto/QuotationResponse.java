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
        Instant createdAt,
        Instant updatedAt
) {
    public static QuotationResponse from(Quotation q) {
        return new QuotationResponse(
                q.getId(), q.getQuotationNumber(),
                q.getCustomer().getId(), q.getCustomer().getCompanyName(),
                q.getCustomerContact() != null ? q.getCustomerContact().getId() : null,
                q.getCustomerContact() != null ? q.getCustomerContact().getName() : null,
                q.getPreparedBy() != null ? q.getPreparedBy().getId() : null,
                q.getPreparedBy() != null ? q.getPreparedBy().getFullName() : null,
                q.getQuotationDate(), q.getValidUntil(), q.getStatus(), q.getCurrency(), q.getTaxPercent(),
                q.getTermsAndConditions(), q.getRemarks(),
                q.getLineItems().stream().map(QuotationLineItemResponse::from).toList(),
                q.getSubtotal(), q.getTaxAmount(), q.getGrandTotal(), q.getTotalMarginAmount(),
                q.getCreatedAt(), q.getUpdatedAt());
    }
}
