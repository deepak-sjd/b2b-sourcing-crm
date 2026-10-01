package com.doctutorials.crm.supplier.dto;

import com.doctutorials.crm.supplier.PaymentTerms;
import com.doctutorials.crm.supplier.Supplier;
import com.doctutorials.crm.supplier.SupplierCategory;
import com.doctutorials.crm.supplier.SupplierStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SupplierResponse(
        UUID id,
        String supplierCode,
        String companyName,
        SupplierCategory category,
        SupplierStatus status,
        String gstNumber,
        boolean gstVerified,
        String panNumber,
        String addressLine1,
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
        List<String> products,
        String createdByName,
        int trustScore,
        String scoreNote,
        Instant createdAt,
        Instant updatedAt
) {
    public static SupplierResponse from(Supplier s) {
        return new SupplierResponse(
                s.getId(), s.getSupplierCode(), s.getCompanyName(), s.getCategory(), s.getStatus(),
                s.getGstNumber(), s.isGstVerified(), s.getPanNumber(),
                s.getAddressLine1(), s.getAddressLine2(), s.getCountry(), s.getState(), s.getCity(), s.getPincode(),
                s.getBankName(), s.getBankAddress(), s.getAccountHolderName(), s.getAccountNumber(), s.getIfscCode(),
                s.getContactName(), s.getContactDesignation(), s.getContactEmail(), s.getContactPhone(),
                s.getPaymentTerms(), s.getProducts(),
                s.getCreatedBy() != null ? s.getCreatedBy().getFullName() : null,
                s.getTrustScore(), s.getScoreNote(),
                s.getCreatedAt(), s.getUpdatedAt()
        );
    }
}
