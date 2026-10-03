package com.doctutorials.crm.lead;

import com.doctutorials.crm.audit.AuditAction;
import com.doctutorials.crm.audit.AuditService;
import com.doctutorials.crm.common.exception.BadRequestException;
import com.doctutorials.crm.common.exception.ResourceNotFoundException;
import com.doctutorials.crm.lead.dto.LeadRequest;
import com.doctutorials.crm.security.CurrentUserService;
import com.doctutorials.crm.user.Role;
import com.doctutorials.crm.user.User;
import com.doctutorials.crm.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LeadService {

    private final LeadRepository leadRepository;
    private final UserRepository userRepository;
    private final LeadScoringService scoringService;
    private final LeadAiInsightService aiInsightService;
    private final LeadAutoAssignmentService autoAssignmentService;
    private final AuditService auditService;
    private final LeadAccessService leadAccessService;
    private final CurrentUserService currentUserService;

    @Value("${app.lead.stale-lead-threshold-days:7}")
    private long staleThresholdDays;

    @Transactional
    public Lead create(LeadRequest req) {
        Lead lead = new Lead();
        applyRequest(lead, req);
        lead.setStage(LeadStage.NEW);
        scoringService.rescore(lead);
        aiInsightService.generate(lead);

        Lead saved = leadRepository.save(lead);
        auditService.log("Lead", saved.getId(), AuditAction.CREATE,
                "Lead created: " + saved.getFullName()
                        + (saved.getOwner() != null ? " (auto-assigned to " + saved.getOwner().getFullName() + ")" : ""));
        return saved;
    }

    @Transactional
    public Lead update(UUID id, LeadRequest req) {
        Lead lead = getById(id);
        applyRequest(lead, req);
        scoringService.rescore(lead);
        aiInsightService.generate(lead);

        Lead saved = leadRepository.save(lead);
        auditService.log("Lead", saved.getId(), AuditAction.UPDATE, "Lead details updated");
        return saved;
    }

    /**
     * Handles the four non-terminal stage moves (NEW/CONTACTED/QUALIFIED/OPPORTUNITY).
     * WON and LOST are rejected here and must go through markWon/markLost instead,
     * because the PRD requires data those two don't carry - a reason for LOST,
     * a closedAt timestamp for both. Without this split, the old generic
     * endpoint let a caller flip a lead straight to LOST with zero reason
     * recorded, which is exactly the gap this feature closes.
     */
    @Transactional
    public Lead changeStage(UUID id, LeadStage newStage) {
        Lead lead = getById(id);
        LeadStage oldStage = lead.getStage();

        if (oldStage == LeadStage.WON || oldStage == LeadStage.LOST) {
            throw new BadRequestException("Cannot move a lead out of a closed stage (" + oldStage + ")");
        }
        if (newStage == LeadStage.WON || newStage == LeadStage.LOST) {
            throw new BadRequestException(
                    "Use the Mark Won / Mark Lost action to close a lead - " + newStage + " requires closure details");
        }

        lead.setStage(newStage);
        scoringService.rescore(lead);
        aiInsightService.generate(lead);
        Lead saved = leadRepository.save(lead);

        auditService.log("Lead", id, AuditAction.STAGE_CHANGE,
                "Stage changed from " + oldStage + " to " + newStage);
        return saved;
    }

    @Transactional
    public Lead markWon(UUID id, BigDecimal finalValue, String remarks) {
        Lead lead = getById(id);
        assertOpenStage(lead);

        LeadStage oldStage = lead.getStage();
        lead.setStage(LeadStage.WON);
        lead.setClosedAt(Instant.now());
        lead.setFinalValue(finalValue);
        lead.setClosureRemarks(remarks);
        scoringService.rescore(lead);
        aiInsightService.generate(lead);

        Lead saved = leadRepository.save(lead);
        auditService.log("Lead", id, AuditAction.STAGE_CHANGE,
                "Marked WON (from " + oldStage + ")"
                        + (finalValue != null ? " - final value " + finalValue : ""));
        return saved;
    }

    @Transactional
    public Lead markLost(UUID id, LostReason reason, String comment) {
        Lead lead = getById(id);
        assertOpenStage(lead);

        LeadStage oldStage = lead.getStage();
        lead.setStage(LeadStage.LOST);
        lead.setClosedAt(Instant.now());
        lead.setLostReason(reason);
        lead.setLostComment(comment);
        scoringService.rescore(lead);
        aiInsightService.generate(lead);

        Lead saved = leadRepository.save(lead);
        auditService.log("Lead", id, AuditAction.STAGE_CHANGE,
                "Marked LOST (from " + oldStage + ") - reason: " + reason);
        return saved;
    }

    private void assertOpenStage(Lead lead) {
        if (lead.getStage() == LeadStage.WON || lead.getStage() == LeadStage.LOST) {
            throw new BadRequestException("This lead is already closed (" + lead.getStage() + ")");
        }
    }

    /**
     * PRD §14.3: "not suitable, do not match the requirement, are inactive,
     * duplicate or incorrect, or are no longer worked on." WON/LOST are
     * excluded - both are already a final outcome with their own reason
     * trail, and scrapping on top of that would blur two different questions
     * ("why did this not become business" vs "why was this never a real
     * lead").
     */
    @Transactional
    public Lead moveToScrap(UUID id, ScrapReason reason, String remarks) {
        Lead lead = getById(id);
        if (lead.isScrapped()) {
            throw new BadRequestException("This lead is already in Scrap");
        }
        if (lead.getStage() == LeadStage.WON || lead.getStage() == LeadStage.LOST) {
            throw new BadRequestException("A " + lead.getStage() + " lead already has a final outcome and can't also be scrapped");
        }

        lead.setScrapped(true);
        lead.setScrapReason(reason);
        lead.setScrapRemarks(remarks);
        lead.setScrappedAt(Instant.now());

        Lead saved = leadRepository.save(lead);
        auditService.log("Lead", id, AuditAction.DELETE, "Moved to Scrap - reason: " + reason);
        return saved;
    }

    /**
     * PRD §14.3: "A Scrap record can be reactivated if the customer becomes
     * relevant again." Deliberately leaves `stage` untouched - a scrapped
     * lead's stage is frozen at whatever it was when scrapped (see
     * moveToScrap, which never changes it), so reactivating just un-hides it
     * back into wherever it already was in the funnel rather than resetting
     * it to NEW and losing that progress.
     */
    @Transactional
    public Lead reactivate(UUID id) {
        Lead lead = getById(id);
        if (!lead.isScrapped()) {
            throw new BadRequestException("This lead is not in Scrap");
        }

        lead.setScrapped(false);
        lead.setScrapReason(null);
        lead.setScrapRemarks(null);
        lead.setScrappedAt(null);

        Lead saved = leadRepository.save(lead);
        auditService.log("Lead", id, AuditAction.UPDATE, "Reactivated from Scrap");
        return saved;
    }

    @Transactional
    public void recordTouchpoint(UUID id) {
        Lead lead = getById(id);
        scoringService.markTouched(lead);
        aiInsightService.generate(lead);
        leadRepository.save(lead);
    }

    /**
     * Manually re-runs scoring + AI insight generation without changing any
     * other field — backs the "Refresh AI Insight" button on the Lead detail
     * page's AI panel, and is reused by LeadAiRefreshJob for the nightly sweep.
     */
    @Transactional
    public Lead refreshAiInsight(UUID id) {
        Lead lead = getById(id);
        scoringService.rescore(lead);
        aiInsightService.generate(lead);
        return leadRepository.save(lead);
    }

    @Transactional
    public Lead assignOwner(UUID id, UUID ownerId) {
        Lead lead = getById(id);
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> ResourceNotFoundException.of("User", ownerId));
        leadAccessService.assertCanAssignOwner(currentUserService.get(), owner);
        lead.setOwner(owner);
        Lead saved = leadRepository.save(lead);
        auditService.log("Lead", id, AuditAction.UPDATE, "Reassigned to " + owner.getFullName());
        return saved;
    }

    public Lead getById(UUID id) {
        Lead lead = leadRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Lead", id));
        leadAccessService.assertVisible(lead);
        return lead;
    }

    /**
     * notScrapped() is a soft delete - it flips the scrapped flag rather than
     * deleting the row, so audit history and attached tasks/calls/notes survive.
     * That only behaves like a delete if every list path actually filters the
     * flag out, which is what this spec is for. Previously only findAtRiskLeads
     * filtered it, so scrapping a lead appeared to do nothing: the lead stayed
     * in the main list. (Renamed from notArchived() alongside the
     * archived -> scrapped column rename - see V12 migration.)
     */
    private Specification<Lead> notScrapped() {
        return (root, query, cb) -> cb.isFalse(root.get("scrapped"));
    }

    public Page<Lead> list(Pageable pageable) {
        Specification<Lead> spec = Specification.where(leadAccessService.leadVisibility())
                .and(notScrapped());
        return leadRepository.findAll(spec, pageable);
    }

    public Page<Lead> listByStage(LeadStage stage, Pageable pageable) {
        Specification<Lead> spec = Specification.where(leadAccessService.leadVisibility())
                .and(notScrapped())
                .and((root, query, cb) -> cb.equal(root.get("stage"), stage));
        return leadRepository.findAll(spec, pageable);
    }

    public Page<Lead> listByOwner(UUID ownerId, Pageable pageable) {
        // Combining the caller's visibility scope with the requested ownerId (rather
        // than treating ownerId as a separate permission check) means an out-of-scope
        // query just comes back empty by construction - there's no separate "are you
        // allowed to ask for this ownerId" branch to accidentally get wrong or forget
        // to update later.
        Specification<Lead> spec = Specification.where(leadAccessService.leadVisibility())
                .and(notScrapped())
                .and((root, query, cb) -> cb.equal(root.get("owner").get("id"), ownerId));
        return leadRepository.findAll(spec, pageable);
    }

    /** Powers the Scrap Records page (PRD §14.3: "Authorised users can search
     *  and review Scrap records"). Deliberately the mirror image of list() -
     *  scrapped-only instead of notScrapped() - rather than a boolean param
     *  threaded through list(), so the "everywhere else" default stays the
     *  one that can't accidentally leak scrapped rows into a normal view. */
    public Page<Lead> listScrapped(Pageable pageable) {
        Specification<Lead> spec = Specification.where(leadAccessService.leadVisibility())
                .and((root, query, cb) -> cb.isTrue(root.get("scrapped")));
        return leadRepository.findAll(spec, pageable);
    }

    /**
     * Powers the "inactive for N days" risk banner across the whole pipeline, not
     * just one lead.
     */
    public List<Lead> findAtRiskLeads() {
        Instant threshold = Instant.now().minus(staleThresholdDays, ChronoUnit.DAYS);
        Specification<Lead> spec = Specification.where(leadAccessService.leadVisibility())
                .and(notScrapped())
                .and((root, query, cb) -> cb.lessThan(root.get("lastTouchpointAt"), threshold));
        return leadRepository.findAll(spec);
    }

    public long getStaleThresholdDays() {
        return staleThresholdDays;
    }

    /**
     * Rescoring + AI insight refresh for every open lead, org-wide - deliberately
     * bypassing LeadAccessService, unlike every other method on this class. Every
     * other query here is scoped to "what can the calling user see" because it
     * runs inside a specific user's authenticated request; this one runs from
     * LeadAiRefreshJob on a scheduler thread with no user in context at all, and
     * its job genuinely is "every open lead in the system", not "every open lead
     * one particular user can see". Do not call this from a controller.
     */
    @Transactional
    public int refreshAllOpenLeadsForSystemJob() {
        Specification<Lead> spec = Specification.where(notScrapped())
                .and((root, query, cb) -> cb.not(root.get("stage").in(List.of(LeadStage.WON, LeadStage.LOST))));
        List<Lead> openLeads = leadRepository.findAll(spec);
        for (Lead lead : openLeads) {
            scoringService.rescore(lead);
            aiInsightService.generate(lead);
        }
        leadRepository.saveAll(openLeads);
        return openLeads.size();
    }

    private void applyRequest(Lead lead, LeadRequest req) {
        lead.setFullName(req.fullName());
        lead.setPhone(req.phone());
        lead.setEmail(req.email());
        lead.setCity(req.city());
        lead.setInterest(req.interest());
        lead.setBudget(req.budget());
        if (req.priority() != null)
            lead.setPriority(req.priority());
        lead.setSource(req.source());
        lead.setCampaign(req.campaign());

        User currentUser = currentUserService.get();

        if (req.ownerId() != null) {
            User owner = userRepository.findById(req.ownerId())
                    .orElseThrow(() -> ResourceNotFoundException.of("User", req.ownerId()));
            leadAccessService.assertCanAssignOwner(currentUser, owner);
            lead.setOwner(owner);
        } else if (lead.getOwner() == null && currentUser.getRole() == Role.COUNSELOR) {
            // A counselor creating a lead with no ownerId specified becomes its owner by
            // default - there's no sensible "unowned" state for a counselor-created lead,
            // and this matches how the UI screenshots always show a single named owner.
            lead.setOwner(currentUser);
        } else if (lead.getOwner() == null
                && (currentUser.getRole() == Role.MANAGER
                        || currentUser.getRole() == Role.ADMIN
                        || currentUser.getRole() == Role.SUPER_ADMIN)) {
            // Auto-assignment: a manager/admin creating a lead without naming an owner
            // gets it routed to the least-loaded eligible counselor instead of it sitting
            // unowned in triage. Silently a no-op (lead stays unassigned) if there's no
            // eligible counselor in scope - see LeadAutoAssignmentService for the rule.
            User autoOwner = autoAssignmentService.pickOwnerFor(currentUser);
            if (autoOwner != null) {
                lead.setOwner(autoOwner);
            }
        }
    }
}
