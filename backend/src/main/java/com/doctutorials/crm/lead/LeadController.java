package com.doctutorials.crm.lead;

import com.doctutorials.crm.common.ApiResponse;
import com.doctutorials.crm.common.PageResponse;
import com.doctutorials.crm.lead.dto.LeadRequest;
import com.doctutorials.crm.lead.dto.LeadResponse;
import com.doctutorials.crm.lead.dto.MarkLostRequest;
import com.doctutorials.crm.lead.dto.MarkWonRequest;
import com.doctutorials.crm.lead.dto.ScrapRequest;
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
@RequestMapping("/api/v1/leads")
@RequiredArgsConstructor
public class LeadController {

    private final LeadService leadService;

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','MANAGER','COUNSELOR')")
    public ResponseEntity<ApiResponse<LeadResponse>> create(@Valid @RequestBody LeadRequest request) {
        Lead lead = leadService.create(request);
        return ResponseEntity.ok(ApiResponse.ok("Lead created", toResponse(lead)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<LeadResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(toResponse(leadService.getById(id))));
    }

    /**
     * Default sort is newest-first, with id as a tie-breaker.
     *
     * This is load-bearing, not cosmetic. Without an explicit ORDER BY, Postgres
     * is free to return rows in any order it likes (in practice: heap/physical
     * order), which caused two real bugs:
     *   1. A newly created lead was written at the end of the heap, so with 100+
     *      existing leads it landed on the last page - the UI refetched page 0
     *      after "Lead created" and the new lead was nowhere to be seen.
     *   2. Paging through an unordered result set can show the same row on two
     *      pages and skip others entirely, because LIMIT/OFFSET has no stable
     *      ordering to slice.
     * The id tie-breaker matters because createdAt has limited resolution -
     * two leads created in the same instant would otherwise still be unstable.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<LeadResponse>>> list(
            @RequestParam(required = false) LeadStage stage,
            @RequestParam(required = false) UUID ownerId,
            @PageableDefault(sort = { "createdAt", "id" }, direction = Sort.Direction.DESC) Pageable pageable) {

        Page<Lead> page;
        if (stage != null) {
            page = leadService.listByStage(stage, pageable);
        } else if (ownerId != null) {
            page = leadService.listByOwner(ownerId, pageable);
        } else {
            page = leadService.list(pageable);
        }

        Page<LeadResponse> mapped = page.map(this::toResponse);
        return ResponseEntity.ok(ApiResponse.ok(new PageResponse<>(mapped)));
    }

    /** PRD §14.3 - the Scrap Records section, kept as its own list endpoint
     *  rather than a filter on the main one; see LeadService.listScrapped. */
    @GetMapping("/scrap")
    public ResponseEntity<ApiResponse<PageResponse<LeadResponse>>> listScrapped(
            @PageableDefault(sort = { "scrappedAt", "id" }, direction = Sort.Direction.DESC) Pageable pageable) {
        Page<LeadResponse> mapped = leadService.listScrapped(pageable).map(this::toResponse);
        return ResponseEntity.ok(ApiResponse.ok(new PageResponse<>(mapped)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','MANAGER','COUNSELOR')")
    public ResponseEntity<ApiResponse<LeadResponse>> update(@PathVariable UUID id, @Valid @RequestBody LeadRequest request) {
        Lead lead = leadService.update(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Lead updated", toResponse(lead)));
    }

    /** Only handles the four open-pipeline stages - see LeadService.changeStage's
     *  javadoc for why WON/LOST are rejected here. */
    @PatchMapping("/{id}/stage")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','MANAGER','COUNSELOR')")
    public ResponseEntity<ApiResponse<LeadResponse>> changeStage(@PathVariable UUID id, @RequestParam LeadStage stage) {
        Lead lead = leadService.changeStage(id, stage);
        return ResponseEntity.ok(ApiResponse.ok("Stage updated", toResponse(lead)));
    }

    @PostMapping("/{id}/won")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','MANAGER','COUNSELOR')")
    public ResponseEntity<ApiResponse<LeadResponse>> markWon(@PathVariable UUID id, @Valid @RequestBody MarkWonRequest request) {
        Lead lead = leadService.markWon(id, request.finalValue(), request.remarks());
        return ResponseEntity.ok(ApiResponse.ok("Lead marked Won", toResponse(lead)));
    }

    @PostMapping("/{id}/lost")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','MANAGER','COUNSELOR')")
    public ResponseEntity<ApiResponse<LeadResponse>> markLost(@PathVariable UUID id, @Valid @RequestBody MarkLostRequest request) {
        Lead lead = leadService.markLost(id, request.lostReason(), request.comment());
        return ResponseEntity.ok(ApiResponse.ok("Lead marked Lost", toResponse(lead)));
    }

    @PostMapping("/{id}/scrap")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','MANAGER','COUNSELOR')")
    public ResponseEntity<ApiResponse<LeadResponse>> moveToScrap(@PathVariable UUID id, @Valid @RequestBody ScrapRequest request) {
        Lead lead = leadService.moveToScrap(id, request.scrapReason(), request.remarks());
        return ResponseEntity.ok(ApiResponse.ok("Lead moved to Scrap", toResponse(lead)));
    }

    @PostMapping("/{id}/reactivate")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','MANAGER','COUNSELOR')")
    public ResponseEntity<ApiResponse<LeadResponse>> reactivate(@PathVariable UUID id) {
        Lead lead = leadService.reactivate(id);
        return ResponseEntity.ok(ApiResponse.ok("Lead reactivated", toResponse(lead)));
    }

    @PatchMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<LeadResponse>> assign(@PathVariable UUID id, @RequestParam UUID ownerId) {
        Lead lead = leadService.assignOwner(id, ownerId);
        return ResponseEntity.ok(ApiResponse.ok("Lead reassigned", toResponse(lead)));
    }

    @PostMapping("/{id}/touchpoint")
    public ResponseEntity<ApiResponse<Void>> recordTouchpoint(@PathVariable UUID id) {
        leadService.recordTouchpoint(id);
        return ResponseEntity.ok(ApiResponse.ok("Touchpoint recorded", null));
    }

    @PostMapping("/{id}/ai/refresh")
    public ResponseEntity<ApiResponse<LeadResponse>> refreshAiInsight(@PathVariable UUID id) {
        Lead lead = leadService.refreshAiInsight(id);
        return ResponseEntity.ok(ApiResponse.ok("AI insight refreshed", toResponse(lead)));
    }

    @GetMapping("/at-risk")
    public ResponseEntity<ApiResponse<List<LeadResponse>>> atRisk() {
        List<LeadResponse> risky = leadService.findAtRiskLeads().stream().map(this::toResponse).toList();
        return ResponseEntity.ok(ApiResponse.ok(risky));
    }

    private LeadResponse toResponse(Lead lead) {
        return LeadResponse.from(lead, leadService.getStaleThresholdDays());
    }
}
