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
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * No @PreAuthorize role gate on these - same as TaskController/notes/documents.
 * Authorization is "can you see the lead this enquiry belongs to", enforced
 * inside SupplierEnquiryService via LeadService.getById, not a role check
 * here; anyone who can work a lead can source for it.
 */
@RestController
@RequestMapping("/api/v1/supplier-enquiries")
@RequiredArgsConstructor
public class SupplierEnquiryController {

    private final SupplierEnquiryService enquiryService;

    @PostMapping
    public ResponseEntity<ApiResponse<SupplierEnquiryResponse>> create(@Valid @RequestBody SupplierEnquiryRequest req) {
        SupplierEnquiry enquiry = enquiryService.create(req);
        return ResponseEntity.ok(ApiResponse.ok("Enquiry sent", SupplierEnquiryResponse.from(enquiry)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SupplierEnquiryResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(SupplierEnquiryResponse.from(enquiryService.getById(id))));
    }

    @GetMapping("/by-lead/{leadId}")
    public ResponseEntity<ApiResponse<PageResponse<SupplierEnquiryResponse>>> byLead(
            @PathVariable UUID leadId,
            @PageableDefault(sort = { "createdAt", "id" }, direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(
                new PageResponse<>(enquiryService.listByLead(leadId, pageable).map(SupplierEnquiryResponse::from))));
    }

    /** Supplier-side history tab (PRD 12.1: "Supplier history is kept"). */
    @GetMapping("/by-supplier/{supplierId}")
    public ResponseEntity<ApiResponse<PageResponse<SupplierEnquiryReplyResponse>>> bySupplier(
            @PathVariable UUID supplierId,
            @PageableDefault(sort = { "createdAt", "id" }, direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(new PageResponse<>(
                enquiryService.listBySupplier(supplierId, pageable).map(SupplierEnquiryReplyResponse::from))));
    }

    @PatchMapping("/{enquiryId}/replies/{replyId}")
    public ResponseEntity<ApiResponse<SupplierEnquiryResponse>> recordReply(
            @PathVariable UUID enquiryId, @PathVariable UUID replyId,
            @RequestBody SupplierEnquiryReplyRequest req) {
        SupplierEnquiry enquiry = enquiryService.recordReply(enquiryId, replyId, req);
        return ResponseEntity.ok(ApiResponse.ok("Reply recorded", SupplierEnquiryResponse.from(enquiry)));
    }

    @PostMapping("/{enquiryId}/replies/{replyId}/choose")
    public ResponseEntity<ApiResponse<SupplierEnquiryResponse>> chooseReply(
            @PathVariable UUID enquiryId, @PathVariable UUID replyId) {
        SupplierEnquiry enquiry = enquiryService.chooseReply(enquiryId, replyId);
        return ResponseEntity.ok(ApiResponse.ok("Price reference chosen", SupplierEnquiryResponse.from(enquiry)));
    }
}
