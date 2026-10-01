package com.doctutorials.crm.supplier.dto;

import com.doctutorials.crm.supplier.PaymentTerms;
import com.doctutorials.crm.supplier.SupplierCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record SupplierRequest(
        @NotBlank String companyName,
        @NotNull SupplierCategory category,
        String gstNumber,
        String panNumber,
        @NotBlank String addressLine1,
        String addressLine2,
        String country,
        String state,
        String city,
        String pincode,
        String bankName,
        String bankAddress,
        String accountHolderName,
        String accountNumber,
        String ifscCode,
        String contactName,
        String contactDesignation,
        String contactEmail,
        String contactPhone,
        PaymentTerms paymentTerms,
        List<String> products
) {}
