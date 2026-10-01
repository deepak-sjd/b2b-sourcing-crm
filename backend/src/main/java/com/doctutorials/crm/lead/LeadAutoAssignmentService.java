package com.doctutorials.crm.lead;

import com.doctutorials.crm.user.Role;
import com.doctutorials.crm.user.User;
import com.doctutorials.crm.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

/**
 * Auto-assigns a newly created, unowned lead to the least-loaded eligible
 * counselor, instead of leaving it to sit unassigned in triage.
 *
 * "Least-loaded" = fewest currently-open leads (not archived, not WON/LOST) —
 * a simple, explainable load-balancing rule, deliberately not a black box.
 * Ties break on user id for determinism (stable, repeatable assignment in
 * tests and audits — not "whoever JPA happened to return first").
 *
 * Scope, matching LeadAccessService's existing visibility rules:
 *  - MANAGER-created lead with no ownerId -> least-loaded COUNSELOR on that
 *    manager's own team.
 *  - ADMIN/SUPER_ADMIN-created lead with no ownerId -> least-loaded COUNSELOR
 *    across the creator's organization.
 *  - A COUNSELOR creating a lead already self-assigns in LeadService — this
 *    class is never consulted for that case.
 *  If no eligible counselor exists (e.g. a manager with an empty team), the
 *  lead is left unassigned rather than guessing — same fail-closed posture as
 *  LeadAccessService uses for a team-less manager.
 */
@Service
@RequiredArgsConstructor
public class LeadAutoAssignmentService {

    private final UserRepository userRepository;
    private final LeadRepository leadRepository;

    public User pickOwnerFor(User creator) {
        List<User> candidates = switch (creator.getRole()) {
            case MANAGER -> creator.getTeam() == null
                    ? List.<User>of()
                    : userRepository.findByRoleAndTeamIdAndActiveTrue(Role.COUNSELOR, creator.getTeam().getId());
            case SUPER_ADMIN, ADMIN -> userRepository.findByRoleAndOrganizationIdAndActiveTrue(
                    Role.COUNSELOR, creator.getOrganization().getId());
            default -> List.<User>of();
        };

        if (candidates.isEmpty()) return null;

        return candidates.stream()
                .min(Comparator.<User>comparingLong(this::openLeadCount).thenComparing(User::getId))
                .orElse(null);
    }

    private long openLeadCount(User owner) {
        Specification<Lead> spec = (root, query, cb) -> cb.and(
                cb.equal(root.get("owner").get("id"), owner.getId()),
                cb.isFalse(root.get("archived")),
                cb.not(root.get("stage").in(List.of(LeadStage.WON, LeadStage.LOST))));
        return leadRepository.count(spec);
    }
}
