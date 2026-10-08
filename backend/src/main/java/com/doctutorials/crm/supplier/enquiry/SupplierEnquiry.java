package com.doctutorials.crm.supplier.enquiry;

import com.doctutorials.crm.common.BaseEntity;
import com.doctutorials.crm.lead.Lead;
import com.doctutorials.crm.requirement.Requirement;
import com.doctutorials.crm.user.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * PRD 12.2's "Customer Requirement -> Supplier Enquiry" step: raised when a
 * Requirement needs sourcing from one or more suppliers. Links to both the
 * Requirement (what's being sourced - PRD 8/12.2's "keep the link between
 * the customer requirement and the supplier reply") and, denormalized, its
 * Lead (the active deal; visibility is still derived from the Requirement's
 * lead, never stored independently - see SupplierEnquiryService).
 *
 * Attached to Lead, not Customer - sourcing happens while a deal is still
 * being worked, before it's won and a Customer record exists. Visibility is
 * therefore "visible exactly when its lead is visible", the same rule
 * Task/Note/Document already use - see LeadAccessService's class comment and
 * SupplierEnquiryService.getById.
 *
 * itemDescription/quantity are a snapshot taken from the Requirement at
 * creation time, not a live reference - if the requirement is edited later,
 * past enquiries should keep showing what was actually asked for.
 */
@Entity
@Table(name = "supplier_enquiries", indexes = {
        @Index(name = "idx_supplier_enquiries_lead_id", columnList = "lead_id"),
        @Index(name = "idx_supplier_enquiries_status", columnList = "status"),
        @Index(name = "idx_supplier_enquiries_requirement_id", columnList = "requirement_id")
})
@Getter
@Setter
public class SupplierEnquiry extends BaseEntity {

    /** Sequence-backed, e.g. "ENQ-000042". Assigned once, never reused. */
    @Column(name = "enquiry_code", nullable = false, unique = true, updatable = false)
    private String enquiryCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requirement_id")
    private Requirement requirement;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lead_id")
    private Lead lead;

    /** Snapshot of Requirement.productName at creation time. */
    @NotBlank
    @Column(name = "item_description", nullable = false, length = 500)
    private String itemDescription;

    /** Snapshot of Requirement.quantity + unit, formatted as text (e.g. "500 kg"). */
    @Column(length = 100)
    private String quantity;

    @Column(length = 2000)
    private String notes;

    @Column(name = "response_due_date")
    private LocalDate responseDueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SupplierEnquiryStatus status = SupplierEnquiryStatus.OPEN;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    private User createdBy;

    @OneToMany(mappedBy = "enquiry", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<SupplierEnquiryReply> replies = new ArrayList<>();
}
