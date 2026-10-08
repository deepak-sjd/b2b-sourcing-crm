package com.doctutorials.crm.quotation;

import com.doctutorials.crm.common.exception.ResourceNotFoundException;
import com.doctutorials.crm.customer.Customer;
import com.doctutorials.crm.customer.CustomerAccessService;
import com.doctutorials.crm.lead.Lead;
import com.doctutorials.crm.lead.LeadAccessService;
import com.doctutorials.crm.security.CurrentUserService;
import com.doctutorials.crm.user.User;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Who can see which quotation - deliberately NOT its own ownership field.
 * A quotation belongs to EITHER a customer or a lead (see Quotation.java's
 * class comment), so visibility is "visible via the customer rule OR visible
 * via the lead rule" - for a single entity, assertVisible just reuses
 * CustomerAccessService/LeadAccessService's own isVisible so the actual role
 * logic lives in exactly one place each.
 *
 * The list-query Specification can't reuse those the same way (they build
 * Specification<Customer>/Specification<Lead>, not Specification<Quotation>),
 * so it's rebuilt here with explicit LEFT JOINs. That "explicit" matters:
 * navigating root.get("customer").get(...) in a WHERE clause makes Hibernate
 * add an implicit INNER JOIN on customer for the WHOLE query, which would
 * silently drop every lead-only quotation before the OR even runs. Using
 * root.join("customer", JoinType.LEFT) avoids that.
 */
@Service
@RequiredArgsConstructor
public class QuotationAccessService {

    private final CurrentUserService currentUserService;
    private final CustomerAccessService customerAccessService;
    private final LeadAccessService leadAccessService;

    public Specification<Quotation> quotationVisibility() {
        return visibilityFor(currentUserService.get());
    }

    private Specification<Quotation> visibilityFor(User user) {
        return switch (user.getRole()) {
            case SUPER_ADMIN, ADMIN -> null;
            case MANAGER -> {
                UUID teamId = user.getTeam() != null ? user.getTeam().getId() : null;
                if (teamId == null) {
                    yield (root, query, cb) -> cb.disjunction();
                }
                yield (root, query, cb) -> {
                    Join<Quotation, Customer> customerJoin = root.join("customer", JoinType.LEFT);
                    Join<Customer, User> accountManagerJoin = customerJoin.join("accountManager", JoinType.LEFT);
                    Join<Quotation, Lead> leadJoin = root.join("lead", JoinType.LEFT);
                    Join<Lead, User> leadOwnerJoin = leadJoin.join("owner", JoinType.LEFT);
                    return cb.or(
                            cb.equal(accountManagerJoin.get("team").get("id"), teamId),
                            cb.equal(leadOwnerJoin.get("team").get("id"), teamId));
                };
            }
            case COUNSELOR, VIEWER -> {
                UUID userId = user.getId();
                yield (root, query, cb) -> {
                    Join<Quotation, Customer> customerJoin = root.join("customer", JoinType.LEFT);
                    Join<Customer, User> accountManagerJoin = customerJoin.join("accountManager", JoinType.LEFT);
                    Join<Quotation, Lead> leadJoin = root.join("lead", JoinType.LEFT);
                    Join<Lead, User> leadOwnerJoin = leadJoin.join("owner", JoinType.LEFT);
                    return cb.or(
                            cb.equal(accountManagerJoin.get("id"), userId),
                            cb.equal(leadOwnerJoin.get("id"), userId));
                };
            }
        };
    }

    public void assertVisible(Quotation quotation) {
        User user = currentUserService.get();
        boolean visible = (quotation.getCustomer() != null && customerAccessService.isVisible(quotation.getCustomer(), user))
                || (quotation.getLead() != null && leadAccessService.isVisible(quotation.getLead(), user));
        if (!visible) {
            throw ResourceNotFoundException.of("Quotation", quotation.getId());
        }
    }
}
