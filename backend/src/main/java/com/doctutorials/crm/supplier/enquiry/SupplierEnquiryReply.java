package com.doctutorials.crm.supplier.enquiry;

import com.doctutorials.crm.common.BaseEntity;
import com.doctutorials.crm.supplier.Supplier;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * One supplier's slot within a SupplierEnquiry - created PENDING for every
 * supplier the enquiry is sent to, then filled in (PRD 12.2's "Supplier
 * Reply" fields) once their price comes back. At most one reply per enquiry
 * has chosen=true: that's the price PRD 12.3 says every customer quotation
 * should be able to show as its reference. Enforced in
 * SupplierEnquiryService.chooseReply (unset the rest, then set this one) -
 * not a DB constraint, since "exactly one chosen, or none yet" is easier to
 * keep correct in one transactional method than to express as SQL here.
 */
@Entity
@Table(name = "supplier_enquiry_replies", indexes = {
        @Index(name = "idx_supplier_enquiry_replies_enquiry_id", columnList = "enquiry_id"),
        @Index(name = "idx_supplier_enquiry_replies_supplier_id", columnList = "supplier_id")
})
@Getter
@Setter
public class SupplierEnquiryReply extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "enquiry_id")
    private SupplierEnquiry enquiry;

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

    /** PRD's "Supplier quotation / reference number" — the supplier's own doc number, not ours. */
    @Column(name = "supplier_reference", length = 100)
    private String supplierReference;

    @Column(length = 1000)
    private String terms;

    @Column(length = 1000)
    private String remarks;

    /** True on at most one reply per enquiry — the price used in the customer quotation. */
    @Column(nullable = false)
    private boolean chosen = false;

    /** When the enquiry email actually went out to this supplier (best-effort — stays
     *  null if the supplier has no email on file, or if delivery failed; see MailService). */
    @Column(name = "email_sent_at")
    private Instant emailSentAt;

    @Column(name = "replied_at")
    private Instant repliedAt;
}
