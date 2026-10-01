package com.doctutorials.crm.supplier.enquiry.dto;

import com.doctutorials.crm.supplier.enquiry.SupplierReplyStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Body for PATCH /supplier-enquiries/{enquiryId}/replies/{replyId}. status is
 * included so the same endpoint covers both "log the price they gave us"
 * (REPLIED) and "they can't supply this" (DECLINED) — the UI's reply form
 * has both actions, not just a price field.
 */
public record SupplierEnquiryReplyRequest(
        SupplierReplyStatus status,
        BigDecimal price,
        String currency,
        LocalDate validUntil,
        String deliveryInfo,
        String supplierReference,
        String terms,
        String remarks
) {}
