package com.doctutorials.crm.audit;

import com.doctutorials.crm.common.ApiResponse;
import com.doctutorials.crm.common.PageResponse;
import com.doctutorials.crm.customer.CustomerService;
import com.doctutorials.crm.lead.LeadService;
import com.doctutorials.crm.security.CurrentUserService;
import com.doctutorials.crm.user.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * The data has existed since day one (AuditService writes on every
 * create/update/delete/stage-change) - this controller is just the missing
 * read side, backing both the Admin > Audit Log screen and each
 * lead's/customer's "Audit Logs" tab.
 *
 * CHANGE from the original version: byEntity used to accept only "Lead" for
 * non-admins. It now also accepts "Customer", resolved through
 * CustomerService.getById the same way — see that method's javadoc below.
 */
@RestController
@RequestMapping("/api/v1/audit-logs")
@RequiredArgsConstructor
public class AuditController {

    private final AuditService auditService;
    private final LeadService leadService;
    private final CustomerService customerService;
    private final CurrentUserService currentUserService;

    /** Org-wide trail. Admin-only by design - see AuditService.listAll javadoc. */
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AuditLog>>> list(
            @RequestParam(required = false) String entityType,
            @PageableDefault(sort = { "createdAt", "id" }, direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(new PageResponse<>(auditService.listAll(entityType, pageable))));
    }

    /**
     * Per-entity trail (e.g. a lead's or customer's "Audit Logs" tab). Admins
     * can look up any entity; anyone else may only look up a "Lead" or
     * "Customer" they can already see - leadService.getById /
     * customerService.getById throw 404 for a record outside their
     * visibility, which doubles as the access check here. Any other
     * entityType from a non-admin is rejected outright rather than silently
     * returning an empty page, so a caller can't probe for the existence of
     * entities they can't see.
     */
    @GetMapping("/by-entity/{entityType}/{entityId}")
    public ResponseEntity<ApiResponse<PageResponse<AuditLog>>> byEntity(
            @PathVariable String entityType,
            @PathVariable UUID entityId,
            Pageable pageable) {
        Role role = currentUserService.get().getRole();
        boolean isAdmin = role == Role.SUPER_ADMIN || role == Role.ADMIN;

        if (!isAdmin) {
            if ("Lead".equalsIgnoreCase(entityType)) {
                leadService.getById(entityId); // throws 404 if this lead isn't visible to the caller
            } else if ("Customer".equalsIgnoreCase(entityType)) {
                customerService.getById(entityId); // throws 404 if this customer isn't visible to the caller
            } else {
                throw new AccessDeniedException("Only admins can view audit logs for this entity type");
            }
        }

        return ResponseEntity.ok(ApiResponse.ok(new PageResponse<>(auditService.listByEntity(entityType, entityId, pageable))));
    }
}
