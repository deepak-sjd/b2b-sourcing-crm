package com.doctutorials.crm.requirement;

import com.doctutorials.crm.common.ApiResponse;
import com.doctutorials.crm.common.PageResponse;
import com.doctutorials.crm.lead.Priority;
import com.doctutorials.crm.requirement.dto.RequirementRequest;
import com.doctutorials.crm.requirement.dto.RequirementResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/requirements")
@RequiredArgsConstructor
public class RequirementController {

    private final RequirementService requirementService;

    @PostMapping
    public ResponseEntity<ApiResponse<RequirementResponse>> create(@Valid @RequestBody RequirementRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Requirement added", requirementService.create(request)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RequirementResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(requirementService.getById(id)));
    }

    /** Global, visibility-scoped list for the Requirements screen. */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<RequirementResponse>>> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) RequirementStatus status,
            @RequestParam(required = false) Priority priority,
            @RequestParam(required = false) Boolean needsSupplier,
            @PageableDefault(sort = { "createdAt", "id" }, direction = Sort.Direction.DESC) Pageable pageable) {
        var page = requirementService.listAll(q, status, priority, needsSupplier, pageable);
        return ResponseEntity.ok(ApiResponse.ok(new PageResponse<>(page)));
    }

    @GetMapping("/by-lead/{leadId}")
    public ResponseEntity<ApiResponse<PageResponse<RequirementResponse>>> byLead(@PathVariable UUID leadId,
            @PageableDefault(sort = { "createdAt", "id" }, direction = Sort.Direction.DESC) Pageable pageable) {
        var page = requirementService.listByLead(leadId, pageable);
        return ResponseEntity.ok(ApiResponse.ok(new PageResponse<>(page)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<RequirementResponse>> update(@PathVariable UUID id,
            @Valid @RequestBody RequirementRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Requirement updated",
                requirementService.update(id, request)));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<RequirementResponse>> changeStatus(@PathVariable UUID id,
            @RequestParam RequirementStatus status) {
        return ResponseEntity.ok(ApiResponse.ok("Status updated",
                requirementService.changeStatus(id, status)));
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<Map<String, Long>>> summary() {
        return ResponseEntity.ok(ApiResponse.ok(requirementService.summary()));
    }
}
