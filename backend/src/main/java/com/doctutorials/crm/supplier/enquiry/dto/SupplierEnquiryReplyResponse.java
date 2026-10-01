package com.doctutorials.crm.supplier.enquiry.dto;

import com.doctutorials.crm.supplier.enquiry.SupplierEnquiryReply;
import com.doctutorials.crm.supplier.enquiry.SupplierReplyStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record SupplierEnquiryReplyResponse(
        UUID id,
        UUID supplierId,
        String supplierName,
        String supplierContactEmail,
        SupplierReplyStatus status,
        BigDecimal price,
        String currency,
        LocalDate validUntil,
        String deliveryInfo,
        String supplierReference,
        String terms,
        String remarks,
        boolean chosen,
        Instant emailSentAt,
        Instant repliedAt
) {
    public static SupplierEnquiryReplyResponse from(SupplierEnquiryReply r) {
        return new SupplierEnquiryReplyResponse(
                r.getId(), r.getSupplier().getId(), r.getSupplier().getCompanyName(),
                r.getSupplier().getContactEmail(), r.getStatus(), r.getPrice(), r.getCurrency(),
                r.getValidUntil(), r.getDeliveryInfo(), r.getSupplierReference(), r.getTerms(), r.getRemarks(),
                r.isChosen(), r.getEmailSentAt(), r.getRepliedAt());
    }
}
