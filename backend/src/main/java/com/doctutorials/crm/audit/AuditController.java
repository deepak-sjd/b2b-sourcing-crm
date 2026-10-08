package com.doctutorials.crm.audit;

import com.doctutorials.crm.audit.dto.AuditTimelineEntry;
import com.doctutorials.crm.common.ApiResponse;
import com.doctutorials.crm.common.PageResponse;
import com.doctutorials.crm.customer.CustomerService;
import com.doctutorials.crm.lead.LeadService;
import com.doctutorials.crm.security.CurrentUserService;
import com.doctutorials.crm.user.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/audit-logs")
@RequiredArgsConstructor
public class AuditController {

    private final AuditService auditService;
    private final LeadService leadService;
    private final CustomerService customerService;
    private final CurrentUserService currentUserService;

    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AuditLog>>> list(
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) AuditAction action,
            @RequestParam(required = false) UUID actorUserId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @PageableDefault(sort = { "createdAt", "id" }, direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(new PageResponse<>(
                auditService.listAll(entityType, action, actorUserId, from, to, pageable))));
    }

    @GetMapping("/by-entity/{entityType}/{entityId}")
    public ResponseEntity<ApiResponse<PageResponse<AuditLog>>> byEntity(
            @PathVariable String entityType, @PathVariable UUID entityId, Pageable pageable) {
        assertEntityVisible(entityType, entityId);
        return ResponseEntity.ok(ApiResponse.ok(new PageResponse<>(auditService.listByEntity(entityType, entityId, pageable))));
    }

    /** Backs LeadDetailPage's Timeline tab - see Task A item 4/8. */
    @GetMapping("/timeline/lead/{leadId}")
    public ResponseEntity<ApiResponse<PageResponse<AuditTimelineEntry>>> leadTimeline(
            @PathVariable UUID leadId,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @PageableDefault(size = 30) Pageable pageable) {
        leadService.getById(leadId); // 404s if this lead isn't visible to the caller
        var page = auditService.leadTimeline(leadId, entityType, from, to, pageable)
                .map(AuditTimelineEntry::from);
        return ResponseEntity.ok(ApiResponse.ok(new PageResponse<>(page)));
    }

    private void assertEntityVisible(String entityType, UUID entityId) {
        Role role = currentUserService.get().getRole();
        boolean isAdmin = role == Role.SUPER_ADMIN || role == Role.ADMIN;
        if (isAdmin) return;

        if ("Lead".equalsIgnoreCase(entityType)) {
            leadService.getById(entityId);
        } else if ("Customer".equalsIgnoreCase(entityType)) {
            customerService.getById(entityId);
        } else {
            throw new AccessDeniedException("Only admins can view audit logs for this entity type");
        }
    }
}