package com.doctutorials.crm.task;

import com.doctutorials.crm.common.ApiResponse;
import com.doctutorials.crm.common.PageResponse;
import com.doctutorials.crm.common.exception.BadRequestException;
import com.doctutorials.crm.lead.Priority;
import com.doctutorials.crm.task.dto.TaskActionRequests;
import com.doctutorials.crm.task.dto.TaskRequest;
import com.doctutorials.crm.task.dto.TaskResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** R4: every write is @PreAuthorize'd (VIEWER is read-only); reads are scoped in TaskService. R5: DTOs only. */
@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
public class TaskController {

    private static final String WRITERS = "hasAnyRole('SUPER_ADMIN','ADMIN','MANAGER','COUNSELOR')";

    private final TaskService taskService;

    @PostMapping
    @PreAuthorize(WRITERS)
    public ResponseEntity<ApiResponse<TaskResponse>> create(@Valid @RequestBody TaskRequest req) {
        return ResponseEntity.ok(ApiResponse.ok("Task created", taskService.create(req)));
    }

    @PutMapping("/{id}")
    @PreAuthorize(WRITERS)
    public ResponseEntity<ApiResponse<TaskResponse>> update(@PathVariable UUID id, @Valid @RequestBody TaskRequest req) {
        return ResponseEntity.ok(ApiResponse.ok("Task updated", taskService.update(id, req)));
    }

    @PatchMapping("/{id}/complete")
    @PreAuthorize(WRITERS)
    public ResponseEntity<ApiResponse<TaskResponse>> complete(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok("Task completed", taskService.complete(id)));
    }

    @PostMapping("/bulk-complete")
    @PreAuthorize(WRITERS)
    public ResponseEntity<ApiResponse<List<TaskResponse>>> bulkComplete(
            @Valid @RequestBody TaskActionRequests.BulkComplete req) {
        return ResponseEntity.ok(ApiResponse.ok("Tasks completed", taskService.bulkComplete(req.ids())));
    }

    @PatchMapping("/{id}/reopen")
    @PreAuthorize(WRITERS)
    public ResponseEntity<ApiResponse<TaskResponse>> reopen(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok("Task reopened", taskService.reopen(id)));
    }

    @PatchMapping("/{id}/snooze")
    @PreAuthorize(WRITERS)
    public ResponseEntity<ApiResponse<TaskResponse>> snooze(@PathVariable UUID id,
            @Valid @RequestBody TaskActionRequests.Snooze req) {
        return ResponseEntity.ok(ApiResponse.ok("Task snoozed", taskService.snooze(id, req.until())));
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize(WRITERS)
    public ResponseEntity<ApiResponse<TaskResponse>> cancel(@PathVariable UUID id,
            @Valid @RequestBody(required = false) TaskActionRequests.Cancel req) {
        return ResponseEntity.ok(ApiResponse.ok("Task cancelled",
                taskService.cancel(id, req == null ? null : req.reason())));
    }

    @PatchMapping("/{id}/assign")
    @PreAuthorize(WRITERS)
    public ResponseEntity<ApiResponse<TaskResponse>> reassign(@PathVariable UUID id,
            @Valid @RequestBody TaskActionRequests.Reassign req) {
        return ResponseEntity.ok(ApiResponse.ok("Task reassigned", taskService.reassign(id, req.assigneeId())));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TaskResponse>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(taskService.getById(id)));
    }

    /** Global, visibility-scoped list. Soonest due first. */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<TaskResponse>>> list(
            @RequestParam(required = false) UUID assigneeId,
            @RequestParam(required = false) TaskType type,
            @RequestParam(required = false) Priority priority,
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dueFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dueTo,
            @RequestParam(required = false) UUID leadId,
            @PageableDefault(sort = { "dueDate", "id" }) Pageable pageable) {
        var filter = new TaskService.TaskFilter(assigneeId, type, priority, status, dueFrom, dueTo, leadId);
        return ResponseEntity.ok(ApiResponse.ok(new PageResponse<>(taskService.listAll(filter, pageable))));
    }

    /** My Day. tz = the browser's IANA zone (e.g. Asia/Kolkata) so "today" means the user's today. */
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<PageResponse<TaskResponse>>> my(
            @RequestParam TaskBucket bucket,
            @RequestParam(defaultValue = "UTC") String tz,
            @PageableDefault(sort = { "dueDate", "id" }) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(new PageResponse<>(taskService.my(bucket, zone(tz), pageable))));
    }

    /** Sidebar badge and section counts in one call. */
    @GetMapping("/my/counts")
    public ResponseEntity<ApiResponse<TaskService.TaskCounts>> myCounts(@RequestParam(defaultValue = "UTC") String tz) {
        return ResponseEntity.ok(ApiResponse.ok(taskService.myCounts(zone(tz))));
    }

    @GetMapping("/by-lead/{leadId}")
    public ResponseEntity<ApiResponse<PageResponse<TaskResponse>>> byLead(@PathVariable UUID leadId,
            @PageableDefault(sort = { "createdAt", "id" }, direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(new PageResponse<>(taskService.listByLead(leadId, pageable))));
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<Map<String, Long>>> summary() {
        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "pending", taskService.countPending(),
                "completed", taskService.countCompleted(),
                "overdue", taskService.countOverdue())));
    }

    private static ZoneId zone(String tz) {
        try {
            return ZoneId.of(tz);
        } catch (DateTimeException e) {
            throw new BadRequestException("Unknown time zone: " + tz);
        }
    }
}
