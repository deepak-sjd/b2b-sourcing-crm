package com.doctutorials.crm.supplier.enquiry;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SupplierEnquiryRepository extends JpaRepository<SupplierEnquiry, UUID> {
    // No visibility scoping needed at this layer - every call site reaches this
    // via SupplierEnquiryService, which always checks the parent lead's
    // visibility first (see getById/listByLead/listByRequirement).

    @EntityGraph(attributePaths = { "lead", "requirement", "createdBy" })
    Page<SupplierEnquiry> findByLeadId(UUID leadId, Pageable pageable);

    @EntityGraph(attributePaths = { "lead", "requirement", "createdBy" })
    List<SupplierEnquiry> findByRequirementIdOrderByCreatedAtDesc(UUID requirementId);
}
