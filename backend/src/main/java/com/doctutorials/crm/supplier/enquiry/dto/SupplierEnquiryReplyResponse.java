package com.doctutorials.crm.supplier.enquiry.dto;

import com.doctutorials.crm.supplier.SupplierStatus;
import com.doctutorials.crm.supplier.enquiry.SupplierEnquiryReply;
import com.doctutorials.crm.supplier.enquiry.SupplierReplyStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record SupplierEnquiryReplyResponse(
        UUID id,
        UUID enquiryId,
        UUID requirementId,
        UUID supplierId,
        String supplierCode,
        String supplierName,
        String supplierContactEmail,
        SupplierStatus supplierStatus,
        SupplierReplyStatus status,
        boolean emailSent,
        String emailError,
        Instant emailSentAt,
        BigDecimal price,
        String currency,
        LocalDate validUntil,
        boolean expired,
        String deliveryInfo,
        String supplierReference,
        String terms,
        String remarks,
        boolean chosen,
        Instant repliedAt,
        // Extra context fields present only on the /by-supplier history listing,
        // where a flat reply list (not grouped under its enquiry) needs them to
        // be self-describing. Null on every other endpoint.
        String enquiryCode,
        String itemDescription,
        String leadName
) {
    public static SupplierEnquiryReplyResponse from(SupplierEnquiryReply r) {
        return from(r, null);
    }

    public static SupplierEnquiryReplyResponse from(SupplierEnquiryReply r, com.doctutorials.crm.supplier.enquiry.SupplierEnquiry enquiryContext) {
        boolean expired = r.getValidUntil() != null && r.getValidUntil().isBefore(LocalDate.now());
        var enquiry = enquiryContext != null ? enquiryContext : r.getEnquiry();
        return new SupplierEnquiryReplyResponse(
                r.getId(), enquiry.getId(), r.getRequirement().getId(),
                r.getSupplier().getId(), r.getSupplier().getSupplierCode(), r.getSupplier().getCompanyName(),
                r.getSupplier().getContactEmail(), r.getSupplier().getStatus(),
                r.getStatus(), r.getEmailSentAt() != null, r.getEmailError(), r.getEmailSentAt(),
                r.getPrice(), r.getCurrency(), r.getValidUntil(), expired,
                r.getDeliveryInfo(), r.getSupplierReference(), r.getTerms(), r.getRemarks(),
                r.isChosen(), r.getRepliedAt(),
                enquiryContext == null ? enquiry.getEnquiryCode() : null,
                enquiryContext == null ? enquiry.getItemDescription() : null,
                enquiryContext == null ? enquiry.getLead().getFullName() : null);
    }
}
