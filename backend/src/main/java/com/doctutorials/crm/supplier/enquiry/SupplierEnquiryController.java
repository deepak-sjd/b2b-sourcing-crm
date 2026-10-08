package com.doctutorials.crm.supplier.enquiry;

import com.doctutorials.crm.common.ApiResponse;
import com.doctutorials.crm.common.PageResponse;
import com.doctutorials.crm.supplier.enquiry.dto.SupplierEnquiryReplyRequest;
import com.doctutorials.crm.supplier.enquiry.dto.SupplierEnquiryReplyResponse;
import com.doctutorials.crm.supplier.enquiry.dto.SupplierEnquiryRequest;
import com.doctutorials.crm.supplier.enquiry.dto.SupplierEnquiryResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Reads are open to any authenticated role (incl. VIEWER) - visibility is
 * still enforced inside the service via the lead. Writes require at least
 * COUNSELOR; VIEWER can look but never send an enquiry, record a reply, or
 * pick a price. The service never returns a bare entity here - every method
 * already returns a DTO built inside its own transaction (see
 * SupplierEnquiryService's class comment), so this controller never touches
 * a lazy field directly.
 */
@RestController
@RequestMapping("/api/v1/supplier-enquiries")
@RequiredArgsConstructor
public class SupplierEnquiryController {

    private final SupplierEnquiryService enquiryService;

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','MANAGER','COUNSELOR')")
    public ResponseEntity<ApiResponse<SupplierEnquiryResponse>> create(@Valid @RequestBody SupplierEnquiryRequest req) {
        return ResponseEntity.ok(ApiResponse.ok("Enquiry sent", enquiryService.create(req)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SupplierEnquiryResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(enquiryService.getById(id)));
    }

    @GetMapping("/by-requirement/{requirementId}")
    public ResponseEntity<ApiResponse<List<SupplierEnquiryResponse>>> byRequirement(@PathVariable UUID requirementId) {
        return ResponseEntity.ok(ApiResponse.ok(enquiryService.listByRequirement(requirementId)));
    }

    @GetMapping("/by-lead/{leadId}")
    public ResponseEntity<ApiResponse<PageResponse<SupplierEnquiryResponse>>> byLead(
            @PathVariable UUID leadId,
            @PageableDefault(sort = { "createdAt", "id" }, direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(new PageResponse<>(enquiryService.listByLead(leadId, pageable))));
    }

    /** Supplier-side history tab (PRD 12.1: "Supplier history is kept"). */
    @GetMapping("/by-supplier/{supplierId}")
    public ResponseEntity<ApiResponse<PageResponse<SupplierEnquiryReplyResponse>>> bySupplier(
            @PathVariable UUID supplierId,
            @PageableDefault(sort = { "createdAt", "id" }, direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(new PageResponse<>(enquiryService.listBySupplier(supplierId, pageable))));
    }

    @PatchMapping("/{enquiryId}/replies/{replyId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','MANAGER','COUNSELOR')")
    public ResponseEntity<ApiResponse<SupplierEnquiryResponse>> recordReply(
            @PathVariable UUID enquiryId, @PathVariable UUID replyId,
            @Valid @RequestBody SupplierEnquiryReplyRequest req) {
        return ResponseEntity.ok(ApiResponse.ok("Reply recorded", enquiryService.recordReply(enquiryId, replyId, req)));
    }

    @PostMapping("/{enquiryId}/replies/{replyId}/choose")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','MANAGER','COUNSELOR')")
    public ResponseEntity<ApiResponse<SupplierEnquiryResponse>> chooseReply(
            @PathVariable UUID enquiryId, @PathVariable UUID replyId) {
        return ResponseEntity.ok(ApiResponse.ok("Price reference chosen", enquiryService.chooseReply(enquiryId, replyId)));
    }

    @DeleteMapping("/{enquiryId}/replies/{replyId}/choose")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','MANAGER','COUNSELOR')")
    public ResponseEntity<ApiResponse<SupplierEnquiryResponse>> unchooseReply(
            @PathVariable UUID enquiryId, @PathVariable UUID replyId) {
        return ResponseEntity.ok(ApiResponse.ok("Price reference cleared", enquiryService.unchooseReply(enquiryId, replyId)));
    }

    @PostMapping("/{enquiryId}/replies/{replyId}/resend")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','MANAGER','COUNSELOR')")
    public ResponseEntity<ApiResponse<SupplierEnquiryReplyResponse>> resend(
            @PathVariable UUID enquiryId, @PathVariable UUID replyId) {
        return ResponseEntity.ok(ApiResponse.ok("Enquiry resent", enquiryService.resend(enquiryId, replyId)));
    }
}
