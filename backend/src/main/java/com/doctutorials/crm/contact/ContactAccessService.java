package com.doctutorials.crm.contact;

import com.doctutorials.crm.common.exception.BadRequestException;
import com.doctutorials.crm.common.exception.ResourceNotFoundException;
import com.doctutorials.crm.security.CurrentUserService;
import com.doctutorials.crm.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Same visibility model as lead.LeadAccessService, applied to Contact:
 * SUPER_ADMIN, ADMIN     -> everything (org-wide).
 * MANAGER                -> contacts owned by any user on the manager's team.
 * COUNSELOR, VIEWER      -> contacts owned by the user themself.
 * An unowned contact is only visible to SUPER_ADMIN/ADMIN.
 */
@Service
@RequiredArgsConstructor
public class ContactAccessService {

    private final CurrentUserService currentUserService;

    public Specification<Contact> contactVisibility() {
        return visibilityFor(currentUserService.get());
    }

    private Specification<Contact> visibilityFor(User user) {
        return switch (user.getRole()) {
            case SUPER_ADMIN, ADMIN -> null;
            case MANAGER -> {
                UUID teamId = user.getTeam() != null ? user.getTeam().getId() : null;
                if (teamId == null) {
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

    public void assertVisible(Contact contact) {
        if (!isVisible(contact, currentUserService.get())) {
            throw ResourceNotFoundException.of("Contact", contact.getId());
        }
    }

    public boolean isVisible(Contact contact, User user) {
        return switch (user.getRole()) {
            case SUPER_ADMIN, ADMIN -> true;
            case MANAGER -> contact.getOwner() != null
                    && contact.getOwner().getTeam() != null
                    && user.getTeam() != null
                    && contact.getOwner().getTeam().getId().equals(user.getTeam().getId());
            case COUNSELOR, VIEWER -> contact.getOwner() != null && contact.getOwner().getId().equals(user.getId());
        };
    }

    public void assertCanAssignOwner(User currentUser, User requestedOwner) {
        switch (currentUser.getRole()) {
            case SUPER_ADMIN, ADMIN -> {
                /* no restriction */ }
            case MANAGER -> {
                if (currentUser.getTeam() == null || requestedOwner.getTeam() == null
                        || !currentUser.getTeam().getId().equals(requestedOwner.getTeam().getId())) {
                    throw new BadRequestException("Cannot assign a contact to a user outside your team");
                }
            }
            case COUNSELOR, VIEWER -> {
                if (!requestedOwner.getId().equals(currentUser.getId())) {
                    throw new BadRequestException("You can only own contacts assigned to yourself");
                }
            }
        }
    }
}
