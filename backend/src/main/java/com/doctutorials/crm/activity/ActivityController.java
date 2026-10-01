package com.doctutorials.crm.activity;

import com.doctutorials.crm.common.ApiResponse;
import com.doctutorials.crm.common.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/activities")
@RequiredArgsConstructor
public class ActivityController {

    private final ActivityService activityService;

    @PostMapping
    public ResponseEntity<ApiResponse<Activity>> log(@RequestBody ActivityService.LogActivityRequest req) {
        return ResponseEntity.ok(ApiResponse.ok("Activity logged", activityService.logActivity(req)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<Activity>>> list(
            @RequestParam(required = false) ActivityType type,
            @RequestParam(required = false) ActivityStatus status,
            @PageableDefault(sort = { "scheduledAt", "id" }, direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(new PageResponse<>(activityService.listAll(type, status, pageable))));
    }

    @GetMapping("/by-lead/{leadId}")
    public ResponseEntity<ApiResponse<PageResponse<Activity>>> byLead(
            @PathVariable UUID leadId, @RequestParam(required = false) ActivityType type,
            @PageableDefault(sort = { "scheduledAt", "id" }, direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(new PageResponse<>(activityService.listByLead(leadId, type, pageable))));
    }

    /** Auto-loaded by the Activities page on mount. */
    @GetMapping("/due-follow-ups")
    public ResponseEntity<ApiResponse<PageResponse<Activity>>> dueFollowUps(
            @PageableDefault(sort = { "followUpAt" }, direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(new PageResponse<>(activityService.listDueFollowUps(pageable))));
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<Map<String, Object>>> summary() {
        return ResponseEntity.ok(ApiResponse.ok(activityService.summary()));
    }
}