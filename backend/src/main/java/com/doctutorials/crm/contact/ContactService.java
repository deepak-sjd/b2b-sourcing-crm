package com.doctutorials.crm.contact;

import com.doctutorials.crm.audit.AuditAction;
import com.doctutorials.crm.audit.AuditService;
import com.doctutorials.crm.common.exception.ResourceNotFoundException;
import com.doctutorials.crm.contact.dto.ContactRequest;
import com.doctutorials.crm.contact.dto.ContactStatsResponse;
import com.doctutorials.crm.contact.dto.DuplicateCheckResponse;
import com.doctutorials.crm.contact.dto.ContactResponse;
import com.doctutorials.crm.security.CurrentUserService;
import com.doctutorials.crm.user.Role;
import com.doctutorials.crm.user.User;
import com.doctutorials.crm.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ContactService {

    private final ContactRepository contactRepository;
    private final UserRepository userRepository;
    private final ContactEngagementService engagementService;
    private final AuditService auditService;
    private final ContactAccessService contactAccessService;
    private final CurrentUserService currentUserService;

    @Transactional
    public Contact create(ContactRequest req) {
        Contact contact = new Contact();
        contact.setCreatedBy(currentUserService.get());
        applyRequest(contact, req);
        engagementService.recompute(contact);

        Contact saved = contactRepository.save(contact);
        auditService.log("Contact", saved.getId(), AuditAction.CREATE,
                "Contact created: " + saved.getFullName());
        return saved;
    }

    @Transactional
    public Contact update(UUID id, ContactRequest req) {
        Contact contact = getById(id);
        applyRequest(contact, req);
        engagementService.recompute(contact);

        Contact saved = contactRepository.save(contact);
        auditService.log("Contact", saved.getId(), AuditAction.UPDATE, "Contact details updated");
        return saved;
    }

    @Transactional
    public Contact assignOwner(UUID id, UUID ownerId) {
        Contact contact = getById(id);
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> ResourceNotFoundException.of("User", ownerId));
        contactAccessService.assertCanAssignOwner(currentUserService.get(), owner);
        contact.setOwner(owner);
        Contact saved = contactRepository.save(contact);
        auditService.log("Contact", id, AuditAction.UPDATE, "Reassigned to " + owner.getFullName());
        return saved;
    }

    /** Records a touchpoint — called explicitly ("Log activity") and automatically
     *  whenever the UI's call/WhatsApp/email quick-actions are used, so
     *  "last activity" and the engagement score stay current without extra clicks. */
    @Transactional
    public Contact recordTouchpoint(UUID id) {
        Contact contact = getById(id);
        engagementService.markTouched(contact);
        return contactRepository.save(contact);
    }

    @Transactional
    public void archive(UUID id) {
        Contact contact = getById(id);
        contact.setArchived(true);
        contactRepository.save(contact);
        auditService.log("Contact", id, AuditAction.DELETE, "Contact archived");
    }

    public Contact getById(UUID id) {
        Contact contact = contactRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Contact", id));
        contactAccessService.assertVisible(contact);
        return contact;
    }

    private Specification<Contact> notArchived() {
        return (root, query, cb) -> cb.isFalse(root.get("archived"));
    }

    private Specification<Contact> matchesSearch(String search) {
        String q = "%" + search.trim().toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("fullName")), q),
                cb.like(cb.lower(cb.coalesce(root.get("company"), "")), q),
                cb.like(cb.lower(cb.coalesce(root.get("email"), "")), q),
                cb.like(cb.lower(cb.coalesce(root.get("phone"), "")), q)
        );
    }

    public Page<Contact> list(String search, ContactEngagement engagement, UUID ownerId, Pageable pageable) {
        Specification<Contact> spec = Specification.where(contactAccessService.contactVisibility())
                .and(notArchived());

        if (StringUtils.hasText(search)) {
            spec = spec.and(matchesSearch(search));
        }
        if (engagement != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("engagement"), engagement));
        }
        if (ownerId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("owner").get("id"), ownerId));
        }
        return contactRepository.findAll(spec, pageable);
    }

    /** Powers the KPI strip atop the Contacts page — scoped to what the caller can see. */
    public ContactStatsResponse stats() {
        Specification<Contact> base = Specification.where(contactAccessService.contactVisibility())
                .and(notArchived());

        long total = contactRepository.count(base);
        long active = contactRepository.count(base.and(engagementIs(ContactEngagement.ACTIVE)));
        long cooling = contactRepository.count(base.and(engagementIs(ContactEngagement.COOLING)));
        long atRisk = contactRepository.count(base.and(engagementIs(ContactEngagement.AT_RISK)));
        long bounced = contactRepository.count(base.and((root, query, cb) -> cb.isTrue(root.get("emailBounced"))));
        long unassigned = contactRepository.count(base.and((root, query, cb) -> cb.isNull(root.get("owner"))));

        return new ContactStatsResponse(total, active, cooling, atRisk, bounced, unassigned);
    }

    private Specification<Contact> engagementIs(ContactEngagement engagement) {
        return (root, query, cb) -> cb.equal(root.get("engagement"), engagement);
    }

    /** Lightweight AI-assist: warns the counselor before they create a likely
     *  duplicate, rather than silently letting the same person be added twice
     *  under two different owners. Checked by phone first (higher signal for
     *  this business than a shared inbox email), then email. */
    public DuplicateCheckResponse checkDuplicate(String phone, String email, UUID excludeId) {
        if (StringUtils.hasText(phone)) {
            var match = contactRepository.findFirstByPhoneAndArchivedFalse(phone.trim());
            if (match.isPresent() && !match.get().getId().equals(excludeId)) {
                return DuplicateCheckResponse.of(ContactResponse.from(match.get()), "phone");
            }
        }
        if (StringUtils.hasText(email)) {
            var match = contactRepository.findFirstByEmailIgnoreCaseAndArchivedFalse(email.trim());
            if (match.isPresent() && !match.get().getId().equals(excludeId)) {
                return DuplicateCheckResponse.of(ContactResponse.from(match.get()), "email");
            }
        }
        return DuplicateCheckResponse.none();
    }

    private void applyRequest(Contact contact, ContactRequest req) {
        contact.setFullName(req.fullName());
        contact.setDesignation(req.designation());
        contact.setCompany(req.company());
        contact.setPhone(req.phone());
        contact.setEmail(req.email());
        contact.setCity(req.city());
        contact.setSource(req.source());
        contact.setEmailBounced(req.emailBounced());

        User currentUser = currentUserService.get();

        if (req.ownerId() != null) {
            User owner = userRepository.findById(req.ownerId())
                    .orElseThrow(() -> ResourceNotFoundException.of("User", req.ownerId()));
            contactAccessService.assertCanAssignOwner(currentUser, owner);
            contact.setOwner(owner);
        } else if (contact.getOwner() == null && currentUser.getRole() == Role.COUNSELOR) {
            // Same default as LeadService.applyRequest: a counselor creating a
            // contact with no owner specified becomes its owner automatically.
            contact.setOwner(currentUser);
        }
    }
}
