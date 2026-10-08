package com.doctutorials.crm.supplier.enquiry.dto;

import com.doctutorials.crm.supplier.enquiry.SupplierEnquiry;
import com.doctutorials.crm.supplier.enquiry.SupplierEnquiryStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record SupplierEnquiryResponse(
        UUID id,
        String enquiryCode,
        UUID requirementId,
        UUID leadId,
        String leadName,
        String itemDescription,
        String quantity,
        String notes,
        SupplierEnquiryStatus status,
        String createdByName,
        Instant createdAt,
        LocalDate responseDueDate,
        List<SupplierEnquiryReplyResponse> replies
) {
    public static SupplierEnquiryResponse from(SupplierEnquiry e) {
        return new SupplierEnquiryResponse(
                e.getId(), e.getEnquiryCode(), e.getRequirement() != null ? e.getRequirement().getId() : null, e.getLead().getId(),
                e.getLead().getFullName(), e.getItemDescription(), e.getQuantity(), e.getNotes(), e.getStatus(),
                e.getCreatedBy() != null ? e.getCreatedBy().getFullName() : null, e.getCreatedAt(),
                e.getResponseDueDate(),
                e.getReplies().stream().map(r -> SupplierEnquiryReplyResponse.from(r, e)).toList());
    }
}
