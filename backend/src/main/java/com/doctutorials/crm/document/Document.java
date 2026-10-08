package com.doctutorials.crm.document;

import com.doctutorials.crm.common.BaseEntity;
import com.doctutorials.crm.customer.Customer;
import com.doctutorials.crm.lead.Lead;
import com.doctutorials.crm.quotation.Quotation;
import com.doctutorials.crm.requirement.Requirement;
import com.doctutorials.crm.supplier.Supplier;
import com.doctutorials.crm.supplier.enquiry.SupplierEnquiryReply;
import com.doctutorials.crm.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/** Generalized from Lead-only (PRD §20/§12.2) - exactly one of the six
 *  parent references is set, enforced by a DB CHECK (V55), not in Java,
 *  since that's the one place it truly can't be bypassed. */
@Entity
@Table(name = "documents")
@Getter
@Setter
public class Document extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lead_id")
    private Lead lead;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requirement_id")
    private Requirement requirement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quotation_id")
    private Quotation quotation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enquiry_reply_id")
    private SupplierEnquiryReply enquiryReply;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by")
    private User uploadedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false)
    private DocumentType documentType = DocumentType.OTHER;

    @Column(name = "original_file_name", nullable = false)
    private String originalFileName;

    @Column(name = "storage_key", nullable = false, length = 500)
    private String storageKey;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    private String description;

    // Soft delete - PRD: valuable records are never deleted.
    private Instant deletedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deleted_by_id")
    private User deletedBy;
}