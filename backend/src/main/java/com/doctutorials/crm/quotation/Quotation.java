package com.doctutorials.crm.quotation;

import com.doctutorials.crm.common.BaseEntity;
import com.doctutorials.crm.customer.Customer;
import com.doctutorials.crm.customer.CustomerContact;
import com.doctutorials.crm.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * A customer-facing quotation — PRD §13. Deliberately keyed off Customer (not
 * Lead): by the time a quotation exists, PRD §4's workflow already has the
 * lead through the funnel and into a customer requirement/opportunity, so
 * every quotation belongs to a customer record. Each quotation carries its
 * own multi-line item list (see QuotationLineItem) rather than one product
 * per quotation, since real B2B quotes bundle several products/services with
 * one grand total.
 */
@Entity
@Table(name = "quotations", indexes = {
        @Index(name = "idx_quotations_customer", columnList = "customer_id"),
        @Index(name = "idx_quotations_status", columnList = "status")
})
@Getter
@Setter
public class Quotation extends BaseEntity {

    /** Server-generated, e.g. "QT-2026-0001" — see QuotationService.generateNumber. */
    @Column(nullable = false, unique = true, updatable = false)
    private String quotationNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    /** Who to address the quotation to — optional, one of that customer's contacts. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_contact_id")
    private CustomerContact customerContact;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prepared_by_id")
    private User preparedBy;

    @Column(nullable = false)
    private LocalDate quotationDate = LocalDate.now();

    private LocalDate validUntil;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QuotationStatus status = QuotationStatus.DRAFT;

    @Column(nullable = false)
    private String currency = "INR";

    /** Flat tax rate applied to the subtotal (e.g. GST %). Null/zero = no tax line. */
    private BigDecimal taxPercent;

    @Column(length = 2000)
    private String termsAndConditions;

    @Column(length = 1000)
    private String remarks;

    @OneToMany(mappedBy = "quotation", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("sortOrder asc")
    private List<QuotationLineItem> lineItems = new ArrayList<>();

    // --- Totals, recomputed server-side on every create/update by
    // QuotationService.recomputeTotals - never trust a client-sent total. ---
    @Column(nullable = false)
    private BigDecimal subtotal = BigDecimal.ZERO;
    @Column(nullable = false)
    private BigDecimal taxAmount = BigDecimal.ZERO;
    @Column(nullable = false)
    private BigDecimal grandTotal = BigDecimal.ZERO;
    /** Sum of each line's margin - only meaningful for items that have a supplier price set. */
    @Column(nullable = false)
    private BigDecimal totalMarginAmount = BigDecimal.ZERO;

    public void addLineItem(QuotationLineItem item) {
        item.setQuotation(this);
        lineItems.add(item);
    }
}
