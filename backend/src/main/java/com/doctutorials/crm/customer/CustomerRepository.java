package com.doctutorials.crm.customer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface CustomerRepository extends JpaRepository<Customer, UUID>, JpaSpecificationExecutor<Customer> {

    // Same discipline as LeadRepository: every list/lookup used by ordinary
    // CRUD flows goes through CustomerService + CustomerAccessService's
    // visibility Specification, not a derived query sitting here.
    //
    // The two methods below are deliberate, narrow exceptions:
    //
    // findPossibleDuplicates backs CustomerDuplicateService, which must see
    // across ALL customers regardless of the caller's own visibility scope —
    // the whole point is to catch a company someone else already entered.
    // It's never used to render a list, only to return a short "you might
    // mean..." warning.
    @Query("select c from Customer c where lower(c.companyName) = lower(:companyName) " +
            "or (:gstNo is not null and c.gstNo = :gstNo)")
    List<Customer> findPossibleDuplicates(@Param("companyName") String companyName, @Param("gstNo") String gstNo);

    // Backs AccountManagerSuggestionService's load-balancing calculation, which
    // by definition needs the real count across every user, not just the
    // caller's own visible slice.
    long countByAccountManagerIdAndActiveTrue(UUID accountManagerId);
}
