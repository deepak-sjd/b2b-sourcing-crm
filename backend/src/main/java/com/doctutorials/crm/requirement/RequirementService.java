package com.doctutorials.crm.requirement;

import com.doctutorials.crm.audit.AuditAction;
import com.doctutorials.crm.audit.AuditService;
import com.doctutorials.crm.common.exception.BadRequestException;
import com.doctutorials.crm.common.exception.ResourceNotFoundException;
import com.doctutorials.crm.lead.Lead;
import com.doctutorials.crm.lead.LeadService;
import com.doctutorials.crm.lead.Priority;
import com.doctutorials.crm.requirement.dto.RequirementRequest;
import com.doctutorials.crm.requirement.dto.RequirementResponse;
import com.doctutorials.crm.security.CurrentUserService;
import com.doctutorials.crm.user.User;
import com.doctutorials.crm.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RequirementService {

    private final RequirementRepository requirementRepository;
    private final LeadService leadService;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final RequirementAccessService accessService;
    private final CurrentUserService currentUserService;

    @Transactional
    public RequirementResponse create(RequirementRequest req) {
        if (req.leadId() == null) {
            throw new BadRequestException("leadId is required");
        }
        // LeadService.getById enforces lead visibility (404 for a lead the caller can't see).
        Lead lead = leadService.getById(req.leadId());

        Requirement r = new Requirement();
        r.setLead(lead);
        r.setCreatedBy(currentUserService.get());
        apply(r, req);

        // Default the assignee to the lead's owner so a new requirement is never orphaned.
        if (r.getAssignedTo() == null) {
            r.setAssignedTo(lead.getOwner());
        }

        Requirement saved = requirementRepository.save(r);
        auditService.log("Requirement", saved.getId(), AuditAction.CREATE,
                "Requirement added: " + saved.getProductName() + " (lead " + lead.getFullName() + ")");
        return RequirementResponse.from(saved);
    }

    @Transactional
    public RequirementResponse update(UUID id, RequirementRequest req) {
        Requirement r = load(id);
        if (r.getStatus() == RequirementStatus.CLOSED || r.getStatus() == RequirementStatus.CANCELLED) {
            throw new BadRequestException("A " + r.getStatus() + " requirement can't be edited; reopen it first");
        }
        apply(r, req);
        Requirement saved = requirementRepository.save(r);
        auditService.log("Requirement", id, AuditAction.UPDATE, "Requirement updated: " + saved.getProductName());
        return RequirementResponse.from(saved);
    }

    @Transactional
    public RequirementResponse changeStatus(UUID id, RequirementStatus newStatus) {
        Requirement r = load(id);
        RequirementStatus old = r.getStatus();
        if (old == newStatus) {
            return RequirementResponse.from(r);
        }
        // SOURCING / QUOTED are driven by the enquiry and quotation modules, but allow manual
        // correction by a human. Only guard the one nonsensical jump.
        if (newStatus == RequirementStatus.SOURCING && !r.isNeedsSupplier()) {
            throw new BadRequestException("Mark this requirement as 'needs supplier' before moving it to sourcing");
        }
        r.setStatus(newStatus);
        Requirement saved = requirementRepository.save(r);
        auditService.log("Requirement", id, AuditAction.STATUS_CHANGE,
                "Requirement status " + old + " -> " + newStatus);
        return RequirementResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public RequirementResponse getById(UUID id) {
        return RequirementResponse.from(load(id));
    }

    /** Visibility-checked fetch; visibility follows the parent lead. Call only inside a transaction. */
    private Requirement load(UUID id) {
        Requirement r = requirementRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Requirement", id));
        leadService.getById(r.getLead().getId());
        return r;
    }

    @Transactional(readOnly = true)
    public Page<RequirementResponse> listByLead(UUID leadId, Pageable pageable) {
        leadService.getById(leadId);
        return requirementRepository.findByLeadId(leadId, pageable).map(RequirementResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<RequirementResponse> listAll(String q, RequirementStatus status, Priority priority,
            Boolean needsSupplier, Pageable pageable) {
        Specification<Requirement> spec = Specification.where(accessService.requirementVisibility());
        if (q != null && !q.isBlank()) {
            String like = "%" + q.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("productName")), like),
                    cb.like(cb.lower(root.get("lead").get("fullName")), like)));
        }
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (priority != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("priority"), priority));
        }
        if (needsSupplier != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("needsSupplier"), needsSupplier));
        }
        return requirementRepository.findAll(spec, pageable).map(RequirementResponse::from);
    }

    /** Counts scoped to what the caller can see; powers the Requirements page cards. */
    @Transactional(readOnly = true)
    public Map<String, Long> summary() {
        Specification<Requirement> base = Specification.where(accessService.requirementVisibility());
        Map<String, Long> out = new LinkedHashMap<>();
        out.put("total", requirementRepository.count(base));
        for (RequirementStatus s : RequirementStatus.values()) {
            out.put(s.name().toLowerCase(),
                    requirementRepository.count(base.and((root, query, cb) -> cb.equal(root.get("status"), s))));
        }
        out.put("awaitingSupplier", requirementRepository.count(base.and((root, query, cb) -> cb.and(
                cb.isTrue(root.get("needsSupplier")),
                cb.equal(root.get("status"), RequirementStatus.OPEN)))));
        return out;
    }

    private void apply(Requirement r, RequirementRequest req) {
        r.setProductName(req.productName().trim());
        r.setDescription(blankToNull(req.description()));
        r.setQuantity(req.quantity());
        r.setUnit(blankToNull(req.unit()));
        r.setExpectedTimeline(blankToNull(req.expectedTimeline()));
        r.setBudget(req.budget());
        r.setPriority(req.priority() != null ? req.priority() : Priority.MEDIUM);
        r.setRequiredDeliveryDate(req.requiredDeliveryDate());
        r.setAdditionalSpecifications(blankToNull(req.additionalSpecifications()));
        r.setRemarks(blankToNull(req.remarks()));
        r.setNeedsSupplier(Boolean.TRUE.equals(req.needsSupplier()));

        if (req.assignedToId() != null) {
            User assignee = userRepository.findById(req.assignedToId())
                    .orElseThrow(() -> ResourceNotFoundException.of("User", req.assignedToId()));
            r.setAssignedTo(assignee);
        }
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
