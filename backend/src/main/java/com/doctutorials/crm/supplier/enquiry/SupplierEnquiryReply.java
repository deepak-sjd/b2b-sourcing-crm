package com.doctutorials.crm.supplier.enquiry;

import com.doctutorials.crm.common.BaseEntity;
import com.doctutorials.crm.requirement.Requirement;
import com.doctutorials.crm.supplier.Supplier;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * One supplier's slot within a SupplierEnquiry - created PENDING for every
 * supplier the enquiry is sent to, then filled in once their price comes
 * back (PRD 12.2's "Supplier Reply" fields). At most one reply per
 * requirement has chosen=true: that's the price PRD 12.3 says every customer
 * quotation should be able to show as its reference.
 *
 * requirement is denormalized from enquiry.requirement purely so the
 * DB can enforce "at most one chosen reply per requirement" (see V16's
 * partial unique index) across every enquiry ever raised for it, not just
 * within one - a requirement can be re-sourced into a second enquiry later
 * and the uniqueness rule must still hold globally for that requirement.
 */
@Entity
@Table(name = "supplier_enquiry_replies", indexes = {
        @Index(name = "idx_supplier_enquiry_replies_enquiry_id", columnList = "enquiry_id"),
        @Index(name = "idx_supplier_enquiry_replies_supplier_id", columnList = "supplier_id"),
        @Index(name = "idx_supplier_enquiry_replies_requirement_id", columnList = "requirement_id")
})
@Getter
@Setter
public class SupplierEnquiryReply extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "enquiry_id")
    private SupplierEnquiry enquiry;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requirement_id")
    private Requirement requirement;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SupplierReplyStatus status = SupplierReplyStatus.PENDING;

    @Column(precision = 14, scale = 2)
    private BigDecimal price;

    @Column(nullable = false, length = 10)
    private String currency = "INR";

    private LocalDate validUntil;

    @Column(name = "delivery_info", length = 500)
    private String deliveryInfo;

    /** PRD's "Supplier quotation / reference number" - the supplier's own doc number, not ours. */
    @Column(name = "supplier_reference", length = 100)
    private String supplierReference;

    @Column(length = 1000)
    private String terms;

    @Column(length = 1000)
    private String remarks;

    /** True on at most one reply per requirement (see class comment) - the price
     *  used as the customer quotation's reference (PRD 12.3). */
    @Column(nullable = false)
    private boolean chosen = false;

    /** When the enquiry email actually went out to this supplier (best-effort - stays
     *  null if the supplier has no email on file, or if delivery failed; see MailService). */
    @Column(name = "email_sent_at")
    private Instant emailSentAt;

    /** Set when sendSupplierEnquiryEmail returns false - kept so the UI can show
     *  "delivery failed" instead of silently looking like nothing happened. */
    @Column(name = "email_error", length = 500)
    private String emailError;

    @Column(name = "replied_at")
    private Instant repliedAt;
}
