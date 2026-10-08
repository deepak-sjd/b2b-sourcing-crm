package com.doctutorials.crm.supplier.enquiry;

import com.doctutorials.crm.audit.AuditAction;
import com.doctutorials.crm.audit.AuditService;
import com.doctutorials.crm.common.exception.BadRequestException;
import com.doctutorials.crm.common.exception.ResourceNotFoundException;
import com.doctutorials.crm.lead.Lead;
import com.doctutorials.crm.lead.LeadService;
import com.doctutorials.crm.mail.MailService;
import com.doctutorials.crm.mail.SupplierEnquiryEmailRequest;
import com.doctutorials.crm.requirement.Requirement;
import com.doctutorials.crm.requirement.RequirementRepository;
import com.doctutorials.crm.requirement.RequirementService;
import com.doctutorials.crm.requirement.RequirementStatus;
import com.doctutorials.crm.security.CurrentUserService;
import com.doctutorials.crm.supplier.Supplier;
import com.doctutorials.crm.supplier.SupplierService;
import com.doctutorials.crm.supplier.SupplierStatus;
import com.doctutorials.crm.supplier.enquiry.dto.SupplierEnquiryReplyRequest;
import com.doctutorials.crm.supplier.enquiry.dto.SupplierEnquiryReplyResponse;
import com.doctutorials.crm.supplier.enquiry.dto.SupplierEnquiryRequest;
import com.doctutorials.crm.supplier.enquiry.dto.SupplierEnquiryResponse;
import com.doctutorials.crm.user.User;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * PRD 12.2's full chain: Requirement -> Supplier Enquiry -> Supplier Contact
 * (email) -> Supplier Reply -> Supplier Price -> chosen as the Customer
 * Quotation's reference.
 *
 * Every public method drives its DB work through {@link #transactionTemplate}
 * explicitly rather than Spring's {@code @Transactional} annotation. Two
 * reasons: (1) {@code create} needs the enquiry+reply rows committed in one
 * transaction, then emails sent with NO transaction open, then a second,
 * separate transaction to record the results - that can't be one
 * {@code @Transactional} method. (2) once some reads call other reads on
 * {@code this} (e.g. create() finishing with getById()), the
 * {@code @Transactional} annotation would silently stop applying on that
 * internal call (Spring AOP proxies don't intercept self-invocation) -
 * exactly the class of bug that produced the original
 * LazyInitializationException reports. Driving transactions imperatively
 * sidesteps that trap entirely: it behaves identically whether called from
 * the controller or from another method in this class.
 *
 * Visibility follows TaskService's lead-delegation pattern: an enquiry is
 * visible exactly when its requirement's lead is, via LeadService.getById.
 */
@Service
@RequiredArgsConstructor
public class SupplierEnquiryService {

    private final SupplierEnquiryRepository enquiryRepository;
    private final SupplierEnquiryReplyRepository replyRepository;
    private final RequirementRepository requirementRepository;
    private final RequirementService requirementService;
    private final LeadService leadService;
    private final SupplierService supplierService;
    private final MailService mailService;
    private final AuditService auditService;
    private final CurrentUserService currentUserService;
    private final TransactionTemplate transactionTemplate;
    private final EntityManager entityManager;

    public SupplierEnquiryResponse create(SupplierEnquiryRequest req) {
        User currentUser = currentUserService.get();
        List<UUID> supplierIds = req.supplierIds().stream().distinct().toList();

        CreatePhaseResult phase1 = transactionTemplate.execute(status -> doCreatePhase(req, supplierIds, currentUser));

        List<EmailOutcome> outcomes = new ArrayList<>();
        for (PendingEmail pending : phase1.pendingEmails()) {
            boolean sent = mailService.sendSupplierEnquiryEmail(new SupplierEnquiryEmailRequest(
                    pending.email(), pending.contactName(), phase1.enquiryCode(), phase1.itemDescription(),
                    phase1.quantity(), phase1.specs(), phase1.deliveryDate(), phase1.dueDate(),
                    req.message(), currentUser.getFullName(), currentUser.getEmail()));
            outcomes.add(new EmailOutcome(pending.replyId(), sent));
        }
        if (!outcomes.isEmpty()) {
            transactionTemplate.executeWithoutResult(status -> applyEmailOutcomes(outcomes));
        }

        return getById(phase1.enquiryId());
    }

    private CreatePhaseResult doCreatePhase(SupplierEnquiryRequest req, List<UUID> supplierIds, User currentUser) {
        Requirement requirement = requirementRepository.findById(req.requirementId())
                .orElseThrow(() -> ResourceNotFoundException.of("Requirement", req.requirementId()));
        // Visibility check + a fresh, fully-visible Lead instance - never trust requirement.getLead() alone for this.
        Lead lead = leadService.getById(requirement.getLead().getId());

        SupplierEnquiry enquiry = new SupplierEnquiry();
        enquiry.setRequirement(requirement);
        enquiry.setLead(lead);
        enquiry.setItemDescription(requirement.getProductName());
        enquiry.setQuantity(formatQuantity(requirement.getQuantity(), requirement.getUnit()));
        enquiry.setNotes(req.message());
        enquiry.setResponseDueDate(req.responseDueDate());
        enquiry.setCreatedBy(currentUser);

        SupplierEnquiry saved = enquiryRepository.save(enquiry);
        saved.setEnquiryCode(nextEnquiryCode());
        saved = enquiryRepository.save(saved);

        List<PendingEmail> pending = new ArrayList<>();
        for (UUID supplierId : supplierIds) {
            Supplier supplier = supplierService.getById(supplierId); // 404 on unknown/archived
            if (supplier.getStatus() == SupplierStatus.BLACKLISTED || supplier.getStatus() == SupplierStatus.ON_HOLD) {
                throw new BadRequestException(supplier.getCompanyName() + " is "
                        + supplier.getStatus().name().toLowerCase().replace('_', ' ') + " and can't be sent an enquiry");
            }

            SupplierEnquiryReply reply = new SupplierEnquiryReply();
            reply.setEnquiry(saved);
            reply.setRequirement(requirement);
            reply.setSupplier(supplier);
            SupplierEnquiryReply savedReply = replyRepository.save(reply);
            saved.getReplies().add(savedReply);

            if (supplier.getContactEmail() != null && !supplier.getContactEmail().isBlank()) {
                pending.add(new PendingEmail(savedReply.getId(), supplier.getContactEmail(), supplier.getContactName()));
            }
        }

        // Side effect (dev1 task #4): needs-supplier flag + OPEN -> SOURCING. Only
        // force the status jump from OPEN - don't yank a QUOTED/CLOSED/CANCELLED
        // requirement backwards just because someone raised a fresh enquiry on it.
        requirement.setNeedsSupplier(true);
        requirementRepository.save(requirement);
        if (requirement.getStatus() == RequirementStatus.OPEN) {
            requirementService.changeStatus(requirement.getId(), RequirementStatus.SOURCING);
        }

        auditService.log("SupplierEnquiry", saved.getId(), AuditAction.CREATE,
                "Enquiry " + saved.getEnquiryCode() + " sent to " + supplierIds.size()
                        + " supplier(s) for: " + saved.getItemDescription());

        return new CreatePhaseResult(saved.getId(), saved.getEnquiryCode(), saved.getItemDescription(),
                saved.getQuantity(), requirement.getAdditionalSpecifications(), requirement.getRequiredDeliveryDate(),
                req.responseDueDate(), pending);
    }

    /** Phase 3 of create(): record what actually happened with each email, in its own transaction. */
    private void applyEmailOutcomes(List<EmailOutcome> outcomes) {
        for (EmailOutcome outcome : outcomes) {
            replyRepository.findById(outcome.replyId()).ifPresent(reply -> {
                if (outcome.sent()) {
                    reply.setEmailSentAt(Instant.now());
                    reply.setEmailError(null);
                } else {
                    reply.setEmailError("Delivery failed - try resending or contact them directly");
                }
                replyRepository.save(reply);
            });
        }
    }

    public SupplierEnquiryReplyResponse resend(UUID enquiryId, UUID replyId) {
        User currentUser = currentUserService.get();

        ResendPrep prep = transactionTemplate.execute(status -> {
            SupplierEnquiry enquiry = loadEnquiry(enquiryId);
            SupplierEnquiryReply reply = findReply(enquiry, replyId);
            if (reply.getStatus() != SupplierReplyStatus.PENDING) {
                throw new BadRequestException("Can only resend to a supplier who hasn't replied yet");
            }
            Supplier supplier = reply.getSupplier();
            if (supplier.getContactEmail() == null || supplier.getContactEmail().isBlank()) {
                throw new BadRequestException(supplier.getCompanyName() + " has no email on file");
            }
            Requirement requirement = enquiry.getRequirement(); // null on enquiries raised before requirements were linked
            return new ResendPrep(reply.getId(), supplier.getContactEmail(), supplier.getContactName(),
                    enquiry.getEnquiryCode(), enquiry.getItemDescription(), enquiry.getQuantity(),
                    requirement != null ? requirement.getAdditionalSpecifications() : null,
                    requirement != null ? requirement.getRequiredDeliveryDate() : null,
                    enquiry.getResponseDueDate(), enquiry.getNotes());
        });

        boolean sent = mailService.sendSupplierEnquiryEmail(new SupplierEnquiryEmailRequest(
                prep.email(), prep.contactName(), prep.enquiryCode(), prep.itemDescription(), prep.quantity(),
                prep.specs(), prep.deliveryDate(), prep.dueDate(), prep.message(),
                currentUser.getFullName(), currentUser.getEmail()));

        return transactionTemplate.execute(status -> {
            SupplierEnquiryReply reply = replyRepository.findById(prep.replyId())
                    .orElseThrow(() -> ResourceNotFoundException.of("SupplierEnquiryReply", prep.replyId()));
            if (sent) {
                reply.setEmailSentAt(Instant.now());
                reply.setEmailError(null);
            } else {
                reply.setEmailError("Delivery failed - try again or contact them directly");
            }
            replyRepository.save(reply);
            auditService.log("SupplierEnquiry", enquiryId, AuditAction.UPDATE,
                    "Resent enquiry email to " + reply.getSupplier().getCompanyName() + (sent ? "" : " (delivery failed)"));
            return SupplierEnquiryReplyResponse.from(reply);
        });
    }

    public SupplierEnquiryResponse recordReply(UUID enquiryId, UUID replyId, SupplierEnquiryReplyRequest req) {
        return transactionTemplate.execute(status -> {
            SupplierEnquiry enquiry = loadEnquiry(enquiryId);
            SupplierEnquiryReply reply = findReply(enquiry, replyId);

            if (req.status() == SupplierReplyStatus.REPLIED) {
                if (req.price() == null || req.price().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new BadRequestException("Price must be greater than zero for a recorded reply");
                }
                String currency = (req.currency() == null || req.currency().isBlank())
                        ? "INR" : req.currency().trim().toUpperCase();
                if (req.validUntil() != null && req.validUntil().isBefore(LocalDate.now())
                        && !req.validUntil().equals(reply.getValidUntil())) {
                    throw new BadRequestException("Valid-until date can't be in the past");
                }
                reply.setPrice(req.price());
                reply.setCurrency(currency);
                reply.setValidUntil(req.validUntil());
                reply.setDeliveryInfo(req.deliveryInfo());
                reply.setSupplierReference(req.supplierReference());
                reply.setTerms(req.terms());
                reply.setRemarks(req.remarks());
                reply.setStatus(SupplierReplyStatus.REPLIED);
            } else {
                reply.setStatus(SupplierReplyStatus.DECLINED);
                reply.setPrice(null);
                reply.setRemarks(req.remarks());
                if (reply.isChosen()) {
                    reply.setChosen(false);
                }
            }
            reply.setRepliedAt(Instant.now());
            replyRepository.save(reply);
            recomputeEnquiryStatus(enquiry);
            enquiryRepository.save(enquiry);

            auditService.log("SupplierEnquiry", enquiryId, AuditAction.UPDATE,
                    reply.getSupplier().getCompanyName() + " " + reply.getStatus().name().toLowerCase()
                            + (reply.getPrice() != null ? " at " + reply.getCurrency() + " " + reply.getPrice() : ""));
            return SupplierEnquiryResponse.from(enquiry);
        });
    }

    /**
     * Marks one reply as THE price reference for its requirement (PRD 12.3).
     * Uniqueness holds per requirement, not per enquiry - a requirement can be
     * re-sourced into a second enquiry later, and the earlier choice (possibly
     * on a different enquiry) must be cleared first, which is why this looks up
     * every currently-chosen reply for the requirement, not just this enquiry's.
     */
    public SupplierEnquiryResponse chooseReply(UUID enquiryId, UUID replyId) {
        return transactionTemplate.execute(status -> {
            SupplierEnquiry enquiry = loadEnquiry(enquiryId);
            SupplierEnquiryReply target = findReply(enquiry, replyId);
            Requirement requirement = target.getRequirement();

            if (requirement.getStatus() == RequirementStatus.CLOSED || requirement.getStatus() == RequirementStatus.CANCELLED) {
                throw new BadRequestException("Can't choose a price for a " + requirement.getStatus() + " requirement");
            }
            if (target.getStatus() != SupplierReplyStatus.REPLIED || target.getPrice() == null) {
                throw new BadRequestException("Can't choose a reply that hasn't come in yet");
            }
            if (target.getValidUntil() != null && target.getValidUntil().isBefore(LocalDate.now())) {
                throw new BadRequestException("This price has expired");
            }
            Supplier supplier = target.getSupplier();
            if (supplier.getStatus() == SupplierStatus.BLACKLISTED || supplier.getStatus() == SupplierStatus.ON_HOLD) {
                throw new BadRequestException(supplier.getCompanyName() + " is no longer usable");
            }

            List<SupplierEnquiryReply> previouslyChosen = replyRepository.findByRequirementIdAndChosenTrue(requirement.getId());
            Set<SupplierEnquiry> affectedEnquiries = new HashSet<>();
            for (SupplierEnquiryReply r : previouslyChosen) {
                r.setChosen(false);
                affectedEnquiries.add(r.getEnquiry());
            }
            replyRepository.saveAll(previouslyChosen);
            replyRepository.flush(); // before setting the new one - avoids a transient two-chosen state hitting the partial unique index

            target.setChosen(true);
            replyRepository.save(target);
            affectedEnquiries.add(enquiry);
            for (SupplierEnquiry e : affectedEnquiries) {
                recomputeEnquiryStatus(e);
                enquiryRepository.save(e);
            }

            auditService.log("SupplierEnquiry", enquiryId, AuditAction.STATUS_CHANGE,
                    supplier.getCompanyName() + " chosen as price reference (" + target.getCurrency() + " " + target.getPrice() + ")");
            return SupplierEnquiryResponse.from(enquiry);
        });
    }

    public SupplierEnquiryResponse unchooseReply(UUID enquiryId, UUID replyId) {
        return transactionTemplate.execute(status -> {
            SupplierEnquiry enquiry = loadEnquiry(enquiryId);
            SupplierEnquiryReply reply = findReply(enquiry, replyId);
            if (reply.isChosen()) {
                reply.setChosen(false);
                replyRepository.save(reply);
                recomputeEnquiryStatus(enquiry);
                enquiryRepository.save(enquiry);
                auditService.log("SupplierEnquiry", enquiryId, AuditAction.STATUS_CHANGE,
                        "Cleared " + reply.getSupplier().getCompanyName() + " as the price reference");
            }
            return SupplierEnquiryResponse.from(enquiry);
        });
    }

    public SupplierEnquiryResponse getById(UUID id) {
        return transactionTemplate.execute(status -> SupplierEnquiryResponse.from(loadEnquiry(id)));
    }

    public Page<SupplierEnquiryResponse> listByLead(UUID leadId, Pageable pageable) {
        leadService.getById(leadId); // 404 if the lead itself isn't visible
        return transactionTemplate.execute(status ->
                enquiryRepository.findByLeadId(leadId, pageable).map(SupplierEnquiryResponse::from));
    }

    public List<SupplierEnquiryResponse> listByRequirement(UUID requirementId) {
        Requirement requirement = requirementRepository.findById(requirementId)
                .orElseThrow(() -> ResourceNotFoundException.of("Requirement", requirementId));
        leadService.getById(requirement.getLead().getId());
        return transactionTemplate.execute(status ->
                enquiryRepository.findByRequirementIdOrderByCreatedAtDesc(requirementId).stream()
                        .map(SupplierEnquiryResponse::from).toList());
    }

    /** Supplier-side "previous transactions" history (PRD 12.1), filtered to replies
     *  whose lead the caller can see - never another team's sourcing activity. */
    public Page<SupplierEnquiryReplyResponse> listBySupplier(UUID supplierId, Pageable pageable) {
        supplierService.getById(supplierId); // 404 on unknown/archived supplier
        return transactionTemplate.execute(status -> {
            Specification<SupplierEnquiryReply> spec = Specification.where(replyLeadVisibility())
                    .and((root, query, cb) -> cb.equal(root.get("supplier").get("id"), supplierId));
            return replyRepository.findAll(spec, pageable).map(SupplierEnquiryReplyResponse::from);
        });
    }

    // --- internals ---

    private SupplierEnquiry loadEnquiry(UUID id) {
        SupplierEnquiry enquiry = enquiryRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("SupplierEnquiry", id));
        leadService.getById(enquiry.getLead().getId()); // re-checks visibility
        return enquiry;
    }

    private SupplierEnquiryReply findReply(SupplierEnquiry enquiry, UUID replyId) {
        return enquiry.getReplies().stream()
                .filter(r -> r.getId().equals(replyId))
                .findFirst()
                .orElseThrow(() -> ResourceNotFoundException.of("SupplierEnquiryReply", replyId));
    }

    private void recomputeEnquiryStatus(SupplierEnquiry enquiry) {
        boolean anyChosen = enquiry.getReplies().stream().anyMatch(SupplierEnquiryReply::isChosen);
        enquiry.setStatus(anyChosen ? SupplierEnquiryStatus.CLOSED : SupplierEnquiryStatus.OPEN);
    }

    /** Mirrors LeadAccessService's rule, rooted at SupplierEnquiryReply instead of
     *  Lead (Specification's generic type won't let LeadAccessService's own
     *  Lead-rooted Specification be reused directly for a join like this one). If
     *  the visibility rule ever changes, both need updating together. */
    private Specification<SupplierEnquiryReply> replyLeadVisibility() {
        User user = currentUserService.get();
        return switch (user.getRole()) {
            case SUPER_ADMIN, ADMIN -> null;
            case MANAGER -> {
                UUID teamId = user.getTeam() != null ? user.getTeam().getId() : null;
                if (teamId == null) {
                    yield (root, query, cb) -> cb.disjunction();
                }
                yield (root, query, cb) -> cb.equal(
                        root.get("enquiry").get("lead").get("owner").get("team").get("id"), teamId);
            }
            case COUNSELOR, VIEWER -> {
                UUID userId = user.getId();
                yield (root, query, cb) -> cb.equal(root.get("enquiry").get("lead").get("owner").get("id"), userId);
            }
        };
    }

    private String formatQuantity(BigDecimal qty, String unit) {
        if (qty == null) return null;
        String qtyStr = qty.stripTrailingZeros().toPlainString();
        return (unit != null && !unit.isBlank()) ? qtyStr + " " + unit : qtyStr;
    }

    private String nextEnquiryCode() {
        Number seq = (Number) entityManager.createNativeQuery("SELECT nextval('supplier_enquiry_code_seq')")
                .getSingleResult();
        return String.format("ENQ-%06d", seq.longValue());
    }

    private record PendingEmail(UUID replyId, String email, String contactName) {}

    private record EmailOutcome(UUID replyId, boolean sent) {}

    private record CreatePhaseResult(UUID enquiryId, String enquiryCode, String itemDescription, String quantity,
            String specs, LocalDate deliveryDate, LocalDate dueDate, List<PendingEmail> pendingEmails) {}

    private record ResendPrep(UUID replyId, String email, String contactName, String enquiryCode,
            String itemDescription, String quantity, String specs, LocalDate deliveryDate, LocalDate dueDate,
            String message) {}
}
