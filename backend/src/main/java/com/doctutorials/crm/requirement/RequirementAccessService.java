package com.doctutorials.crm.requirement;

import com.doctutorials.crm.security.CurrentUserService;
import com.doctutorials.crm.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * A requirement is visible exactly to the audience of its parent lead. Same
 * rule as TaskAccessService, expressed over Requirement.lead.owner. Fails
 * closed for a manager without a team.
 */
@Service
@RequiredArgsConstructor
public class RequirementAccessService {

    private final CurrentUserService currentUserService;

    public Specification<Requirement> requirementVisibility() {
        User user = currentUserService.get();
        return switch (user.getRole()) {
            case SUPER_ADMIN, ADMIN -> null;
            case MANAGER -> {
                UUID teamId = user.getTeam() != null ? user.getTeam().getId() : null;
                if (teamId == null) {
                    yield (root, query, cb) -> cb.disjunction();
                }
                yield (root, query, cb) -> cb.equal(root.get("lead").get("owner").get("team").get("id"), teamId);
            }
            case COUNSELOR, VIEWER -> {
                UUID userId = user.getId();
                yield (root, query, cb) -> cb.equal(root.get("lead").get("owner").get("id"), userId);
            }
        };
    }
}
