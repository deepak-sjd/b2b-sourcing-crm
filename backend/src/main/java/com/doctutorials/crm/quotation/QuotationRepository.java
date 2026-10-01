package com.doctutorials.crm.quotation;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface QuotationRepository extends JpaRepository<Quotation, UUID>, JpaSpecificationExecutor<Quotation> {

    // Ordinary list/lookup flows go through QuotationService + QuotationAccessService's
    // visibility Specification - same discipline as LeadRepository/CustomerRepository.

    Page<Quotation> findByCustomerIdOrderByCreatedAtDesc(UUID customerId, Pageable pageable);
}
