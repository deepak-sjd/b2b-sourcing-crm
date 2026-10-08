package com.doctutorials.crm.quotation.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Shape returned by GET /quotations/prefill?requirementId= — everything needed to
 *  drop one pre-filled, price-trail-locked line item into the quotation builder. */
public record QuotationPrefillResponse(
        UUID leadId,
        String leadName,
        /** Set if the lead has already been converted - lets the UI default to
         *  building the quotation against the customer instead of the lead. */
        UUID customerId,
        String productName,
        BigDecimal quantity,
        String unit,
        String currency,
        UUID supplierId,
        String supplierName,
        BigDecimal supplierPrice,
        UUID enquiryReplyId,
        String supplierReference,
        String enquiryCode,
        LocalDate replyValidUntil
) {}
