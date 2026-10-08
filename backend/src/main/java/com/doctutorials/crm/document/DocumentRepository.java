package com.doctutorials.crm.document;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DocumentRepository extends JpaRepository<Document, UUID> {
    Page<Document> findByLeadIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID leadId, Pageable pageable);
    Page<Document> findByCustomerIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID customerId, Pageable pageable);
    Page<Document> findBySupplierIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID supplierId, Pageable pageable);
    Page<Document> findByRequirementIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID requirementId, Pageable pageable);
    Page<Document> findByQuotationIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID quotationId, Pageable pageable);
    Page<Document> findByEnquiryReplyIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID enquiryReplyId, Pageable pageable);
}