package com.doctutorials.crm.quotation;

import com.doctutorials.crm.common.BaseEntity;
import com.doctutorials.crm.supplier.Supplier;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * One product/service row on a Quotation. Carries its own optional Supplier +
 * supplierPrice link so the quotation keeps the "price trail" PRD §12.3 asks
 * for - every line can show where its cost came from, even though the fuller
 * RFQ/enquiry-reply workflow (PRD §12.2) is a separate, not-yet-built feature.
 * productName is free text rather than a link to a Product entity, since
 * there's no Product master list yet either (PRD §21) - see this module's
 * chat notes for that gap.
 */
@Entity
@Table(name = "quotation_line_items", indexes = {
        @Index(name = "idx_quotation_line_items_quotation", columnList = "quotation_id")
})
@Getter
@Setter
public class QuotationLineItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quotation_id")
    private Quotation quotation;

    /** Stable row order within the quotation - assigned by QuotationService.syncLineItems. */
    @Column(nullable = false)
    private int sortOrder;

    @NotBlank
    @Column(nullable = false)
    private String productName;

    private String description;

    @Column(nullable = false)
    private String unit = "Nos";

    @Column(nullable = false, precision = 14, scale = 3)
    private BigDecimal quantity;

    /** Where this line's cost was sourced from - optional link to the Supplier module. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    /** Cost price per unit, as quoted by the supplier. Null when sourced internally / no supplier involved. */
    @Column(precision = 14, scale = 2)
    private BigDecimal supplierPrice;

    /** Unit price shown to the customer. */
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal sellingPrice;

    // --- Computed by QuotationService.recomputeTotals - never trust a client-sent value. ---
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal lineTotal = BigDecimal.ZERO;
    /** Null (not zero) when there's no supplierPrice to compare against, so the UI can tell
     *  "no margin data" apart from "zero margin". */
    @Column(precision = 14, scale = 2)
    private BigDecimal marginAmount;
    @Column(precision = 5, scale = 2)
    private BigDecimal marginPercent;

    @Column(length = 500)
    private String remarks;
}
