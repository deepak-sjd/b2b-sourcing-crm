package com.doctutorials.crm.quotation.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record QuotationLineItemRequest(
        /** Present on update of an existing row; null for a new one - same
         *  id-based upsert pattern as CustomerContactRequest. */
        UUID id,
        @NotBlank String productName,
        String description,
        String unit,
        @NotNull @DecimalMin(value = "0.001", message = "Quantity must be greater than 0") BigDecimal quantity,
        UUID supplierId,
        @DecimalMin(value = "0", message = "Supplier price can't be negative") BigDecimal supplierPrice,
        @NotNull @DecimalMin(value = "0", message = "Selling price can't be negative") BigDecimal sellingPrice,
        String remarks
) {}
