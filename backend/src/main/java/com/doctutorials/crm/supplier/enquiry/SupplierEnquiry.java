package com.doctutorials.crm.supplier.enquiry;

import com.doctutorials.crm.common.BaseEntity;
import com.doctutorials.crm.lead.Lead;
import com.doctutorials.crm.user.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * PRD 12.2's "Customer Requirement -> Supplier Enquiry" step: raised when a
 * lead needs something not currently available in-house, so it's sourced
 * from one or more suppliers. Attached to Lead (the active deal), not
 * Customer - sourcing happens while a deal is still being worked, before it's
 * won and a Customer record exists. Visibility is therefore "visible exactly
 * when its lead is visible", the same rule Task/Note/Document already use -
 * see LeadAccessService's class comment and SupplierEnquiryService.getById.
 */
@Entity
@Table(name = "supplier_enquiries", indexes = {
        @Index(name = "idx_supplier_enquiries_lead_id", columnList = "lead_id"),
        @Index(name = "idx_supplier_enquiries_status", columnList = "status")
})
@Getter
@Setter
public class SupplierEnquiry extends BaseEntity {

    /** Human-friendly short code, e.g. "ENQ-0042". Assigned once, never reused. */
    @Column(name = "enquiry_code", nullable = false, unique = true, updatable = false)
    private String enquiryCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lead_id")
    private Lead lead;

    @NotBlank
    @Column(name = "item_description", nullable = false, length = 500)
    private String itemDescription;

    @Column(length = 100)
    private String quantity;

    @Column(length = 2000)
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SupplierEnquiryStatus status = SupplierEnquiryStatus.OPEN;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    private User createdBy;

    @OneToMany(mappedBy = "enquiry", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<SupplierEnquiryReply> replies = new ArrayList<>();
}
