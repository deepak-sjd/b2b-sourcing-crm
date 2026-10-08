package com.doctutorials.crm.quotation;

import com.doctutorials.crm.audit.AuditAction;
import com.doctutorials.crm.audit.AuditService;
import com.doctutorials.crm.common.exception.BadRequestException;
import com.doctutorials.crm.common.exception.ResourceNotFoundException;
import com.doctutorials.crm.customer.Customer;
import com.doctutorials.crm.customer.CustomerContact;
import com.doctutorials.crm.customer.CustomerRepository;
import com.doctutorials.crm.customer.CustomerService;
import com.doctutorials.crm.lead.Lead;
import com.doctutorials.crm.lead.LeadService;
import com.doctutorials.crm.lead.LeadStage;
import com.doctutorials.crm.quotation.dto.QuotationLineItemRequest;
import com.doctutorials.crm.quotation.dto.QuotationPrefillResponse;
import com.doctutorials.crm.quotation.dto.QuotationRequest;
import com.doctutorials.crm.requirement.Requirement;
import com.doctutorials.crm.requirement.RequirementRepository;
import com.doctutorials.crm.requirement.RequirementStatus;
import com.doctutorials.crm.security.CurrentUserService;
import com.doctutorials.crm.supplier.Supplier;
import com.doctutorials.crm.supplier.SupplierRepository;
import com.doctutorials.crm.supplier.SupplierStatus;
import com.doctutorials.crm.supplier.enquiry.SupplierEnquiryReply;
import com.doctutorials.crm.supplier.enquiry.SupplierEnquiryReplyRepository;
import com.doctutorials.crm.supplier.enquiry.SupplierReplyStatus;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Year;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuotationService {

    /** Mirrors the chain in the PRD/spec exactly: DRAFT->UNDER_REVIEW->SENT->VIEWED->
     *  NEGOTIATION->ACCEPTED|REJECTED, with SENT/VIEWED/NEGOTIATION able to lapse to
     *  EXPIRED (manually here; QuotationExpiryJob does the automatic version), and
     *  DRAFT/UNDER_REVIEW able to step back to DRAFT. ACCEPTED/REJECTED/EXPIRED are
     *  terminal - reopening one isn't a "status change", it's a new quotation. */
    private static final Map<QuotationStatus, Set<QuotationStatus>> ALLOWED_TRANSITIONS = Map.of(
            QuotationStatus.DRAFT, Set.of(QuotationStatus.UNDER_REVIEW),
            QuotationStatus.UNDER_REVIEW, Set.of(QuotationStatus.DRAFT, QuotationStatus.SENT),
            QuotationStatus.SENT, Set.of(QuotationStatus.VIEWED, QuotationStatus.NEGOTIATION,
                    QuotationStatus.ACCEPTED, QuotationStatus.REJECTED, QuotationStatus.EXPIRED),
            QuotationStatus.VIEWED, Set.of(QuotationStatus.NEGOTIATION,
                    QuotationStatus.ACCEPTED, QuotationStatus.REJECTED, QuotationStatus.EXPIRED),
            QuotationStatus.NEGOTIATION, Set.of(
                    QuotationStatus.ACCEPTED, QuotationStatus.REJECTED, QuotationStatus.EXPIRED),
            QuotationStatus.ACCEPTED, Set.of(),
            QuotationStatus.REJECTED, Set.of(),
            QuotationStatus.EXPIRED, Set.of());

    /** Content (line items, prices, terms) is only editable before a quotation has
     *  gone out - once SENT it's a record of what the customer was actually shown. */
    private static final Set<QuotationStatus> EDITABLE = Set.of(QuotationStatus.DRAFT, QuotationStatus.UNDER_REVIEW);

    private static final Set<QuotationStatus> EXPIRABLE =
            Set.of(QuotationStatus.SENT, QuotationStatus.VIEWED, QuotationStatus.NEGOTIATION);

    private final QuotationRepository quotationRepository;
    private final QuotationAccessService quotationAccessService;
    private final CustomerService customerService;
    private final CustomerRepository customerRepository;
    private final LeadService leadService;
    private final RequirementRepository requirementRepository;
    private final SupplierRepository supplierRepository;
    private final SupplierEnquiryReplyRepository supplierEnquiryReplyRepository;
    private final AuditService auditService;
    private final CurrentUserService currentUserService;

    @Transactional
    public Quotation create(QuotationRequest req) {
        Customer customer = req.customerId() != null ? customerService.getById(req.customerId()) : null;
        Lead lead = req.leadId() != null ? leadService.getById(req.leadId()) : null;
        if (customer == null && lead == null) {
            throw new BadRequestException("A quotation needs either a customer or a lead.");
        }

        Requirement requirement = null;
        if (req.requirementId() != null) {
            requirement = requirementRepository.findById(req.requirementId())
                    .orElseThrow(() -> ResourceNotFoundException.of("Requirement", req.requirementId()));
            leadService.getById(requirement.getLead().getId()); // visibility check
            if (lead == null) {
                lead = requirement.getLead(); // infer the lead when only requirementId was given
            } else if (!lead.getId().equals(requirement.getLead().getId())) {
                throw new BadRequestException("That requirement doesn't belong to the selected lead.");
            }
        }

        Quotation quotation = new Quotation();
        quotation.setCustomer(customer);
        quotation.setLead(lead);
        quotation.setRequirement(requirement);
        quotation.setPreparedBy(currentUserService.get());
        applyRequest(quotation, req);

        Quotation saved = quotationRepository.save(quotation);
        saved.setQuotationNumber(generateNumber());
        saved = quotationRepository.save(saved);

        auditService.log("Quotation", saved.getId(), AuditAction.CREATE,
                "Quotation " + saved.getQuotationNumber() + " created for "
                        + (customer != null ? customer.getCompanyName() : lead.getFullName()));
        return saved;
    }

    @Transactional
    public Quotation update(UUID id, QuotationRequest req) {
        Quotation quotation = getById(id);
        if (!EDITABLE.contains(quotation.getStatus())) {
            throw new BadRequestException(
                    "This quotation is " + quotation.getStatus() + " and can't be edited - "
                            + "move it back to DRAFT first if this needs to be reopened.");
        }
        // Customer/lead/requirement are fixed at creation time - a quotation doesn't move targets.
        applyRequest(quotation, req);

        Quotation saved = quotationRepository.save(quotation);
        auditService.log("Quotation", saved.getId(), AuditAction.UPDATE, "Quotation details updated");
        return saved;
    }

    @Transactional
    public Quotation changeStatus(UUID id, QuotationStatus newStatus) {
        Quotation quotation = getById(id);
        QuotationStatus old = quotation.getStatus();
        if (old == newStatus) {
            return quotation;
        }
        if (!ALLOWED_TRANSITIONS.getOrDefault(old, Set.of()).contains(newStatus)) {
            throw new BadRequestException("Cannot move a quotation from " + old + " to " + newStatus + ".");
        }
        quotation.setStatus(newStatus);

        // Sending a quotation is the signal that a deal has reached the Quotation stage
        // (PRD §10) - never downgrade a stage the lead has already moved past, and never
        // touch WON ourselves (see QuotationResponse.canMarkLeadWon's comment).
        if (newStatus == QuotationStatus.SENT && quotation.getLead() != null) {
            Lead lead = quotation.getLead();
            if (lead.getStage().ordinal() < LeadStage.QUOTATION.ordinal()) {
                leadService.changeStage(lead.getId(), LeadStage.QUOTATION);
            }
            if (quotation.getRequirement() != null) {
                Requirement requirement = quotation.getRequirement();
                requirement.setStatus(RequirementStatus.QUOTED);
                requirementRepository.save(requirement);
            }
        }

        Quotation saved = quotationRepository.save(quotation);
        auditService.log("Quotation", id, AuditAction.STATUS_CHANGE,
                "Status changed from " + old + " to " + newStatus);
        return saved;
    }

    /**
     * A real delete, not an archive - but only while the quotation is still a
     * DRAFT. Anything that's ever been sent is a record of what a customer was
     * actually offered; per the PRD's "no valuable record is ever deleted"
     * principle, the way to retire it is a status change (e.g. REJECTED or
     * EXPIRED), not removal.
     */
    @Transactional
    public void delete(UUID id) {
        Quotation quotation = getById(id);
        if (quotation.getStatus() != QuotationStatus.DRAFT) {
            throw new BadRequestException(
                    "Only a DRAFT quotation can be deleted - change its status instead to keep the record.");
        }
        quotationRepository.delete(quotation);
        auditService.log("Quotation", id, AuditAction.DELETE, "Draft quotation deleted");
    }

    public Quotation getById(UUID id) {
        Quotation quotation = quotationRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Quotation", id));
        quotationAccessService.assertVisible(quotation);
        return quotation;
    }

    public Page<Quotation> list(UUID customerId, QuotationStatus status, String search, Pageable pageable) {
        Specification<Quotation> spec = Specification.where(quotationAccessService.quotationVisibility());

        if (customerId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("customer").get("id"), customerId));
        }
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (search != null && !search.isBlank()) {
            String like = "%" + search.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> {
                Join<Quotation, Customer> customerJoin = root.join("customer", JoinType.LEFT);
                Join<Quotation, Lead> leadJoin = root.join("lead", JoinType.LEFT);
                return cb.or(
                        cb.like(cb.lower(root.get("quotationNumber")), like),
                        cb.like(cb.lower(customerJoin.get("companyName")), like),
                        cb.like(cb.lower(leadJoin.get("fullName")), like));
            });
        }
        return quotationRepository.findAll(spec, pageable);
    }

    /** Backs the "Quotation history" tab on CustomerDetailPage (PRD §6). */
    public Page<Quotation> byCustomer(UUID customerId, Pageable pageable) {
        customerService.getById(customerId); // enforces visibility before exposing its quotations
        return quotationRepository.findByCustomerIdOrderByCreatedAtDesc(customerId, pageable);
    }

    /** Backs LeadQuotationsTab - quotations prepared against the lead directly,
     *  i.e. before (or instead of) it ever became a customer. */
    public Page<Quotation> byLead(UUID leadId, Pageable pageable) {
        leadService.getById(leadId); // enforces visibility before exposing its quotations
        return quotationRepository.findByLeadIdOrderByCreatedAtDesc(leadId, pageable);
    }

    /** Backs "Start from requirement": one prefilled, price-trail-locked line
     *  from whichever supplier reply was chosen for this requirement. */
    @Transactional(readOnly = true)
    public QuotationPrefillResponse prefill(UUID requirementId) {
        Requirement requirement = requirementRepository.findById(requirementId)
                .orElseThrow(() -> ResourceNotFoundException.of("Requirement", requirementId));
        Lead lead = leadService.getById(requirement.getLead().getId()); // visibility check

        SupplierEnquiryReply reply = supplierEnquiryReplyRepository
                .findFirstByEnquiry_RequirementIdAndChosenTrueOrderByCreatedAtDesc(requirementId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Choose a supplier price first for this requirement, then try again."));

        UUID customerId = customerRepository.findByConvertedFromLeadId(lead.getId())
                .map(Customer::getId).orElse(null);

        return new QuotationPrefillResponse(
                lead.getId(), lead.getFullName(), customerId,
                requirement.getProductName(), requirement.getQuantity(), requirement.getUnit(),
                reply.getCurrency(),
                reply.getSupplier().getId(), reply.getSupplier().getCompanyName(), reply.getPrice(),
                reply.getId(), reply.getSupplierReference(), reply.getEnquiry().getEnquiryCode(),
                reply.getValidUntil());
    }

    public QuotationSummary summary() {
        var all = quotationRepository.findAll(Specification.where(quotationAccessService.quotationVisibility()));

        long draft = all.stream().filter(q -> q.getStatus() == QuotationStatus.DRAFT).count();
        long pending = all.stream().filter(q -> Set.of(QuotationStatus.UNDER_REVIEW, QuotationStatus.SENT,
                QuotationStatus.VIEWED, QuotationStatus.NEGOTIATION).contains(q.getStatus())).count();
        long accepted = all.stream().filter(q -> q.getStatus() == QuotationStatus.ACCEPTED).count();
        long rejectedOrExpired = all.stream().filter(q -> q.getStatus() == QuotationStatus.REJECTED
                || q.getStatus() == QuotationStatus.EXPIRED).count();
        BigDecimal wonValue = all.stream().filter(q -> q.getStatus() == QuotationStatus.ACCEPTED)
                .map(Quotation::getGrandTotal).reduce(BigDecimal.ZERO, BigDecimal::add);

        return new QuotationSummary(all.size(), draft, pending, accepted, rejectedOrExpired, wonValue);
    }

    public record QuotationSummary(long total, long draft, long pending, long accepted, long rejectedOrExpired,
            BigDecimal acceptedValue) {}

    /** Called only by QuotationExpiryJob - org-wide, not scoped to any caller's
     *  visibility, since there's no caller; it's the nightly job. */
    @Transactional
    public int expireOverdueQuotationsForSystemJob() {
        List<Quotation> overdue = quotationRepository.findByValidUntilBeforeAndStatusIn(
                LocalDate.now(), List.copyOf(EXPIRABLE));
        for (Quotation q : overdue) {
            q.setStatus(QuotationStatus.EXPIRED);
            auditService.log("Quotation", q.getId(), AuditAction.STATUS_CHANGE,
                    "Automatically expired - valid until " + q.getValidUntil() + " has passed");
        }
        if (!overdue.isEmpty()) {
            quotationRepository.saveAll(overdue);
        }
        return overdue.size();
    }

    // --- internals ---

    private void applyRequest(Quotation quotation, QuotationRequest req) {
        if (quotation.getCustomer() != null && req.customerContactId() != null) {
            CustomerContact contact = quotation.getCustomer().getContacts().stream()
                    .filter(c -> c.getId().equals(req.customerContactId()))
                    .findFirst()
                    .orElseThrow(() -> ResourceNotFoundException.of("CustomerContact", req.customerContactId()));
            quotation.setCustomerContact(contact);
        } else {
            quotation.setCustomerContact(null);
        }

        quotation.setQuotationDate(req.quotationDate() != null ? req.quotationDate() : LocalDate.now());
        quotation.setValidUntil(req.validUntil());
        quotation.setCurrency(req.currency() == null || req.currency().isBlank() ? "INR" : req.currency());
        quotation.setTaxPercent(req.taxPercent());
        quotation.setTermsAndConditions(req.termsAndConditions());
        quotation.setRemarks(req.remarks());

        syncLineItems(quotation, req.lineItems());
        recomputeTotals(quotation);
    }

    /** Same id-based upsert shape as CustomerService.syncContacts - a row with an id is
     *  edited in place, one without is new, and any existing row not present in the
     *  request is dropped (orphanRemoval turns that into a real DELETE on save). */
    private void syncLineItems(Quotation quotation, java.util.List<QuotationLineItemRequest> requested) {
        Set<UUID> requestedIds = requested.stream()
                .map(QuotationLineItemRequest::id)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        quotation.getLineItems().removeIf(existing -> !requestedIds.contains(existing.getId()));

        int order = 0;
        for (QuotationLineItemRequest lr : requested) {
            QuotationLineItem item;
            if (lr.id() != null) {
                item = quotation.getLineItems().stream()
                        .filter(i -> i.getId().equals(lr.id()))
                        .findFirst()
                        .orElseThrow(() -> ResourceNotFoundException.of("QuotationLineItem", lr.id()));
            } else {
                item = new QuotationLineItem();
                quotation.addLineItem(item);
            }

            item.setSortOrder(order++);
            item.setProductName(lr.productName().trim());
            item.setDescription(lr.description());
            item.setUnit(lr.unit() == null || lr.unit().isBlank() ? "Nos" : lr.unit());
            item.setQuantity(lr.quantity());
            item.setSellingPrice(lr.sellingPrice());
            item.setRemarks(lr.remarks());

            applyLineItemPricing(item, lr);
        }
    }

    /** Either locks the line to a chosen enquiry reply's price (server-trusted,
     *  client's supplierId/supplierPrice ignored), or takes the hand-typed supplier
     *  + price from the request - never both. */
    private void applyLineItemPricing(QuotationLineItem item, QuotationLineItemRequest lr) {
        if (lr.enquiryReplyId() != null) {
            SupplierEnquiryReply reply = supplierEnquiryReplyRepository.findById(lr.enquiryReplyId())
                    .orElseThrow(() -> ResourceNotFoundException.of("SupplierEnquiryReply", lr.enquiryReplyId()));
            if (reply.getStatus() != SupplierReplyStatus.REPLIED || reply.getPrice() == null) {
                throw new BadRequestException("That supplier reply doesn't have a price recorded yet.");
            }
            if (reply.getValidUntil() != null && reply.getValidUntil().isBefore(LocalDate.now())) {
                throw new BadRequestException("That supplier price has expired - get a fresh quote before using it.");
            }
            if (reply.getSupplier().getStatus() == SupplierStatus.BLACKLISTED) {
                throw new BadRequestException("That supplier is blacklisted and can't be used on a quotation.");
            }
            item.setEnquiryReply(reply);
            item.setSupplier(reply.getSupplier());
            item.setSupplierPrice(reply.getPrice());
            return;
        }

        item.setEnquiryReply(null);
        item.setSupplierPrice(lr.supplierPrice());
        if (lr.supplierId() != null) {
            Supplier supplier = supplierRepository.findById(lr.supplierId())
                    .orElseThrow(() -> ResourceNotFoundException.of("Supplier", lr.supplierId()));
            item.setSupplier(supplier);
        } else {
            item.setSupplier(null);
        }
    }

    /** Recomputes every line's total/margin plus the quotation's subtotal/tax/grand total.
     *  Runs on every create/update so stored totals can never drift from their line items. */
    private void recomputeTotals(Quotation quotation) {
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal totalMargin = BigDecimal.ZERO;

        for (QuotationLineItem item : quotation.getLineItems()) {
            BigDecimal lineTotal = item.getSellingPrice().multiply(item.getQuantity())
                    .setScale(2, RoundingMode.HALF_UP);
            item.setLineTotal(lineTotal);
            subtotal = subtotal.add(lineTotal);

            if (item.getSupplierPrice() != null) {
                BigDecimal margin = item.getSellingPrice().subtract(item.getSupplierPrice())
                        .multiply(item.getQuantity()).setScale(2, RoundingMode.HALF_UP);
                item.setMarginAmount(margin);
                item.setMarginPercent(lineTotal.compareTo(BigDecimal.ZERO) == 0
                        ? BigDecimal.ZERO
                        : margin.multiply(BigDecimal.valueOf(100)).divide(lineTotal, 2, RoundingMode.HALF_UP));
                totalMargin = totalMargin.add(margin);
            } else {
                item.setMarginAmount(null);
                item.setMarginPercent(null);
            }
        }

        BigDecimal taxAmount = quotation.getTaxPercent() != null
                ? subtotal.multiply(quotation.getTaxPercent()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        quotation.setSubtotal(subtotal);
        quotation.setTaxAmount(taxAmount);
        quotation.setGrandTotal(subtotal.add(taxAmount));
        quotation.setTotalMarginAmount(totalMargin);
    }

    /** "QT-2026-0001" - year plus a running count, same best-effort-unique approach
     *  as SupplierService.generateCode (see its comment: cosmetic, not a lookup key,
     *  DB unique constraint is the real backstop under rare concurrent creates). */
    private String generateNumber() {
        long ordinal = quotationRepository.count();
        return String.format("QT-%d-%04d", Year.now().getValue(), ordinal);
    }
}
