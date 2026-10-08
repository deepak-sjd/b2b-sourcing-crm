package com.doctutorials.crm.supplier.enquiry.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record SupplierEnquiryRequest(
        @NotNull UUID requirementId,
        @NotEmpty @Size(max = 20, message = "An enquiry can go to at most 20 suppliers at once")
        List<UUID> supplierIds,
        /** Optional extra note appended to the enquiry email, e.g. "need this by Friday". */
        String message,
        LocalDate responseDueDate
) {}
