package com.doctutorials.crm.supplier.enquiry;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SupplierEnquiryReplyRepository
        extends JpaRepository<SupplierEnquiryReply, UUID>, JpaSpecificationExecutor<SupplierEnquiryReply> {

    List<SupplierEnquiryReply> findByEnquiryId(UUID enquiryId);

    /** Every reply currently chosen for a requirement - used by chooseReply to
     *  clear the previous choice, which may live on a *different* enquiry than
     *  the one being acted on (a requirement can be re-sourced more than once). */
    List<SupplierEnquiryReply> findByRequirementIdAndChosenTrue(UUID requirementId);

    /** Backs QuotationService.prefill - "the chosen price for this requirement".
     *  Picks the most recent if a requirement somehow has more than one chosen
     *  reply across different enquiries (shouldn't normally happen, but an
     *  enquiry can be re-raised). */
    Optional<SupplierEnquiryReply> findFirstByEnquiry_RequirementIdAndChosenTrueOrderByCreatedAtDesc(
            UUID requirementId);

    /** Backs the supplier-side "previous transactions" history (PRD 12.1) - supplier
     *  master data is org-wide visible, so this one is fine to query directly. */
    Page<SupplierEnquiryReply> findBySupplierIdOrderByCreatedAtDesc(UUID supplierId, Pageable pageable);
}
