package com.doctutorials.crm.task;

import com.doctutorials.crm.common.exception.BadRequestException;
import com.doctutorials.crm.common.exception.ResourceNotFoundException;
import com.doctutorials.crm.customer.Customer;
import com.doctutorials.crm.lead.Lead;
import com.doctutorials.crm.security.CurrentUserService;
import com.doctutorials.crm.user.User;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Who can see / edit a task. A task is "theirs" when they are its assignee, the owner of its
 * lead, or the account manager of its customer. Managers also get their team's tasks;
 * admins everything; VIEWER is read-only. TEMPORARY: D2 will replace the role checks with the
 * shared permission policy - keep this class as the single place to swap.
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
                    yield (root, query, cb) -> cb.disjunction(); // fail closed
                }
                yield (root, query, cb) -> {
                    Join<Task, User> assignee = root.join("assignedTo", JoinType.LEFT);
                    Join<Task, Lead> lead = root.join("lead", JoinType.LEFT);
                    Join<Lead, User> leadOwner = lead.join("owner", JoinType.LEFT);
                    Join<Task, Customer> customer = root.join("customer", JoinType.LEFT);
                    Join<Customer, User> manager = customer.join("accountManager", JoinType.LEFT);
                    return cb.or(
                            cb.equal(assignee.get("team").get("id"), teamId),
                            cb.equal(leadOwner.get("team").get("id"), teamId),
                            cb.equal(manager.get("team").get("id"), teamId),
                            cb.equal(assignee.get("id"), user.getId()));
                };
            }
            case COUNSELOR, VIEWER -> {
                UUID userId = user.getId();
                yield (root, query, cb) -> {
                    Join<Task, User> assignee = root.join("assignedTo", JoinType.LEFT);
                    Join<Task, Lead> lead = root.join("lead", JoinType.LEFT);
                    Join<Lead, User> leadOwner = lead.join("owner", JoinType.LEFT);
                    Join<Task, Customer> customer = root.join("customer", JoinType.LEFT);
                    Join<Customer, User> manager = customer.join("accountManager", JoinType.LEFT);
                    return cb.or(
                            cb.equal(assignee.get("id"), userId),
                            cb.equal(leadOwner.get("id"), userId),
                            cb.equal(manager.get("id"), userId));
                };
            }
        };
    }

    /** 404 (not 403) for a task the caller can't see, same convention as LeadAccessService. */
    public void assertCanView(Task task) {
        if (!canView(task, currentUserService.get())) {
            throw ResourceNotFoundException.of("Task", task.getId());
        }
    }

    public void assertCanEdit(Task task) {
        if (!canEdit(task, currentUserService.get())) {
            throw new AccessDeniedException("You can't modify this task");
        }
    }

    public boolean canView(Task task, User user) {
        return switch (user.getRole()) {
            case SUPER_ADMIN, ADMIN -> true;
            case MANAGER -> touches(task, user) || sameTeam(task, user);
            case COUNSELOR, VIEWER -> touches(task, user);
        };
    }

    public boolean canEdit(Task task, User user) {
        return switch (user.getRole()) {
            case SUPER_ADMIN, ADMIN -> true;
            case MANAGER -> touches(task, user) || sameTeam(task, user);
            case COUNSELOR -> touches(task, user);
            case VIEWER -> false;
        };
    }

    /** Who may be given a task: admin anyone, manager their own team, everyone else only themselves. */
    public void assertCanAssign(User actor, User assignee) {
        switch (actor.getRole()) {
            case SUPER_ADMIN, ADMIN -> {
            }
            case MANAGER -> {
                if (actor.getTeam() == null || assignee.getTeam() == null
                        || !actor.getTeam().getId().equals(assignee.getTeam().getId())) {
                    throw new BadRequestException("You can only assign tasks to people on your own team");
                }
            }
            case COUNSELOR, VIEWER -> {
                if (!assignee.getId().equals(actor.getId())) {
                    throw new BadRequestException("You can only assign tasks to yourself");
                }
            }
        }
    }

    private static boolean touches(Task t, User u) {
        return isUser(t.getAssignedTo(), u)
                || (t.getLead() != null && isUser(t.getLead().getOwner(), u))
                || (t.getCustomer() != null && isUser(t.getCustomer().getAccountManager(), u));
    }

    private static boolean sameTeam(Task t, User u) {
        if (u.getTeam() == null) return false;
        UUID team = u.getTeam().getId();
        return teamOf(t.getAssignedTo(), team)
                || (t.getLead() != null && teamOf(t.getLead().getOwner(), team))
                || (t.getCustomer() != null && teamOf(t.getCustomer().getAccountManager(), team));
    }

    private static boolean isUser(User candidate, User u) {
        return candidate != null && candidate.getId().equals(u.getId());
    }

    private static boolean teamOf(User candidate, UUID teamId) {
        return candidate != null && candidate.getTeam() != null && candidate.getTeam().getId().equals(teamId);
    }
}
