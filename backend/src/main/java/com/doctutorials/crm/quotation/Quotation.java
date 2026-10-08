package com.doctutorials.crm.quotation;

import com.doctutorials.crm.common.BaseEntity;
import com.doctutorials.crm.customer.Customer;
import com.doctutorials.crm.customer.CustomerContact;
import com.doctutorials.crm.lead.Lead;
import com.doctutorials.crm.requirement.Requirement;
import com.doctutorials.crm.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * A quotation — PRD §13. Originally keyed off Customer only, on the
 * assumption a quotation only exists once a lead is Won. That assumption was
 * wrong: the funnel has a Quotation stage BEFORE Won/Lost (PRD §10), so a
 * quotation needs to be preparable for an active Lead too, before any
 * Customer record exists. customer and lead are therefore both nullable, with
 * exactly one required (DB CHECK constraint in V16 + validated in
 * QuotationService) — a quotation converted along with its lead (see
 * LeadConversionService) ends up with both set rather than losing the lead
 * link. Each quotation carries its own multi-line item list (see
 * QuotationLineItem) rather than one product per quotation, since real B2B
 * quotes bundle several products/services with one grand total.
 */
@Entity
@Table(name = "quotations", indexes = {
        @Index(name = "idx_quotations_customer", columnList = "customer_id"),
        @Index(name = "idx_quotations_lead", columnList = "lead_id"),
        @Index(name = "idx_quotations_status", columnList = "status")
})
@Getter
@Setter
public class Quotation extends BaseEntity {

    /** Server-generated, e.g. "QT-2026-0001" — see QuotationService.generateNumber. */
    @Column(nullable = false, unique = true, updatable = false)
    private String quotationNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    /** Set when this quotation was prepared against an active deal rather than
     *  an already-won Customer. See LeadConversionService for how this and
     *  customer end up both set once the lead converts. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lead_id")
    private Lead lead;

    /** Which of the lead's requirements this quotation answers — optional, set when
     *  the quotation was built from the "Start from requirement" prefill flow. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requirement_id")
    private Requirement requirement;

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
