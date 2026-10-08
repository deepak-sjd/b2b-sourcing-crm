package com.doctutorials.crm.quotation.dto;

import com.doctutorials.crm.quotation.QuotationLineItem;

import java.math.BigDecimal;
import java.util.UUID;

public record QuotationLineItemResponse(
        UUID id,
        String productName,
        String description,
        String unit,
        BigDecimal quantity,
        UUID supplierId,
        String supplierName,
        BigDecimal supplierPrice,
        BigDecimal sellingPrice,
        BigDecimal lineTotal,
        BigDecimal marginAmount,
        BigDecimal marginPercent,
        /** Set when supplier/supplierPrice above were taken from a chosen enquiry
         *  reply rather than typed by hand - the UI locks supplierPrice and shows
         *  this trio as a read-only "price source" when non-null. */
        UUID enquiryReplyId,
        String supplierReference,
        String enquiryCode,
        String remarks
) {
    public static QuotationLineItemResponse from(QuotationLineItem i) {
        boolean fromReply = i.getEnquiryReply() != null;
        return new QuotationLineItemResponse(
                i.getId(), i.getProductName(), i.getDescription(), i.getUnit(), i.getQuantity(),
                i.getSupplier() != null ? i.getSupplier().getId() : null,
                i.getSupplier() != null ? i.getSupplier().getCompanyName() : null,
                i.getSupplierPrice(), i.getSellingPrice(), i.getLineTotal(), i.getMarginAmount(), i.getMarginPercent(),
                fromReply ? i.getEnquiryReply().getId() : null,
                fromReply ? i.getEnquiryReply().getSupplierReference() : null,
                fromReply ? i.getEnquiryReply().getEnquiry().getEnquiryCode() : null,
                i.getRemarks());
    }
}
