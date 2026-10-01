package com.doctutorials.crm.customer;

import com.doctutorials.crm.common.ApiResponse;
import com.doctutorials.crm.common.PageResponse;
import com.doctutorials.crm.customer.dto.CustomerRequest;
import com.doctutorials.crm.customer.dto.CustomerResponse;
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
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;
    private final CustomerDuplicateService duplicateService;
    private final GstStateResolver gstStateResolver;
    private final AccountManagerSuggestionService suggestionService;

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','MANAGER','COUNSELOR')")
    public ResponseEntity<ApiResponse<CustomerResponse>> create(@Valid @RequestBody CustomerRequest request) {
        Customer customer = customerService.create(request);
        return ResponseEntity.ok(ApiResponse.ok("Customer created", CustomerResponse.from(customer)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(CustomerResponse.from(customerService.getById(id))));
    }

    // Same "why explicit ORDER BY matters" reasoning as LeadController.list —
    // see that method's javadoc.
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<CustomerResponse>>> list(
            @RequestParam(required = false) ApprovalStatus approvalStatus,
            @RequestParam(required = false) UUID accountManagerId,
            @PageableDefault(sort = { "createdAt", "id" }, direction = Sort.Direction.DESC) Pageable pageable) {
        Page<Customer> page = customerService.list(approvalStatus, accountManagerId, pageable);
        return ResponseEntity.ok(ApiResponse.ok(new PageResponse<>(page.map(CustomerResponse::from))));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','MANAGER','COUNSELOR')")
    public ResponseEntity<ApiResponse<CustomerResponse>> update(
            @PathVariable UUID id, @Valid @RequestBody CustomerRequest request) {
        Customer customer = customerService.update(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Customer updated", CustomerResponse.from(customer)));
    }

    @PatchMapping("/{id}/approval-status")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<CustomerResponse>> changeApprovalStatus(
            @PathVariable UUID id, @RequestParam ApprovalStatus status) {
        Customer customer = customerService.changeApprovalStatus(id, status);
        return ResponseEntity.ok(ApiResponse.ok("Approval status updated", CustomerResponse.from(customer)));
    }

    @PatchMapping("/{id}/account-manager")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<CustomerResponse>> assignAccountManager(
            @PathVariable UUID id, @RequestParam UUID accountManagerId) {
        Customer customer = customerService.assignAccountManager(id, accountManagerId);
        return ResponseEntity.ok(ApiResponse.ok("Account manager reassigned", CustomerResponse.from(customer)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<ApiResponse<Void>> archive(@PathVariable UUID id) {
        customerService.archive(id);
        return ResponseEntity.ok(ApiResponse.ok("Customer archived", null));
    }

    // --- AI automation endpoints ---

    /** Powers the duplicate-warning banner in the create/edit form. */
    @GetMapping("/duplicate-check")
    public ResponseEntity<ApiResponse<List<CustomerDuplicateService.DuplicateMatch>>> checkDuplicate(
            @RequestParam String companyName,
            @RequestParam(required = false) String gstNo,
            @RequestParam(required = false) UUID excludingId) {
        return ResponseEntity.ok(ApiResponse.ok(duplicateService.findPossibleDuplicates(companyName, gstNo, excludingId)));
    }

    /** Powers the "detected state: ..." hint under the GST No. field. */
    @GetMapping("/gst-lookup")
    public ResponseEntity<ApiResponse<GstStateResolver.GstInfo>> gstLookup(@RequestParam String gstNo) {
        return ResponseEntity.ok(ApiResponse.ok(gstStateResolver.resolve(gstNo)));
    }

    /** Powers the "Suggest: <name>" chip next to the Account Manager field. */
    @GetMapping("/suggest-account-manager")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<AccountManagerSuggestionService.Suggestion>> suggestAccountManager() {
        return ResponseEntity.ok(ApiResponse.ok(suggestionService.suggest().orElse(null)));
    }
}
