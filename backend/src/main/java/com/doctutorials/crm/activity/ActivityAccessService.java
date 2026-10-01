package com.doctutorials.crm.activity;

import com.doctutorials.crm.security.CurrentUserService;
import com.doctutorials.crm.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.UUID;

/** Mirrors LeadAccessService/TaskAccessService, expressed over
 *  Activity.lead.owner. */
@Service
@RequiredArgsConstructor
public class ActivityAccessService {

    private final CurrentUserService currentUserService;

    public Specification<Activity> activityVisibility() {
        User user = currentUserService.get();
        return switch (user.getRole()) {
            case SUPER_ADMIN, ADMIN -> null;
            case MANAGER -> {
                UUID teamId = user.getTeam() != null ? user.getTeam().getId() : null;
                if (teamId == null) yield (root, query, cb) -> cb.disjunction();
                yield (root, query, cb) -> cb.equal(root.get("lead").get("owner").get("team").get("id"), teamId);
            }
            case COUNSELOR, VIEWER -> {
                UUID userId = user.getId();
                yield (root, query, cb) -> cb.equal(root.get("lead").get("owner").get("id"), userId);
            }
        };
    }
}