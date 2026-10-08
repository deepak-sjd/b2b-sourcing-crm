package com.doctutorials.crm.lead;

import com.doctutorials.crm.audit.AuditAction;
import com.doctutorials.crm.audit.AuditService;
import com.doctutorials.crm.common.exception.BadRequestException;
import com.doctutorials.crm.customer.ApprovalStatus;
import com.doctutorials.crm.customer.Customer;
import com.doctutorials.crm.customer.CustomerContact;
import com.doctutorials.crm.customer.CustomerRepository;
import com.doctutorials.crm.customer.CustomerService;
import com.doctutorials.crm.customer.dto.AddressDto;
import com.doctutorials.crm.customer.dto.CustomerContactRequest;
import com.doctutorials.crm.customer.dto.CustomerRequest;
import com.doctutorials.crm.quotation.Quotation;
import com.doctutorials.crm.quotation.QuotationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * PRD §14.1: once a deal is won (or at least qualified), it becomes a real B2B
 * customer. Lives next to LeadService rather than inside it (and the latter
 * is explicitly left unedited) because "turn this lead into a customer" is a
 * one-shot cross-module action, not part of a lead's own CRUD lifecycle.
 *
 * Deliberately NOT automatic on reaching WON - converting creates a real
 * commercial record (billing address, account manager assignment, approval
 * workflow) that deserves a human decision about timing, not a side effect
 * of a stage change.
 */
@Service
@RequiredArgsConstructor
public class LeadConversionService {

    private final LeadService leadService;
    private final CustomerService customerService;
    private final CustomerRepository customerRepository;
    private final QuotationRepository quotationRepository;
    private final AuditService auditService;

    /** Null if this lead hasn't been converted yet - lets the UI show "Convert"
     *  vs. "View customer" without a 404 round-trip. */
    public Optional<Customer> findConvertedCustomer(UUID leadId) {
        leadService.getById(leadId); // visibility check
        return customerRepository.findByConvertedFromLeadId(leadId);
    }

    @Transactional
    public Customer convert(UUID leadId) {
        Lead lead = leadService.getById(leadId);

        Optional<Customer> existing = customerRepository.findByConvertedFromLeadId(leadId);
        if (existing.isPresent()) {
            relinkQuotations(lead, existing.get()); // idempotent - also catches a quotation
            return existing.get();                   // added to the lead after the first conversion
        }

        if (!isConvertible(lead)) {
            throw new BadRequestException(
                    "A lead can only be converted once it's Qualified or further along the funnel.");
        }

        Customer customer = customerService.create(buildCustomerRequest(lead));
        customer.setConvertedFromLeadId(lead.getId());
        customer = customerRepository.save(customer);

        relinkQuotations(lead, customer);

        auditService.log("Customer", customer.getId(), AuditAction.CREATE,
                "Created by converting lead " + lead.getFullName());
        auditService.log("Lead", lead.getId(), AuditAction.UPDATE,
                "Converted to customer " + customer.getCompanyName());
        return customer;
    }

    /** WON always qualifies; anything else needs to have at least reached Qualified,
     *  and a Lost lead can't convert at all (ordinal order: QUALIFIED < ... < WON < LOST). */
    private boolean isConvertible(Lead lead) {
        if (lead.getStage() == LeadStage.WON) return true;
        if (lead.getStage() == LeadStage.LOST) return false;
        return lead.getStage().ordinal() >= LeadStage.QUALIFIED.ordinal();
    }

    /** Best-effort mapping from a lead's flat contact fields to a B2B customer
     *  record - this CRM's leads don't capture a separate company name, so
     *  fullName doubles as both until someone edits the converted customer. */
    private CustomerRequest buildCustomerRequest(Lead lead) {
        AddressDto billingAddress = new AddressDto(
                "To be updated", null, null, "India", null, lead.getCity(), null, lead.getPhone(), lead.getEmail(),
                null);

        List<CustomerContactRequest> contacts = List.of(
                new CustomerContactRequest(null, "Primary", lead.getFullName(), lead.getEmail(), lead.getPhone(), null));

        UUID accountManagerId = lead.getOwner() != null ? lead.getOwner().getId() : null;

        return new CustomerRequest(
                lead.getFullName(), null, null, accountManagerId,
                billingAddress, null, true,
                ApprovalStatus.PENDING, // billing address is a placeholder until someone fills it in - don't pretend it's approved
                contacts);
    }

    /** A quotation prepared while this was still just a lead should end up attached
     *  to the resulting customer too, without losing its lead link (PRD: no history lost). */
    private void relinkQuotations(Lead lead, Customer customer) {
        List<Quotation> quotations = quotationRepository.findByLeadId(lead.getId());
        for (Quotation q : quotations) {
            if (q.getCustomer() == null) {
                q.setCustomer(customer);
            }
        }
        if (!quotations.isEmpty()) {
            quotationRepository.saveAll(quotations);
        }
    }
}
