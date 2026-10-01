package com.doctutorials.crm.task;

import com.doctutorials.crm.security.CurrentUserService;
import com.doctutorials.crm.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Mirrors LeadAccessService's rule but expressed over Task.lead.owner,
 * since a task is only ever visible to the same audience as its parent
 * lead. Kept as a small separate service (rather than living in
 * LeadAccessService) so the "lead" package never needs to depend on
 * "task" - task already depends on lead, and a dependency the other way
 * would create a package cycle.
 */
@Service
@RequiredArgsConstructor
public class TaskAccessService {

    private final CurrentUserService currentUserService;

    public Specification<Task> taskVisibility() {
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
