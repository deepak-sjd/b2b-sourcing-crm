package com.doctutorials.crm.quotation;

import com.doctutorials.crm.common.ApiResponse;
import com.doctutorials.crm.common.PageResponse;
import com.doctutorials.crm.quotation.dto.QuotationPrefillResponse;
import com.doctutorials.crm.quotation.dto.QuotationRequest;
import com.doctutorials.crm.quotation.dto.QuotationResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/quotations")
@RequiredArgsConstructor
public class QuotationController {

    private final QuotationService quotationService;

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','MANAGER','COUNSELOR')")
    public ResponseEntity<ApiResponse<QuotationResponse>> create(@Valid @RequestBody QuotationRequest request) {
        Quotation quotation = quotationService.create(request);
        return ResponseEntity.ok(ApiResponse.ok("Quotation created", QuotationResponse.from(quotation)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<QuotationResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(QuotationResponse.from(quotationService.getById(id))));
    }

    // Same "always sort explicitly" reasoning as LeadController/CustomerController.list.
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<QuotationResponse>>> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) UUID customerId,
            @RequestParam(required = false) QuotationStatus status,
            @PageableDefault(sort = { "createdAt", "id" }, direction = Sort.Direction.DESC) Pageable pageable) {
        Page<Quotation> page = quotationService.list(customerId, status, q, pageable);
        return ResponseEntity.ok(ApiResponse.ok(new PageResponse<>(page.map(QuotationResponse::from))));
    }

    /** Backs the "Quotation history" tab on CustomerDetailPage. */
    @GetMapping("/by-customer/{customerId}")
    public ResponseEntity<ApiResponse<PageResponse<QuotationResponse>>> byCustomer(
            @PathVariable UUID customerId,
            @PageableDefault(sort = { "createdAt", "id" }, direction = Sort.Direction.DESC) Pageable pageable) {
        Page<Quotation> page = quotationService.byCustomer(customerId, pageable);
        return ResponseEntity.ok(ApiResponse.ok(new PageResponse<>(page.map(QuotationResponse::from))));
    }

    /** Backs LeadQuotationsTab - quotations prepared directly against an active lead. */
    @GetMapping("/by-lead/{leadId}")
    public ResponseEntity<ApiResponse<PageResponse<QuotationResponse>>> byLead(
            @PathVariable UUID leadId,
            @PageableDefault(sort = { "createdAt", "id" }, direction = Sort.Direction.DESC) Pageable pageable) {
        Page<Quotation> page = quotationService.byLead(leadId, pageable);
        return ResponseEntity.ok(ApiResponse.ok(new PageResponse<>(page.map(QuotationResponse::from))));
    }

    /** Backs "Start from requirement" in the builder - 404 (with a clear message) if
     *  no supplier reply has been chosen for this requirement yet. */
    @GetMapping("/prefill")
    public ResponseEntity<ApiResponse<QuotationPrefillResponse>> prefill(@RequestParam UUID requirementId) {
        return ResponseEntity.ok(ApiResponse.ok(quotationService.prefill(requirementId)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','MANAGER','COUNSELOR')")
    public ResponseEntity<ApiResponse<QuotationResponse>> update(@PathVariable UUID id,
            @Valid @RequestBody QuotationRequest request) {
        Quotation quotation = quotationService.update(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Quotation updated", QuotationResponse.from(quotation)));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','MANAGER','COUNSELOR')")
    public ResponseEntity<ApiResponse<QuotationResponse>> changeStatus(@PathVariable UUID id,
            @RequestParam QuotationStatus status) {
        Quotation quotation = quotationService.changeStatus(id, status);
        return ResponseEntity.ok(ApiResponse.ok("Status updated", QuotationResponse.from(quotation)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        quotationService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Quotation deleted", null));
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<QuotationService.QuotationSummary>> summary() {
        return ResponseEntity.ok(ApiResponse.ok(quotationService.summary()));
    }
}
