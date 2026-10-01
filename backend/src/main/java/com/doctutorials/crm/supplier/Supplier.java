package com.doctutorials.crm.supplier;

import com.doctutorials.crm.common.BaseEntity;
import com.doctutorials.crm.user.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * A vendor/supplier this business buys from (B2B procurement side), as
 * opposed to Lead which is someone this business sells to. Kept as its own
 * top-level module rather than bolted onto Lead - the two have almost no
 * overlapping fields (bank details, GST, payment terms vs. pipeline stage,
 * score, buying intent) and different visibility rules (master data visible
 * org-wide, not owner-scoped like a lead).
 */
@Entity
@Table(name = "suppliers", indexes = {
        @Index(name = "idx_suppliers_status", columnList = "status"),
        @Index(name = "idx_suppliers_category", columnList = "category"),
        @Index(name = "idx_suppliers_gst_number", columnList = "gstNumber")
})
@Getter
@Setter
public class Supplier extends BaseEntity {

    /** Human-friendly short code shown in tables/exports, e.g. "SUP-0042". Assigned once, never reused. */
    @Column(nullable = false, unique = true, updatable = false)
    private String supplierCode;

    @NotBlank
    @Column(nullable = false)
    private String companyName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SupplierCategory category = SupplierCategory.OTHER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SupplierStatus status = SupplierStatus.PENDING_VERIFICATION;

    // --- Registration / compliance ---
    private String gstNumber;
    private boolean gstVerified = false;
    private String panNumber;

    // --- Address ---
    @NotBlank
    @Column(nullable = false)
    private String addressLine1;
    private String addressLine2;
    @Column(nullable = false)
    private String country = "India";
    private String state;
    private String city;
    private String pincode;

    // --- Banking ---
    private String bankName;
    private String bankAddress;
    private String accountHolderName;
    private String accountNumber;
    private String ifscCode;

    // --- Primary contact ---
    private String contactName;
    private String contactDesignation;
    private String contactEmail;
    private String contactPhone;

    @Enumerated(EnumType.STRING)
    private PaymentTerms paymentTerms;

    /** Products/services this supplier provides - free-tag list, matches the "+" add-product UI. */
    @ElementCollection
    @CollectionTable(name = "supplier_products", joinColumns = @JoinColumn(name = "supplier_id"))
    @Column(name = "product_name", nullable = false)
    private List<String> products = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    private User createdBy;

    /** 0-100, recomputed by SupplierScoringService on every create/update. Drives the "Profile strength" bar. */
    @Column(nullable = false)
    private int trustScore = 0;

    /** Short auto-generated note on what's missing / what boosted the score, e.g. "Add bank details to reach 100%." */
    @Column(length = 500)
    private String scoreNote;

    private boolean archived = false;
}
