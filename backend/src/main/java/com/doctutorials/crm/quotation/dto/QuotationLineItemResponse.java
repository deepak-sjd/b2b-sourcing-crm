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
        String remarks
) {
    public static QuotationLineItemResponse from(QuotationLineItem i) {
        return new QuotationLineItemResponse(
                i.getId(), i.getProductName(), i.getDescription(), i.getUnit(), i.getQuantity(),
                i.getSupplier() != null ? i.getSupplier().getId() : null,
                i.getSupplier() != null ? i.getSupplier().getCompanyName() : null,
                i.getSupplierPrice(), i.getSellingPrice(), i.getLineTotal(), i.getMarginAmount(), i.getMarginPercent(),
                i.getRemarks());
    }
}
