package com.doctutorials.crm.quotation;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface QuotationRepository extends JpaRepository<Quotation, UUID>, JpaSpecificationExecutor<Quotation> {

    // Ordinary list/lookup flows go through QuotationService + QuotationAccessService's
    // visibility Specification - same discipline as LeadRepository/CustomerRepository.

    Page<Quotation> findByCustomerIdOrderByCreatedAtDesc(UUID customerId, Pageable pageable);

    Page<Quotation> findByLeadIdOrderByCreatedAtDesc(UUID leadId, Pageable pageable);

    /** Used by QuotationExpiryJob - every quotation that's lapsed but not yet marked so. */
    List<Quotation> findByValidUntilBeforeAndStatusIn(LocalDate date, List<QuotationStatus> statuses);

    /** Used by LeadConversionService to re-point a converted lead's quotations at its new customer. */
    List<Quotation> findByLeadId(UUID leadId);
}
