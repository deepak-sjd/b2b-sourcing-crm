package com.doctutorials.crm.supplier.enquiry;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SupplierEnquiryReplyRepository extends JpaRepository<SupplierEnquiryReply, UUID> {
    List<SupplierEnquiryReply> findByEnquiryId(UUID enquiryId);

    /** Backs the supplier-side "previous transactions" history (PRD 12.1) — supplier
     *  master data is org-wide visible, so this one is fine to query directly. */
    Page<SupplierEnquiryReply> findBySupplierIdOrderByCreatedAtDesc(UUID supplierId, Pageable pageable);
}
