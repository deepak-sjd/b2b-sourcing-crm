package com.doctutorials.crm.task;

import com.doctutorials.crm.activity.Activity;
import com.doctutorials.crm.activity.ActivityRepository;
import com.doctutorials.crm.audit.AuditAction;
import com.doctutorials.crm.audit.AuditService;
import com.doctutorials.crm.common.event.TaskCompleted;
import com.doctutorials.crm.common.event.TaskCreated;
import com.doctutorials.crm.common.event.TaskOverdue;
import com.doctutorials.crm.common.exception.BadRequestException;
import com.doctutorials.crm.common.exception.ResourceNotFoundException;
import com.doctutorials.crm.customer.Customer;
import com.doctutorials.crm.customer.CustomerService;
import com.doctutorials.crm.lead.Lead;
import com.doctutorials.crm.lead.LeadService;
import com.doctutorials.crm.lead.Priority;
import com.doctutorials.crm.quotation.Quotation;
import com.doctutorials.crm.quotation.QuotationService;
import com.doctutorials.crm.requirement.Requirement;
import com.doctutorials.crm.requirement.RequirementRepository;
import com.doctutorials.crm.security.CurrentUserService;
import com.doctutorials.crm.supplier.enquiry.SupplierEnquiry;
import com.doctutorials.crm.supplier.enquiry.SupplierEnquiryRepository;
import com.doctutorials.crm.task.dto.TaskRequest;
import com.doctutorials.crm.task.dto.TaskResponse;
import com.doctutorials.crm.user.User;
import com.doctutorials.crm.user.UserRepository;
import jakarta.persistence.criteria.Path;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TaskService {

    private static final Logger log = LoggerFactory.getLogger(TaskService.class);

    private static final List<TaskStatus> OPEN = List.of(TaskStatus.PENDING, TaskStatus.IN_PROGRESS, TaskStatus.OVERDUE);
    private static final List<TaskStatus> RUNNING = List.of(TaskStatus.PENDING, TaskStatus.IN_PROGRESS);

    private final TaskRepository taskRepository;
    private final TaskAccessService accessService;
    private final CurrentUserService currentUserService;
    private final LeadService leadService;
    private final CustomerService customerService;
    private final QuotationService quotationService;
    private final RequirementRepository requirementRepository;
    private final SupplierEnquiryRepository enquiryRepository;
    private final ActivityRepository activityRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final ApplicationEventPublisher events;

    public record TaskFilter(UUID assigneeId, TaskType type, Priority priority, TaskStatus status,
            Instant dueFrom, Instant dueTo, UUID leadId) {
    }

    public record TaskCounts(long overdue, long today, long upcoming) {
    }

    // ------------------------------------------------------------------ create / edit

    /** Kept for ActivityService's auto follow-ups (D4-04 moves that into an ActivityLogged listener). */
    @Transactional
    public TaskResponse create(UUID leadId, String title, String description, UUID assignedToId, Instant dueDate) {
        return create(new TaskRequest(title, description, TaskType.CALL, leadId, null, null, null, null, null,
                assignedToId, dueDate, null, null, null));
    }

    @Transactional
    public TaskResponse create(TaskRequest req) {
        User actor = currentUserService.get();
        Task task = new Task();
        task.setCreatedBy(actor);
        applyLinks(task, req);
        applyFields(task, req);
        task.setAssignedTo(resolveAssignee(req.assignedToId(), task, actor));

        Task saved = taskRepository.save(task);
        auditService.logChange("Task", saved.getId(), AuditAction.CREATE, "Task created: " + saved.getTitle(),
                Collections.emptyMap(), snapshot(saved));
        events.publishEvent(new TaskCreated(saved.getId(), leadId(saved), assigneeId(saved), saved.getType(),
                saved.getDueDate()));
        return TaskResponse.from(saved);
    }

    @Transactional
    public TaskResponse update(UUID id, TaskRequest req) {
        Task task = loadForEdit(id);
        assertEditable(task);
        Map<String, Object> before = snapshot(task);
        applyFields(task, req);
        unflipOverdueIfMovedForward(task);
        Task saved = taskRepository.save(task);
        auditService.logChange("Task", id, AuditAction.UPDATE, "Task updated", before, snapshot(saved));
        return TaskResponse.from(saved);
    }

    @Transactional
    public TaskResponse reassign(UUID id, UUID assigneeId) {
        User actor = currentUserService.get();
        Task task = loadForEdit(id);
        assertEditable(task);
        User assignee = findActiveUser(assigneeId);
        accessService.assertCanAssign(actor, assignee);
        Map<String, Object> before = snapshot(task);
        task.setAssignedTo(assignee);
        Task saved = taskRepository.save(task);
        auditService.logChange("Task", id, AuditAction.UPDATE, "Task reassigned to " + assignee.getFullName(),
                before, snapshot(saved));
        return TaskResponse.from(saved);
    }

    /** Snooze = push the due date (and any reminder, by the same amount). Needs an open task. */
    @Transactional
    public TaskResponse snooze(UUID id, Instant until) {
        Task task = loadForEdit(id);
        assertEditable(task);
        if (!until.isAfter(Instant.now())) {
            throw new BadRequestException("Snooze time must be in the future");
        }
        Map<String, Object> before = snapshot(task);
        if (task.getReminderAt() != null && task.getDueDate() != null) {
            long shiftMillis = until.toEpochMilli() - task.getDueDate().toEpochMilli();
            task.setReminderAt(task.getReminderAt().plusMillis(shiftMillis));
        }
        task.setDueDate(until);
        unflipOverdueIfMovedForward(task);
        Task saved = taskRepository.save(task);
        auditService.logChange("Task", id, AuditAction.UPDATE, "Task snoozed", before, snapshot(saved));
        return TaskResponse.from(saved);
    }

    @Transactional
    public TaskResponse complete(UUID id) {
        User actor = currentUserService.get();
        return TaskResponse.from(doComplete(loadForEdit(id), actor));
    }

    /** All-or-nothing: one task the caller can't touch fails the whole batch. */
    @Transactional
    public List<TaskResponse> bulkComplete(List<UUID> ids) {
        User actor = currentUserService.get();
        List<TaskResponse> done = new ArrayList<>();
        for (UUID id : ids.stream().distinct().toList()) {
            done.add(TaskResponse.from(doComplete(loadForEdit(id), actor)));
        }
        return done;
    }

    @Transactional
    public TaskResponse reopen(UUID id) {
        Task task = loadForEdit(id);
        if (task.getStatus() != TaskStatus.COMPLETED && task.getStatus() != TaskStatus.CANCELLED) {
            throw new BadRequestException("Only a completed or cancelled task can be reopened");
        }
        Map<String, Object> before = snapshot(task);
        TaskStatus old = task.getStatus();
        task.setStatus(TaskStatus.PENDING);
        task.setCompletedAt(null);
        task.setCompletedBy(null);
        task.setCancelReason(null);
        Task saved = taskRepository.save(task);
        auditService.logChange("Task", id, AuditAction.STATUS_CHANGE, "Task reopened (was " + old + ")",
                before, snapshot(saved));
        return TaskResponse.from(saved);
    }

    /** Soft cancel only (R1) - there is no delete. */
    @Transactional
    public TaskResponse cancel(UUID id, String reason) {
        Task task = loadForEdit(id);
        if (task.getStatus() == TaskStatus.COMPLETED) {
            throw new BadRequestException("A completed task can't be cancelled - reopen it first");
        }
        if (task.getStatus() == TaskStatus.CANCELLED) {
            return TaskResponse.from(task);
        }
        Map<String, Object> before = snapshot(task);
        task.setStatus(TaskStatus.CANCELLED);
        task.setCancelReason(blankToNull(reason));
        Task saved = taskRepository.save(task);
        auditService.logChange("Task", id, AuditAction.STATUS_CHANGE, "Task cancelled", before, snapshot(saved));
        return TaskResponse.from(saved);
    }

    // ------------------------------------------------------------------ reads

    @Transactional(readOnly = true)
    public TaskResponse getById(UUID id) {
        return TaskResponse.from(loadVisible(id));
    }

    @Transactional(readOnly = true)
    public Page<TaskResponse> listAll(TaskFilter f, Pageable pageable) {
        Specification<Task> spec = Specification.where(accessService.taskVisibility());
        if (f.assigneeId() != null) spec = spec.and(field("assignedTo", f.assigneeId()));
        if (f.leadId() != null) spec = spec.and(field("lead", f.leadId()));
        if (f.type() != null) spec = spec.and((r, q, cb) -> cb.equal(r.get("type"), f.type()));
        if (f.priority() != null) spec = spec.and((r, q, cb) -> cb.equal(r.get("priority"), f.priority()));
        if (f.status() != null) spec = spec.and((r, q, cb) -> cb.equal(r.get("status"), f.status()));
        if (f.dueFrom() != null) spec = spec.and((r, q, cb) -> cb.greaterThanOrEqualTo(r.<Instant>get("dueDate"), f.dueFrom()));
        if (f.dueTo() != null) spec = spec.and((r, q, cb) -> cb.lessThan(r.<Instant>get("dueDate"), f.dueTo()));
        return taskRepository.findAll(spec, pageable).map(TaskResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<TaskResponse> listByLead(UUID leadId, Pageable pageable) {
        leadService.getById(leadId); // 404 if the lead isn't visible
        return taskRepository.findAll(field("lead", leadId), pageable).map(TaskResponse::from);
    }

    /** My Day: the current user's open tasks in one bucket. */
    @Transactional(readOnly = true)
    public Page<TaskResponse> my(TaskBucket bucket, ZoneId zone, Pageable pageable) {
        return taskRepository.findAll(mySpec(bucket, zone), pageable).map(TaskResponse::from);
    }

    @Transactional(readOnly = true)
    public TaskCounts myCounts(ZoneId zone) {
        return new TaskCounts(
                taskRepository.count(mySpec(TaskBucket.OVERDUE, zone)),
                taskRepository.count(mySpec(TaskBucket.TODAY, zone)),
                taskRepository.count(mySpec(TaskBucket.UPCOMING, zone)));
    }

    public long countPending() {
        return count(TaskStatus.PENDING);
    }

    public long countCompleted() {
        return count(TaskStatus.COMPLETED);
    }

    /** Overdue by due date (not status), so it's right between sweeps. */
    public long countOverdue() {
        Instant now = Instant.now();
        return taskRepository.count(Specification.where(accessService.taskVisibility())
                .and((r, q, cb) -> cb.and(r.get("status").in(OPEN), cb.lessThan(r.<Instant>get("dueDate"), now))));
    }

    // ------------------------------------------------------------------ system sweep

    /**
     * Every 30 min. One bulk UPDATE for the whole backlog (no entity loading), then one TaskOverdue
     * event per flipped task. Idempotent, so running on two instances is safe (consumers dedupe on taskId).
     * Derived state: not audited per row (decision for D2 to confirm).
     */
    @Scheduled(fixedRate = 30 * 60 * 1000)
    @Transactional
    public void markOverdueTasks() {
        Instant now = Instant.now();
        List<TaskRepository.OverdueRow> rows = taskRepository.findOverdueCandidates(RUNNING, now);
        if (rows.isEmpty()) return;
        int updated = taskRepository.bulkMarkOverdue(RUNNING, now);
        rows.forEach(r -> events.publishEvent(new TaskOverdue(r.getId(), r.getLeadId(), r.getAssigneeId(), r.getDueDate())));
        log.info("Marked {} task(s) overdue", updated);
    }

    // ------------------------------------------------------------------ internals

    private Task doComplete(Task task, User actor) {
        if (task.getStatus() == TaskStatus.COMPLETED) return task;
        if (task.getStatus() == TaskStatus.CANCELLED) {
            throw new BadRequestException("A cancelled task can't be completed - reopen it first");
        }
        Map<String, Object> before = snapshot(task);
        task.setStatus(TaskStatus.COMPLETED);
        task.setCompletedAt(Instant.now());
        task.setCompletedBy(actor);
        Task saved = taskRepository.save(task);
        auditService.logChange("Task", saved.getId(), AuditAction.STATUS_CHANGE, "Task marked completed",
                before, snapshot(saved));
        events.publishEvent(new TaskCompleted(saved.getId(), leadId(saved), actor.getId()));
        return saved;
    }

    private void applyFields(Task task, TaskRequest req) {
        if (req.reminderAt() != null && req.dueDate() != null && req.reminderAt().isAfter(req.dueDate())) {
            throw new BadRequestException("The reminder must be at or before the due date");
        }
        task.setTitle(req.title().trim());
        task.setDescription(blankToNull(req.description()));
        task.setType(req.type() != null ? req.type() : TaskType.INTERNAL);
        task.setPriority(req.priority() != null ? req.priority() : Priority.MEDIUM);
        task.setDueDate(req.dueDate());
        task.setReminderAt(req.reminderAt());
        task.setRemarks(blankToNull(req.remarks()));
    }

    /** Resolves every link through its own visibility check and makes sure they all point at the same lead. */
    private void applyLinks(Task task, TaskRequest req) {
        Lead lead = req.leadId() != null ? leadService.getById(req.leadId()) : null;
        Customer customer = req.customerId() != null ? customerService.getById(req.customerId()) : null;

        if (req.requirementId() != null) {
            Requirement r = requirementRepository.findById(req.requirementId())
                    .orElseThrow(() -> ResourceNotFoundException.of("Requirement", req.requirementId()));
            lead = reconcile(lead, r.getLead(), "requirement");
            task.setRequirement(r);
        }
        if (req.enquiryId() != null) {
            SupplierEnquiry e = enquiryRepository.findById(req.enquiryId())
                    .orElseThrow(() -> ResourceNotFoundException.of("SupplierEnquiry", req.enquiryId()));
            lead = reconcile(lead, e.getLead(), "enquiry");
            task.setEnquiry(e);
        }
        if (req.activityId() != null) {
            Activity a = activityRepository.findById(req.activityId())
                    .orElseThrow(() -> ResourceNotFoundException.of("Activity", req.activityId()));
            lead = reconcile(lead, a.getLead(), "activity");
            task.setActivity(a);
        }
        if (req.quotationId() != null) {
            Quotation q = quotationService.getById(req.quotationId()); // visibility-checked
            if (q.getLead() != null) lead = reconcile(lead, q.getLead(), "quotation");
            if (customer == null) customer = q.getCustomer();
            task.setQuotation(q);
        }
        if (lead == null && customer == null) {
            throw new BadRequestException("A task needs a lead or a customer");
        }
        task.setLead(lead);
        task.setCustomer(customer);
    }

    private Lead reconcile(Lead current, Lead linked, String what) {
        Lead visible = leadService.getById(linked.getId());
        if (current != null && !current.getId().equals(visible.getId())) {
            throw new BadRequestException("That " + what + " belongs to a different lead");
        }
        return visible;
    }

    private User resolveAssignee(UUID assignedToId, Task task, User actor) {
        if (assignedToId != null) {
            User assignee = findActiveUser(assignedToId);
            accessService.assertCanAssign(actor, assignee);
            return assignee;
        }
        if (task.getLead() != null && task.getLead().getOwner() != null) return task.getLead().getOwner();
        if (task.getCustomer() != null && task.getCustomer().getAccountManager() != null) {
            return task.getCustomer().getAccountManager();
        }
        return actor;
    }

    private User findActiveUser(UUID id) {
        User user = userRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("User", id));
        if (!user.isActive()) throw new BadRequestException(user.getFullName() + " is deactivated");
        return user;
    }

    private Task loadVisible(UUID id) {
        Task task = taskRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Task", id));
        accessService.assertCanView(task);
        return task;
    }

    private Task loadForEdit(UUID id) {
        Task task = loadVisible(id);
        accessService.assertCanEdit(task);
        return task;
    }

    private void assertEditable(Task task) {
        if (task.getStatus() == TaskStatus.COMPLETED || task.getStatus() == TaskStatus.CANCELLED) {
            throw new BadRequestException("A " + task.getStatus() + " task can't be edited - reopen it first");
        }
    }

    /** A task whose due date moved into the future is no longer overdue. */
    private void unflipOverdueIfMovedForward(Task task) {
        if (task.getStatus() == TaskStatus.OVERDUE
                && (task.getDueDate() == null || task.getDueDate().isAfter(Instant.now()))) {
            task.setStatus(TaskStatus.PENDING);
        }
    }

    private Specification<Task> mySpec(TaskBucket bucket, ZoneId zone) {
        UUID me = currentUserService.get().getId();
        Instant now = Instant.now();
        Instant endOfToday = TaskBucket.endOfToday(now, zone);
        return field("assignedTo", me).and((root, q, cb) -> {
            Path<Instant> due = root.get("dueDate");
            var open = root.get("status").in(OPEN);
            return switch (bucket) {
                case OVERDUE -> cb.and(open, cb.lessThan(due, now));
                case TODAY -> cb.and(open, cb.greaterThanOrEqualTo(due, now), cb.lessThan(due, endOfToday));
                case UPCOMING -> cb.and(open, cb.or(cb.isNull(due), cb.greaterThanOrEqualTo(due, endOfToday)));
            };
        });
    }

    private static Specification<Task> field(String association, UUID id) {
        return (root, q, cb) -> cb.equal(root.get(association).get("id"), id);
    }

    private long count(TaskStatus status) {
        return taskRepository.count(Specification.where(accessService.taskVisibility())
                .and((r, q, cb) -> cb.equal(r.get("status"), status)));
    }

    private Map<String, Object> snapshot(Task t) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("title", t.getTitle());
        m.put("description", t.getDescription());
        m.put("type", t.getType());
        m.put("priority", t.getPriority());
        m.put("status", t.getStatus());
        m.put("dueDate", t.getDueDate());
        m.put("reminderAt", t.getReminderAt());
        m.put("assignedTo", t.getAssignedTo() != null ? t.getAssignedTo().getFullName() : null);
        m.put("completedBy", t.getCompletedBy() != null ? t.getCompletedBy().getFullName() : null);
        m.put("remarks", t.getRemarks());
        m.put("cancelReason", t.getCancelReason());
        return m;
    }

    private static UUID leadId(Task t) {
        return t.getLead() != null ? t.getLead().getId() : null;
    }

    private static UUID assigneeId(Task t) {
        return t.getAssignedTo() != null ? t.getAssignedTo().getId() : null;
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
