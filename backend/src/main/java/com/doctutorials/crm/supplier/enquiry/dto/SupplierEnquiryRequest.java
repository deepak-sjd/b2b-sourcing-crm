package com.doctutorials.crm.supplier.enquiry.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record SupplierEnquiryRequest(
        @NotNull UUID leadId,
        @NotBlank String itemDescription,
        String quantity,
        String notes,
        /** Send to one or more suppliers at once — PRD 12.2: "Send one enquiry
         *  by email to all suppliers, or to selected suppliers." */
        @NotEmpty List<UUID> supplierIds
) {}
