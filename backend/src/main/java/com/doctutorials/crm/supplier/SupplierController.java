package com.doctutorials.crm.supplier;

import com.doctutorials.crm.common.ApiResponse;
import com.doctutorials.crm.common.PageResponse;
import com.doctutorials.crm.supplier.dto.DuplicateCheckRequest;
import com.doctutorials.crm.supplier.dto.GstLookupResponse;
import com.doctutorials.crm.supplier.dto.SupplierRequest;
import com.doctutorials.crm.supplier.dto.SupplierResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/suppliers")
@RequiredArgsConstructor
public class SupplierController {

    private final SupplierService supplierService;

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<SupplierResponse>> create(@Valid @RequestBody SupplierRequest request) {
        Supplier supplier = supplierService.create(request);
        return ResponseEntity.ok(ApiResponse.ok("Supplier created", SupplierResponse.from(supplier)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SupplierResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(SupplierResponse.from(supplierService.getById(id))));
    }

    // Same "always sort explicitly" reasoning as LeadController - see its list() comment.
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<SupplierResponse>>> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) SupplierStatus status,
            @RequestParam(required = false) SupplierCategory category,
            @PageableDefault(sort = { "createdAt", "id" }, direction = Sort.Direction.DESC) Pageable pageable) {

        Page<Supplier> page = supplierService.list(q, status, category, pageable);
        Page<SupplierResponse> mapped = page.map(SupplierResponse::from);
        return ResponseEntity.ok(ApiResponse.ok(new PageResponse<>(mapped)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<SupplierResponse>> update(@PathVariable UUID id,
            @Valid @RequestBody SupplierRequest request) {
        Supplier supplier = supplierService.update(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Supplier updated", SupplierResponse.from(supplier)));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<SupplierResponse>> changeStatus(@PathVariable UUID id,
            @RequestParam SupplierStatus status) {
        Supplier supplier = supplierService.changeStatus(id, status);
        return ResponseEntity.ok(ApiResponse.ok("Status updated", SupplierResponse.from(supplier)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<ApiResponse<Void>> archive(@PathVariable UUID id) {
        supplierService.archive(id);
        return ResponseEntity.ok(ApiResponse.ok("Supplier archived", null));
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<SupplierService.SupplierSummary>> summary() {
        return ResponseEntity.ok(ApiResponse.ok(supplierService.summary()));
    }

    /** Powers live "state auto-filled from GST" + "this GSTIN looks invalid" feedback as the user types. */
    @GetMapping("/gst-lookup")
    public ResponseEntity<ApiResponse<GstLookupResponse>> gstLookup(@RequestParam String gstin) {
        GstUtil.GstDetails d = supplierService.lookupGst(gstin);
        return ResponseEntity.ok(ApiResponse.ok(
                new GstLookupResponse(d.structureValid(), d.checksumValid(), d.stateCode(), d.stateName(), d.panNumber())));
    }

    /** Called on blur from the form (name/GST/phone) before save, to surface a "possible duplicate" warning. */
    @PostMapping("/check-duplicate")
    public ResponseEntity<ApiResponse<List<SupplierDuplicateService.DuplicateMatch>>> checkDuplicate(
            @RequestBody DuplicateCheckRequest request) {
        List<SupplierDuplicateService.DuplicateMatch> matches = supplierService.checkDuplicates(
                request.companyName(), request.gstNumber(), request.contactPhone(), request.excludeId());
        return ResponseEntity.ok(ApiResponse.ok(matches));
    }
}
