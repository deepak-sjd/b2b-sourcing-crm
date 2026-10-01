package com.doctutorials.crm.lead;

import com.doctutorials.crm.common.exception.BadRequestException;
import com.doctutorials.crm.common.exception.ResourceNotFoundException;
import com.doctutorials.crm.security.CurrentUserService;
import com.doctutorials.crm.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * The single source of truth for "who can see/touch which lead". Every
 * query and mutation that touches Lead (and, transitively, Task/Call/Note,
 * which only ever exist attached to a Lead) must go through this rather
 * than re-deriving the rule - that's what keeps this a one-place-to-audit
 * property instead of N slightly-different copies drifting apart over time.
 *
 * Visibility rule:
 * SUPER_ADMIN, ADMIN -> everything (org-wide).
 * MANAGER -> leads owned by any user on the manager's own team.
 * COUNSELOR, VIEWER -> leads owned by the user themself.
 *
 * A lead with no owner is only visible to SUPER_ADMIN/ADMIN - it's sitting
 * in an unassigned/triage state, not yet anyone's to see. If you later want
 * counselors to browse an unclaimed lead pool, that's a deliberate new rule
 * to add here, not an oversight to "fix" - handle it explicitly rather than
 * loosening this by accident.
 */
@Service
@RequiredArgsConstructor
public class LeadAccessService {

    private final CurrentUserService currentUserService;

    /**
     * Specification to AND onto any Lead query so results are pre-filtered to what
     * the caller can see.
     */
    public Specification<Lead> leadVisibility() {
        return visibilityFor(currentUserService.get());
    }

    private Specification<Lead> visibilityFor(User user) {
        return switch (user.getRole()) {
            case SUPER_ADMIN, ADMIN -> null; // no restriction - Specification.where(null) is a valid no-op
            case MANAGER -> {
                UUID teamId = user.getTeam() != null ? user.getTeam().getId() : null;
                if (teamId == null) {
                    // Fail closed: a manager with no team assigned sees nothing rather
                    // than everything. Fix the data (assign them a team); don't relax this.
                    yield (root, query, cb) -> cb.disjunction();
                }
                yield (root, query, cb) -> cb.equal(root.get("owner").get("team").get("id"), teamId);
            }
            case COUNSELOR, VIEWER -> {
                UUID userId = user.getId();
                yield (root, query, cb) -> cb.equal(root.get("owner").get("id"), userId);
            }
        };
    }

    /**
     * Throws ResourceNotFoundException (not 403) when the lead exists but the
     * current user can't see it. A 404 here is deliberate, not an oversight -
     * it means "this ID doesn't exist as far as you're concerned" rather than
     * "this ID exists, you're just not allowed", which would confirm to an
     * unauthorized caller that a given lead ID is real.
     */
    public void assertVisible(Lead lead) {
        if (!isVisible(lead, currentUserService.get())) {
            throw ResourceNotFoundException.of("Lead", lead.getId());
        }
    }

    public boolean isVisible(Lead lead, User user) {
        return switch (user.getRole()) {
            case SUPER_ADMIN, ADMIN -> true;
            case MANAGER -> lead.getOwner() != null
                    && lead.getOwner().getTeam() != null
                    && user.getTeam() != null
                    && lead.getOwner().getTeam().getId().equals(user.getTeam().getId());
            case COUNSELOR, VIEWER -> lead.getOwner() != null && lead.getOwner().getId().equals(user.getId());
        };
    }

    /**
     * Enforces who a lead can be handed to, on create/update/assign. Without
     * this, visibility scoping alone wouldn't stop a counselor from creating
     * a lead "owned by" someone else, or a manager handing a lead to a user
     * outside their own team - the data would just immediately vanish from
     * the actor's own view, which is confusing rather than actually safe.
     */
    public void assertCanAssignOwner(User currentUser, User requestedOwner) {
        switch (currentUser.getRole()) {
            case SUPER_ADMIN, ADMIN -> {
                /* no restriction */ }
            case MANAGER -> {
                if (currentUser.getTeam() == null || requestedOwner.getTeam() == null
                        || !currentUser.getTeam().getId().equals(requestedOwner.getTeam().getId())) {
                    throw new BadRequestException("Cannot assign a lead to a user outside your team");
                }
            }
            case COUNSELOR, VIEWER -> {
                if (!requestedOwner.getId().equals(currentUser.getId())) {
                    throw new BadRequestException("You can only own leads assigned to yourself");
                }
            }
        }
    }
}
