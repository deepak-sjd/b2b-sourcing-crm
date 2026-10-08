package com.doctutorials.crm.customer;

import com.doctutorials.crm.audit.AuditAction;
import com.doctutorials.crm.audit.AuditService;
import com.doctutorials.crm.common.exception.BadRequestException;
import com.doctutorials.crm.lead.Lead;
import com.doctutorials.crm.lead.LeadRepository;
import com.doctutorials.crm.lead.LeadService;
import com.doctutorials.crm.lead.LeadStage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Bridges the two independent aggregates (Lead, Customer) at the one moment
 * the PRD's workflow actually connects them: a lead marked WON becomes a
 * real customer. Lives in the "customer" package (not "lead") so the
 * dependency runs customer -> lead, never the other way - lead has no idea
 * Customer exists, same as everywhere else in this codebase.
 *
 * What this does NOT do: delete, archive, or otherwise change the Lead
 * beyond recording where it went. "Valuable records are never deleted" per
 * the PRD - the Lead stays exactly as it is, as the historical record of how
 * this customer originated.
 */
@Service
@RequiredArgsConstructor
public class LeadConversionService {

    private final LeadService leadService;
    private final LeadRepository leadRepository;
    private final CustomerRepository customerRepository;
    private final CustomerInsightService insightService;
    private final AuditService auditService;

    @Transactional
    public Customer convert(UUID leadId) {
        // Scoped - throws 404 if the caller can't see this lead, same as every
        // other lead operation.
        Lead lead = leadService.getById(leadId);

        // Idempotent: a second click (or a retried request) returns the customer
        // already created from this lead instead of creating a duplicate.
        Customer existing = customerRepository.findByConvertedFromLeadId(leadId).orElse(null);
        if (existing != null) {
            return existing;
        }

        if (lead.getStage() != LeadStage.WON) {
            throw new BadRequestException(
                    "Only a WON lead can be converted to a customer (this lead is " + lead.getStage() + ")");
        }

        Customer customer = new Customer();

        // Lead has no separate "company name" field (fullName is a single name,
        // individual-shaped) - it's the best available identity for the new
        // Customer record, but this is a real data gap, not a confident mapping.
        // Flagged in the audit note below so nobody mistakes it for a real company
        // name pulled from a proper source.
        customer.setCompanyName(lead.getFullName());
        customer.setApprovalStatus(ApprovalStatus.PENDING); // same default as any new customer
        customer.setAccountManager(lead.getOwner());

        // Lead only captures a city, never a street address - there is nothing
        // truthful to put in billingAddress.line1 (NOT NULL). Using an explicit
        // "(pending)" marker rather than fabricating a fake street address, so
        // whoever opens this record can immediately see it needs completing -
        // same honesty-over-guessing approach as the rest of this codebase
        // (e.g. NatureOfBusiness's "stub list, confirm before go-live" comment).
        Address billing = customer.getBillingAddress();
        billing.setLine1(lead.getCity() != null ? lead.getCity() + " (exact address pending)" : "(address pending)");
        billing.setCity(lead.getCity());
        billing.setPhone(lead.getPhone());
        billing.setEmail(lead.getEmail());
        customer.setShippingSameAsBilling(true);

        CustomerContact primaryContact = new CustomerContact();
        primaryContact.setCustomer(customer);
        primaryContact.setContactType("Primary");
        primaryContact.setName(lead.getFullName());
        primaryContact.setEmail(lead.getEmail());
        primaryContact.setContactNo(lead.getPhone());
        customer.getContacts().add(primaryContact);

        customer.setConvertedFromLeadId(lead.getId());
        customer.setAiHealthSummary(insightService.generateHealthSummary(customer));
        customer.setAiHealthGeneratedAt(Instant.now());

        Customer saved = customerRepository.save(customer);

        lead.setConvertedToCustomerId(saved.getId());
        leadRepository.save(lead);

        // Lead.interest and Lead.budget have nowhere to live on Customer today -
        // there's no Requirement record yet (PRD module 8, not built). Rather than
        // silently drop them, they're preserved here in the permanent audit trail
        // until a real Requirement entity exists to hold them properly.
        String carriedOverNote = "Converted from lead '%s'. Interest: %s. Budget: %s."
                .formatted(lead.getFullName(),
                        lead.getInterest() != null ? lead.getInterest() : "(none)",
                        lead.getBudget() != null ? lead.getBudget() : "(none)");
        auditService.log("Customer", saved.getId(), AuditAction.CREATE, carriedOverNote);
        auditService.log("Lead", lead.getId(), AuditAction.UPDATE,
                "Converted to customer " + saved.getCompanyName() + " (" + saved.getId() + ")");

        return saved;
    }
}
