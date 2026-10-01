package com.doctutorials.crm.task;

import com.doctutorials.crm.common.ApiResponse;
import com.doctutorials.crm.common.PageResponse;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    public record CreateTaskRequest(@NotBlank String title, String description,
                                     UUID leadId, UUID assignedToId, Instant dueDate) {}

    @PostMapping
    public ResponseEntity<ApiResponse<Task>> create(@RequestBody CreateTaskRequest req) {
        Task task = taskService.create(req.leadId(), req.title(), req.description(), req.assignedToId(), req.dueDate());
        return ResponseEntity.ok(ApiResponse.ok("Task created", task));
    }

    @PatchMapping("/{id}/complete")
    public ResponseEntity<ApiResponse<Task>> complete(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok("Task completed", taskService.complete(id)));
    }

    /**
     * Global, visibility-scoped task list (the "Tasks" screen) - filterable by
     * status, sorted by dueDate ascending by default so the most urgent items
     * surface first. Added alongside /by-lead/{leadId}, which stays as-is for the
     * lead-detail Tasks tab.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<Task>>> list(
            @RequestParam(required = false) TaskStatus status,
            @PageableDefault(sort = { "dueDate", "id" }) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(new PageResponse<>(taskService.listAll(status, pageable))));
    }

    @GetMapping("/by-lead/{leadId}")
    public ResponseEntity<ApiResponse<PageResponse<Task>>> byLead(@PathVariable UUID leadId,
            @PageableDefault(sort = { "createdAt", "id" }, direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(new PageResponse<>(taskService.listByLead(leadId, pageable))));
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<Map<String, Long>>> summary() {
        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "pending", taskService.countPending(),
                "completed", taskService.countCompleted(),
                "overdue", taskService.countOverdue()
        )));
    }
}
