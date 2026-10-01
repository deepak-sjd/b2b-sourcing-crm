package com.doctutorials.crm.task;

import com.doctutorials.crm.audit.AuditAction;
import com.doctutorials.crm.audit.AuditService;
import com.doctutorials.crm.common.exception.ResourceNotFoundException;
import com.doctutorials.crm.lead.Lead;
import com.doctutorials.crm.lead.LeadService;
import com.doctutorials.crm.user.User;
import com.doctutorials.crm.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final LeadService leadService;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final TaskAccessService taskAccessService;

    @Transactional
    public Task create(UUID leadId, String title, String description, UUID assignedToId, Instant dueDate) {
        // Routes through LeadService.getById (not LeadRepository directly) so this
        // throws a
        // 404 for a lead the caller can't see, instead of silently creating a task
        // nobody who
        // could ever see the lead can then see either.
        Lead lead = leadService.getById(leadId);

        Task task = new Task();
        task.setLead(lead);
        task.setTitle(title);
        task.setDescription(description);
        task.setDueDate(dueDate);

        if (assignedToId != null) {
            User assignee = userRepository.findById(assignedToId)
                    .orElseThrow(() -> ResourceNotFoundException.of("User", assignedToId));
            task.setAssignedTo(assignee);
        }

        Task saved = taskRepository.save(task);
        auditService.log("Task", saved.getId(), AuditAction.CREATE, "Task created: " + title);
        return saved;
    }

    @Transactional
    public Task complete(UUID id) {
        Task task = getById(id);
        task.setStatus(TaskStatus.COMPLETED);
        task.setCompletedAt(Instant.now());
        Task saved = taskRepository.save(task);
        auditService.log("Task", id, AuditAction.STATUS_CHANGE, "Task marked completed");
        return saved;
    }

    public Task getById(UUID id) {
        Task task = taskRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Task", id));
        // A task is visible exactly when its lead is - re-running the lead's own
        // visibility
        // check here (instead of a separate Task rule) is what keeps this in sync with
        // LeadAccessService automatically as that rule evolves.
        leadService.getById(task.getLead().getId());
        return task;
    }

    public Page<Task> listByLead(UUID leadId, Pageable pageable) {
        // Throws 404 if the lead itself isn't visible; once that passes, every task
        // under it
        // is in scope by definition, so no extra Task-level filter is needed here.
        leadService.getById(leadId);
        return taskRepository.findByLeadId(leadId, pageable);
    }

    public Page<Task> listByAssignee(UUID userId, Pageable pageable) {
        Specification<Task> spec = Specification.where(taskAccessService.taskVisibility())
                .and((root, query, cb) -> cb.equal(root.get("assignedTo").get("id"), userId));
        return taskRepository.findAll(spec, pageable);
    }

    /**
     * Global, visibility-scoped task list backing GET /tasks (the standalone
     * Tasks screen) - same access rule as everything else here, via
     * TaskAccessService, plus an optional status filter.
     */
    @Transactional(readOnly = true)
    public Page<Task> listAll(TaskStatus status, Pageable pageable) {
        Specification<Task> spec = Specification.where(taskAccessService.taskVisibility());
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        return taskRepository.findAll(spec, pageable);
    }

    public long countPending() {
        return taskRepository.count(Specification.where(taskAccessService.taskVisibility())
                .and((root, query, cb) -> cb.equal(root.get("status"), TaskStatus.PENDING)));
    }

    public long countCompleted() {
        return taskRepository.count(Specification.where(taskAccessService.taskVisibility())
                .and((root, query, cb) -> cb.equal(root.get("status"), TaskStatus.COMPLETED)));
    }

    public long countOverdue() {
        Instant now = Instant.now();
        Specification<Task> spec = Specification.where(taskAccessService.taskVisibility())
                .and((root, query, cb) -> cb.lessThan(root.get("dueDate"), now))
                .and((root, query, cb) -> root.get("status").in(TaskStatus.COMPLETED, TaskStatus.CANCELLED).not());
        return taskRepository.count(spec);
    }

    /**
     * Runs every 30 min: flips any PENDING/IN_PROGRESS task past its due date to
     * OVERDUE.
     */
    @Scheduled(fixedRate = 30 * 60 * 1000)
    @Transactional
    public void markOverdueTasks() {
        // Not access-scoped on purpose: this is a system job with no acting user,
        // running
        // org-wide maintenance, not answering a specific person's request.
        List<Task> due = taskRepository.findByStatusNotAndDueDateBefore(TaskStatus.OVERDUE, Instant.now());
        for (Task t : due) {
            if (t.getStatus() == TaskStatus.PENDING || t.getStatus() == TaskStatus.IN_PROGRESS) {
                t.setStatus(TaskStatus.OVERDUE);
            }
        }
        taskRepository.saveAll(due);
    }
}
