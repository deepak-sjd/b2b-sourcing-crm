package com.doctutorials.crm.audit;

import com.doctutorials.crm.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Fire-and-forget audit writer. Every mutating service call (create/update/delete/stage change)
 * should call this so the "Audit Log" tab has a real trail - this is what the screenshots show
 * under Admin > Audit Log and per-lead "Audit Logs" tab.
 */
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public void log(String entityType, UUID entityId, AuditAction action, String description) {
        AuditLog entry = new AuditLog();
        entry.setEntityType(entityType);
        entry.setEntityId(entityId);
        entry.setAction(action);
        entry.setDescription(description);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User user) {
            entry.setActorUserId(user.getId());
            entry.setActorName(user.getFullName());
        }

        auditLogRepository.save(entry);
    }

    /**
     * Global audit trail (Admin > Audit Log). Deliberately not visibility-scoped
     * the way Lead/Task/Call are - it's gated to SUPER_ADMIN/ADMIN entirely at
     * the controller instead (see AuditController), since "who changed what,
     * org-wide" is inherently an admin-only view, not something to filter down
     * by team/ownership the way a MANAGER's lead list is.
     */
    @Transactional(readOnly = true)
    public Page<AuditLog> listAll(String entityType, Pageable pageable) {
        return entityType != null
                ? auditLogRepository.findByEntityType(entityType, pageable)
                : auditLogRepository.findAll(pageable);
    }

    /**
     * Per-entity audit trail (e.g. a lead's "Audit Logs" tab). Access control is
     * the caller's job (see AuditController) - this method itself only knows how
     * to look rows up, not whether the caller is allowed to see the entity they
     * describe.
     */
    @Transactional(readOnly = true)
    public Page<AuditLog> listByEntity(String entityType, UUID entityId, Pageable pageable) {
        return auditLogRepository.findByEntityTypeAndEntityId(entityType, entityId, pageable);
    }
}
