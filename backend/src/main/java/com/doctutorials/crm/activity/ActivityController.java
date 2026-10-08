package com.doctutorials.crm.activity;

import com.doctutorials.crm.activity.dto.ActivityResponse;
import com.doctutorials.crm.activity.dto.MeetingStatusRequest;
import com.doctutorials.crm.activity.dto.UpdateActivityRequest;
import com.doctutorials.crm.common.ApiResponse;
import com.doctutorials.crm.common.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/activities")
@RequiredArgsConstructor
public class ActivityController {

    private final ActivityService activityService;

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','MANAGER','COUNSELOR')")
    public ResponseEntity<ApiResponse<ActivityResponse>> log(@RequestBody ActivityService.LogActivityRequest req) {
        return ResponseEntity.ok(ApiResponse.ok("Activity logged", activityService.logActivity(req)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ActivityResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(activityService.getById(id)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','MANAGER','COUNSELOR')")
    public ResponseEntity<ApiResponse<ActivityResponse>> update(
            @PathVariable UUID id, @Valid @RequestBody UpdateActivityRequest req) {
        return ResponseEntity.ok(ApiResponse.ok("Activity updated", activityService.update(id, req)));
    }

    /** PRD §9 meeting lifecycle: complete/reschedule/cancel/no-show. */
    @PatchMapping("/{id}/meeting-status")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','MANAGER','COUNSELOR')")
    public ResponseEntity<ApiResponse<ActivityResponse>> changeMeetingStatus(
            @PathVariable UUID id, @Valid @RequestBody MeetingStatusRequest req) {
        return ResponseEntity.ok(ApiResponse.ok("Meeting status updated", activityService.changeMeetingStatus(id, req)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ActivityResponse>>> list(
            @RequestParam(required = false) ActivityType type,
            @RequestParam(required = false) ActivityStatus status,
            @PageableDefault(sort = { "scheduledAt", "id" }, direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(new PageResponse<>(activityService.listAll(type, status, pageable))));
    }

    /** type=MEETING backs LeadMeetingsTab; no type backs LeadActivitiesTab. */
    @GetMapping("/by-lead/{leadId}")
    public ResponseEntity<ApiResponse<PageResponse<ActivityResponse>>> byLead(
            @PathVariable UUID leadId, @RequestParam(required = false) ActivityType type,
            @PageableDefault(sort = { "scheduledAt", "id" }, direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(new PageResponse<>(activityService.listByLead(leadId, type, pageable))));
    }

    /** Auto-loaded by the Activities page on mount. */
    @GetMapping("/due-follow-ups")
    public ResponseEntity<ApiResponse<PageResponse<ActivityResponse>>> dueFollowUps(
            @PageableDefault(sort = { "followUpAt" }, direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(new PageResponse<>(activityService.listDueFollowUps(pageable))));
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<Map<String, Object>>> summary() {
        return ResponseEntity.ok(ApiResponse.ok(activityService.summary()));
    }
}