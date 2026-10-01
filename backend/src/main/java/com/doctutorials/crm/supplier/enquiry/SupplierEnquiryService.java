package com.doctutorials.crm.supplier.enquiry;

import com.doctutorials.crm.audit.AuditAction;
import com.doctutorials.crm.audit.AuditService;
import com.doctutorials.crm.common.exception.BadRequestException;
import com.doctutorials.crm.common.exception.ResourceNotFoundException;
import com.doctutorials.crm.lead.Lead;
import com.doctutorials.crm.lead.LeadService;
import com.doctutorials.crm.mail.MailService;
import com.doctutorials.crm.security.CurrentUserService;
import com.doctutorials.crm.supplier.Supplier;
import com.doctutorials.crm.supplier.SupplierService;
import com.doctutorials.crm.supplier.enquiry.dto.SupplierEnquiryReplyRequest;
import com.doctutorials.crm.supplier.enquiry.dto.SupplierEnquiryRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * PRD 12.2's full chain in one service: Customer Requirement (the Lead) ->
 * Supplier Enquiry -> Supplier Contact (email) -> Supplier Reply -> Supplier
 * Price -> chosen as the Customer Quotation's reference.
 *
 * Visibility follows TaskService's lead-delegation pattern exactly (see its
 * class/getById comments): an enquiry is visible exactly when its lead is,
 * so every read/write here goes through leadService.getById(leadId) rather
 * than a separate access-control class.
 */
@Service
@RequiredArgsConstructor
public class SupplierEnquiryService {

    private final SupplierEnquiryRepository enquiryRepository;
    private final SupplierEnquiryReplyRepository replyRepository;
    private final LeadService leadService;
    private final SupplierService supplierService;
    private final MailService mailService;
    private final AuditService auditService;
    private final CurrentUserService currentUserService;

    @Transactional
    public SupplierEnquiry create(SupplierEnquiryRequest req) {
        Lead lead = leadService.getById(req.leadId()); // 404s if the caller can't see this lead

        SupplierEnquiry enquiry = new SupplierEnquiry();
        enquiry.setLead(lead);
        enquiry.setItemDescription(req.itemDescription().trim());
        enquiry.setQuantity(req.quantity());
        enquiry.setNotes(req.notes());
        enquiry.setCreatedBy(currentUserService.get());

        SupplierEnquiry saved = enquiryRepository.save(enquiry);
        saved.setEnquiryCode(generateCode());
        saved = enquiryRepository.save(saved);

        for (UUID supplierId : req.supplierIds()) {
            Supplier supplier = supplierService.getById(supplierId); // 404s on an unknown/archived supplier

            SupplierEnquiryReply reply = new SupplierEnquiryReply();
            reply.setEnquiry(saved);
            reply.setSupplier(supplier);

            if (supplier.getContactEmail() != null && !supplier.getContactEmail().isBlank()) {
                mailService.sendSupplierEnquiryEmail(
                        supplier.getContactEmail(), supplier.getContactName(), saved.getEnquiryCode(),
                        saved.getItemDescription(), saved.getQuantity(), saved.getNotes());
                reply.setEmailSentAt(Instant.now());
            }
            replyRepository.save(reply);
            // saved.getReplies() is a plain in-memory list on a brand-new entity, not a
            // lazily-loaded Hibernate collection - persisting `reply` above doesn't make
            // it appear here on its own, so it's added explicitly. Matters because the
            // response DTO built from this same `saved` reference needs every reply
            // present, not just whatever a fresh query would eventually show.
            saved.getReplies().add(reply);
        }

        auditService.log("SupplierEnquiry", saved.getId(), AuditAction.CREATE,
                "Enquiry " + saved.getEnquiryCode() + " sent to " + req.supplierIds().size()
                        + " supplier(s) for: " + saved.getItemDescription());
        return saved;
    }

