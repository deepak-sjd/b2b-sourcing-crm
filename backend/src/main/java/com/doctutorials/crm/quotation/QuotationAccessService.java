package com.doctutorials.crm.quotation;

import com.doctutorials.crm.common.exception.ResourceNotFoundException;
import com.doctutorials.crm.security.CurrentUserService;
import com.doctutorials.crm.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Who can see which quotation - deliberately NOT its own ownership field.
 * A quotation belongs to a customer, so visibility follows the customer's
 * account manager exactly like CustomerAccessService (see its javadoc for
 * the fuller reasoning): SUPER_ADMIN/ADMIN see everything, MANAGER sees
 * their team's, COUNSELOR/VIEWER see only their own customers' quotations.
 * Keeping one visibility rule per customer (rather than a second, possibly
 * drifting one on Quotation) is the point.
 */
@Service
@RequiredArgsConstructor
public class QuotationAccessService {

    private final CurrentUserService currentUserService;

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
                yield (root, query, cb) ->
                        cb.equal(root.get("customer").get("accountManager").get("team").get("id"), teamId);
            }
            case COUNSELOR, VIEWER -> {
                UUID userId = user.getId();
                yield (root, query, cb) -> cb.equal(root.get("customer").get("accountManager").get("id"), userId);
            }
        };
    }

    public void assertVisible(Quotation quotation) {
        User user = currentUserService.get();
        boolean visible = switch (user.getRole()) {
            case SUPER_ADMIN, ADMIN -> true;
            case MANAGER -> quotation.getCustomer().getAccountManager() != null
                    && quotation.getCustomer().getAccountManager().getTeam() != null
                    && user.getTeam() != null
                    && quotation.getCustomer().getAccountManager().getTeam().getId().equals(user.getTeam().getId());
            case COUNSELOR, VIEWER -> quotation.getCustomer().getAccountManager() != null
                    && quotation.getCustomer().getAccountManager().getId().equals(user.getId());
        };
        if (!visible) {
            throw ResourceNotFoundException.of("Quotation", quotation.getId());
        }
    }
}
