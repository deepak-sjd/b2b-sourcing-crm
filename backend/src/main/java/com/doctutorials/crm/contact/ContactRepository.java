package com.doctutorials.crm.contact;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface ContactRepository extends JpaRepository<Contact, UUID>, JpaSpecificationExecutor<Contact> {

    // Same rule as LeadRepository: every list/lookup for Contact goes through
    // ContactService, which ANDs in ContactAccessService's visibility
    // Specification — no derived list-shaped query methods here.
    //
    // The two finders below are a deliberate, narrow exception: duplicate
    // detection is a data-integrity check ("does this email/phone already
    // exist"), not a display of someone's contact list, so it isn't
    // visibility-scoped the same way. It only ever returns a single match
    // used to show "possible duplicate of <name>" — never a list.
    Optional<Contact> findFirstByEmailIgnoreCaseAndArchivedFalse(String email);

    Optional<Contact> findFirstByPhoneAndArchivedFalse(String phone);
}
