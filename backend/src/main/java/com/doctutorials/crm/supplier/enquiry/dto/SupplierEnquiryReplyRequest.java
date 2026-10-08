package com.doctutorials.crm.supplier.enquiry.dto;

import com.doctutorials.crm.supplier.enquiry.SupplierReplyStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Body for PATCH /supplier-enquiries/{enquiryId}/replies/{replyId}. status
 * covers both "log the price they gave us" (REPLIED) and "they can't supply
 * this" (DECLINED). price/currency/validUntil are cross-validated against
 * status in SupplierEnquiryService (REPLIED needs a real price; a currency
 * that's present but malformed is rejected here via @Pattern either way).
 */
public record SupplierEnquiryReplyRequest(
        @NotNull SupplierReplyStatus status,
        BigDecimal price,
        @Pattern(regexp = "[A-Za-z]{3}", message = "Currency must be a 3-letter code, e.g. INR")
        String currency,
        LocalDate validUntil,
        String deliveryInfo,
        String supplierReference,
        String terms,
        String remarks
) {}