    @Transactional
    public SupplierEnquiry recordReply(UUID enquiryId, UUID replyId, SupplierEnquiryReplyRequest req) {
        SupplierEnquiry enquiry = getById(enquiryId);
        SupplierEnquiryReply reply = findReply(enquiry, replyId);

        reply.setStatus(req.status() != null ? req.status() : SupplierReplyStatus.REPLIED);
        reply.setPrice(req.price());
        if (req.currency() != null && !req.currency().isBlank()) reply.setCurrency(req.currency());
        reply.setValidUntil(req.validUntil());
        reply.setDeliveryInfo(req.deliveryInfo());
        reply.setSupplierReference(req.supplierReference());
        reply.setTerms(req.terms());
        reply.setRemarks(req.remarks());
        reply.setRepliedAt(Instant.now());
        replyRepository.save(reply);

        auditService.log("SupplierEnquiry", enquiryId, AuditAction.UPDATE,
                reply.getSupplier().getCompanyName() + " " + reply.getStatus().name().toLowerCase()
                        + (reply.getPrice() != null ? " at " + reply.getCurrency() + " " + reply.getPrice() : ""));
        return getById(enquiryId);
    }

    /**
     * Marks one reply as the price reference for this enquiry's quotation
     * (PRD 12.3). Unsets any previously chosen reply first, so "exactly one
     * chosen, or none" holds without a DB constraint - see SupplierEnquiryReply's
     * class comment. Closes the enquiry too: once a price is chosen there's
     * nothing left to wait on. Re-calling this with a different replyId later
     * is how you change your mind, which also silently reopens nothing - the
     * enquiry stays CLOSED, which is fine since a chosen price still exists.
     */
    @Transactional
    public SupplierEnquiry chooseReply(UUID enquiryId, UUID replyId) {
        SupplierEnquiry enquiry = getById(enquiryId);
        SupplierEnquiryReply target = findReply(enquiry, replyId);

        if (target.getStatus() != SupplierReplyStatus.REPLIED) {
            throw new BadRequestException("Can't choose a reply that hasn't come in yet");
        }

        for (SupplierEnquiryReply r : enquiry.getReplies()) {
            if (r.isChosen() && !r.getId().equals(replyId)) {
                r.setChosen(false);
                replyRepository.save(r);
            }
        }
        target.setChosen(true);
        replyRepository.save(target);

        enquiry.setStatus(SupplierEnquiryStatus.CLOSED);
        enquiryRepository.save(enquiry);

        auditService.log("SupplierEnquiry", enquiryId, AuditAction.STATUS_CHANGE,
                target.getSupplier().getCompanyName() + " chosen as price reference ("
                        + target.getCurrency() + " " + target.getPrice() + ")");
        return getById(enquiryId);
    }

    public SupplierEnquiry getById(UUID id) {
        SupplierEnquiry enquiry = enquiryRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("SupplierEnquiry", id));
        leadService.getById(enquiry.getLead().getId()); // re-checks visibility, same pattern as TaskService.getById
        return enquiry;
    }

    public Page<SupplierEnquiry> listByLead(UUID leadId, Pageable pageable) {
        leadService.getById(leadId); // 404s if the lead itself isn't visible
        return enquiryRepository.findByLeadId(leadId, pageable);
    }

    /** Supplier-side "previous transactions" history (PRD 12.1). Supplier master
     *  data is org-wide visible, so no lead-visibility check applies here. */
    public Page<SupplierEnquiryReply> listBySupplier(UUID supplierId, Pageable pageable) {
        supplierService.getById(supplierId); // 404s on an unknown/archived supplier
        return replyRepository.findBySupplierIdOrderByCreatedAtDesc(supplierId, pageable);
    }

    private SupplierEnquiryReply findReply(SupplierEnquiry enquiry, UUID replyId) {
        return enquiry.getReplies().stream()
                .filter(r -> r.getId().equals(replyId))
                .findFirst()
                .orElseThrow(() -> ResourceNotFoundException.of("SupplierEnquiryReply", replyId));
    }

    /** ENQ-0001 style code — same reasoning as Supplier.generateCode: cosmetic only. */
    private String generateCode() {
        long ordinal = enquiryRepository.count();
        return String.format("ENQ-%04d", ordinal);
    }
}
