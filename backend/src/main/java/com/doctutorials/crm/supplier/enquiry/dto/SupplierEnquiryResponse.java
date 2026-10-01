package com.doctutorials.crm.supplier.enquiry.dto;

import com.doctutorials.crm.supplier.enquiry.SupplierEnquiry;
import com.doctutorials.crm.supplier.enquiry.SupplierEnquiryStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SupplierEnquiryResponse(
        UUID id,
        String enquiryCode,
        UUID leadId,
        String leadName,
        String itemDescription,
        String quantity,
        String notes,
        SupplierEnquiryStatus status,
        String createdByName,
        Instant createdAt,
        List<SupplierEnquiryReplyResponse> replies
) {
    public static SupplierEnquiryResponse from(SupplierEnquiry e) {
        return new SupplierEnquiryResponse(
                e.getId(), e.getEnquiryCode(), e.getLead().getId(), e.getLead().getFullName(),
                e.getItemDescription(), e.getQuantity(), e.getNotes(), e.getStatus(),
                e.getCreatedBy() != null ? e.getCreatedBy().getFullName() : null, e.getCreatedAt(),
                e.getReplies().stream().map(SupplierEnquiryReplyResponse::from).toList());
    }
}
