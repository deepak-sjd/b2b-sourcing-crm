package com.doctutorials.crm.quotation;

import com.doctutorials.crm.audit.AuditAction;
import com.doctutorials.crm.audit.AuditService;
import com.doctutorials.crm.common.exception.BadRequestException;
import com.doctutorials.crm.common.exception.ResourceNotFoundException;
import com.doctutorials.crm.customer.Customer;
import com.doctutorials.crm.customer.CustomerContact;
import com.doctutorials.crm.customer.CustomerService;
import com.doctutorials.crm.quotation.dto.QuotationLineItemRequest;
import com.doctutorials.crm.quotation.dto.QuotationRequest;
import com.doctutorials.crm.security.CurrentUserService;
import com.doctutorials.crm.supplier.Supplier;
import com.doctutorials.crm.supplier.SupplierRepository;
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
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuotationService {

    private static final Set<QuotationStatus> LOCKED_FOR_EDITING =
            Set.of(QuotationStatus.ACCEPTED, QuotationStatus.REJECTED, QuotationStatus.EXPIRED);

    private final QuotationRepository quotationRepository;
    private final QuotationAccessService quotationAccessService;
    private final CustomerService customerService;
    private final SupplierRepository supplierRepository;
    private final AuditService auditService;
    private final CurrentUserService currentUserService;

    @Transactional
    public Quotation create(QuotationRequest req) {
        // customerService.getById both fetches the customer and enforces that the
        // current user is allowed to see it - a counselor can't quote for a
        // customer that isn't theirs.
        Customer customer = customerService.getById(req.customerId());

        Quotation quotation = new Quotation();
        quotation.setCustomer(customer);
        quotation.setPreparedBy(currentUserService.get());
        applyRequest(quotation, req);

        Quotation saved = quotationRepository.save(quotation);
        saved.setQuotationNumber(generateNumber());
        saved = quotationRepository.save(saved);

        auditService.log("Quotation", saved.getId(), AuditAction.CREATE,
                "Quotation " + saved.getQuotationNumber() + " created for " + customer.getCompanyName());
        return saved;
    }

    @Transactional
    public Quotation update(UUID id, QuotationRequest req) {
        Quotation quotation = getById(id);
        if (LOCKED_FOR_EDITING.contains(quotation.getStatus())) {
            throw new BadRequestException(
                    "This quotation is " + quotation.getStatus() + " and can't be edited - "
                            + "change its status first if this needs to be reopened.");
        }
        // Customer is fixed at creation time - a quotation doesn't move between customers.
        applyRequest(quotation, req);

        Quotation saved = quotationRepository.save(quotation);
        auditService.log("Quotation", saved.getId(), AuditAction.UPDATE, "Quotation details updated");
        return saved;
    }

    @Transactional
    public Quotation changeStatus(UUID id, QuotationStatus newStatus) {
        Quotation quotation = getById(id);
        QuotationStatus old = quotation.getStatus();
        quotation.setStatus(newStatus);
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
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("quotationNumber")), like),
                    cb.like(cb.lower(root.get("customer").get("companyName")), like)));
        }
        return quotationRepository.findAll(spec, pageable);
    }

    /** Backs the "Quotation history" tab on CustomerDetailPage (PRD §6). */
    public Page<Quotation> byCustomer(UUID customerId, Pageable pageable) {
        customerService.getById(customerId); // enforces visibility before exposing its quotations
        return quotationRepository.findByCustomerIdOrderByCreatedAtDesc(customerId, pageable);
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

    // --- internals ---

    private void applyRequest(Quotation quotation, QuotationRequest req) {
        if (req.customerContactId() != null) {
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
            item.setSupplierPrice(lr.supplierPrice());
            item.setSellingPrice(lr.sellingPrice());
            item.setRemarks(lr.remarks());

            if (lr.supplierId() != null) {
                Supplier supplier = supplierRepository.findById(lr.supplierId())
                        .orElseThrow(() -> ResourceNotFoundException.of("Supplier", lr.supplierId()));
                item.setSupplier(supplier);
            } else {
                item.setSupplier(null);
            }
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
