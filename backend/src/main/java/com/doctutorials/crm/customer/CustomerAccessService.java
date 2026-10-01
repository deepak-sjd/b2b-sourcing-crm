package com.doctutorials.crm.customer;

import com.doctutorials.crm.common.exception.BadRequestException;
import com.doctutorials.crm.common.exception.ResourceNotFoundException;
import com.doctutorials.crm.security.CurrentUserService;
import com.doctutorials.crm.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * The single source of truth for "who can see/touch which customer" — same
 * shape as LeadAccessService, keyed on accountManager instead of owner.
 *
 * Visibility rule:
 * SUPER_ADMIN, ADMIN -> everything (org-wide).
 * MANAGER -> customers whose account manager is on the manager's own team.
 * COUNSELOR, VIEWER -> customers they are the account manager for.
 *
 * A customer with no account manager is only visible to SUPER_ADMIN/ADMIN -
 * it's unassigned/triage state, not yet anyone's to see. Same reasoning as
 * an unowned Lead; see LeadAccessService's javadoc for the fuller argument.
 */
@Service
@RequiredArgsConstructor
public class CustomerAccessService {

    private final CurrentUserService currentUserService;

    public Specification<Customer> customerVisibility() {
        return visibilityFor(currentUserService.get());
    }

    private Specification<Customer> visibilityFor(User user) {
        return switch (user.getRole()) {
            case SUPER_ADMIN, ADMIN -> null; // no restriction - Specification.where(null) is a valid no-op
            case MANAGER -> {
                UUID teamId = user.getTeam() != null ? user.getTeam().getId() : null;
                if (teamId == null) {
                    // Fail closed: a manager with no team assigned sees nothing rather
                    // than everything. Fix the data; don't relax this.
                    yield (root, query, cb) -> cb.disjunction();
                }
                yield (root, query, cb) -> cb.equal(root.get("accountManager").get("team").get("id"), teamId);
            }
            case COUNSELOR, VIEWER -> {
                UUID userId = user.getId();
                yield (root, query, cb) -> cb.equal(root.get("accountManager").get("id"), userId);
            }
        };
    }

    /**
     * Throws ResourceNotFoundException (not 403) when the customer exists but
     * the current user can't see it — a 404 here means "this ID doesn't exist
     * as far as you're concerned", not "this ID exists, you're just not
     * allowed", which would confirm to an unauthorized caller that a given
     * customer ID is real. Same reasoning as LeadAccessService.assertVisible.
     */
    public void assertVisible(Customer customer) {
        if (!isVisible(customer, currentUserService.get())) {
            throw ResourceNotFoundException.of("Customer", customer.getId());
        }
    }

    public boolean isVisible(Customer customer, User user) {
        return switch (user.getRole()) {
            case SUPER_ADMIN, ADMIN -> true;
            case MANAGER -> customer.getAccountManager() != null
                    && customer.getAccountManager().getTeam() != null
                    && user.getTeam() != null
                    && customer.getAccountManager().getTeam().getId().equals(user.getTeam().getId());
            case COUNSELOR, VIEWER -> customer.getAccountManager() != null
                    && customer.getAccountManager().getId().equals(user.getId());
        };
    }

    /**
     * Enforces who a customer can be handed to, on create/update/reassign —
     * without this, visibility scoping alone wouldn't stop a counselor
     * creating a customer "managed by" someone else, or a manager handing a
     * customer to a user outside their own team.
     */
    public void assertCanAssignAccountManager(User currentUser, User requestedManager) {
        switch (currentUser.getRole()) {
            case SUPER_ADMIN, ADMIN -> {
                /* no restriction */ }
            case MANAGER -> {
                if (currentUser.getTeam() == null || requestedManager.getTeam() == null
                        || !currentUser.getTeam().getId().equals(requestedManager.getTeam().getId())) {
                    throw new BadRequestException("Cannot assign a customer to a user outside your team");
                }
            }
            case COUNSELOR, VIEWER -> {
                if (!requestedManager.getId().equals(currentUser.getId())) {
                    throw new BadRequestException("You can only manage customers assigned to yourself");
                }
            }
        }
    }
}
