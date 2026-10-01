package com.doctutorials.crm.activity;

import com.doctutorials.crm.audit.AuditAction;
import com.doctutorials.crm.audit.AuditService;
import com.doctutorials.crm.lead.Lead;
import com.doctutorials.crm.lead.LeadRepository;
import com.doctutorials.crm.lead.LeadScoringService;
import com.doctutorials.crm.lead.LeadService;
import com.doctutorials.crm.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ActivityService {

    private final ActivityRepository activityRepository;
    private final LeadRepository leadRepository;
    private final LeadService leadService;
    private final LeadScoringService scoringService;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final ActivityAccessService activityAccessService;

    public record LogActivityRequest(
            UUID leadId, ActivityType activityType, ActivityDirection direction, ActivityStatus status,
            Instant scheduledAt, String contactPerson, String location, Integer durationSeconds,
            ActivitySentiment sentiment, String requirements, String notes, Instant followUpAt,
            String taggedResourceEmail) {}

    @Transactional
    public Activity logActivity(LogActivityRequest req) {
        // getById (not the repository directly) throws 404 if the caller can't
        // see this lead - same rule CallService used.
        Lead lead = leadService.getById(req.leadId());

        Activity activity = new Activity();
        activity.setLead(lead);
        activity.setActivityType(req.activityType() != null ? req.activityType() : ActivityType.CALL);
        activity.setDirection(req.direction());
        activity.setStatus(req.status());
        activity.setScheduledAt(req.scheduledAt() != null ? req.scheduledAt() : Instant.now());
        activity.setContactPerson(blankToNull(req.contactPerson()));
        activity.setLocation(blankToNull(req.location()));
        activity.setDurationSeconds(req.durationSeconds());
        activity.setSentiment(req.sentiment());
        activity.setRequirements(blankToNull(req.requirements()));
        activity.setNotes(blankToNull(req.notes()));

        // Looked up by email rather than trusting a raw user ID from the
        // client, so this can only ever point at a real account - a typo'd
        // email just leaves it unset instead of failing or letting the
        // client claim an arbitrary user ID.
        if (req.taggedResourceEmail() != null && !req.taggedResourceEmail().isBlank()) {
            userRepository.findByEmailIgnoreCase(req.taggedResourceEmail().trim())
                    .ifPresent(activity::setTaggedResource);
        }

        Instant followUpAt = resolveFollowUpAt(req.status(), req.followUpAt());
        activity.setFollowUpAt(followUpAt);

        Activity saved = activityRepository.save(activity);

        // A real, already-happened touchpoint refreshes staleness + score;
        // SCHEDULED (a future meeting that hasn't happened yet) doesn't count
        // until it's actually completed.
        if (saved.getStatus() != ActivityStatus.SCHEDULED) {
            scoringService.markTouched(lead);
        }
        // Mirror onto the lead so every screen that already reads
        // Lead.nextFollowUpAt (dashboard, stale-lead banner) picks this up
        // automatically - no separate follow-up module needed.
        if (followUpAt != null) {
            lead.setNextFollowUpAt(followUpAt);
        }
        leadRepository.save(lead);

        auditService.log("Activity", saved.getId(), AuditAction.CREATE,
                saved.getActivityType() + " logged (" + saved.getStatus() + ")");
        return saved;
    }

    /**
     * AUTOMATION #1 - deterministic follow-up suggestion, same spirit as
     * AiReportingAssistantService: fixed rules over the data, not a live LLM
     * call (that class's own comment explains why - PII risk). An explicit
     * followUpAt from the client always wins; this only fills the gap when
     * the rep didn't set one but the outcome implies more contact is needed.
     */
    private Instant resolveFollowUpAt(ActivityStatus status, Instant explicit) {
        if (explicit != null) return explicit;
        if (status == null) return null;
        return switch (status) {
            case NO_ANSWER -> Instant.now().plus(Duration.ofHours(2));
            case BUSY, MISSED -> Instant.now().plus(Duration.ofDays(1));
            case SCHEDULED, COMPLETED -> null;
        };
    }

    private static String blankToNull(String s) { return (s == null || s.isBlank()) ? null : s.trim(); }

    public Page<Activity> listByLead(UUID leadId, ActivityType type, Pageable pageable) {
        leadService.getById(leadId);
        Specification<Activity> spec = (root, query, cb) -> cb.equal(root.get("lead").get("id"), leadId);
        if (type != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("activityType"), type));
        return activityRepository.findAll(spec, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Activity> listAll(ActivityType type, ActivityStatus status, Pageable pageable) {
        Specification<Activity> spec = Specification.where(activityAccessService.activityVisibility());
        if (type != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("activityType"), type));
        if (status != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        return activityRepository.findAll(spec, pageable);
    }

    /**
     * AUTOMATION #2 - backs a "Due today" panel that loads automatically
     * (no filter click) when the Activities page opens: every activity with
     * a follow-up in the past or within the next 24h.
     */
    @Transactional(readOnly = true)
    public Page<Activity> listDueFollowUps(Pageable pageable) {
        Instant windowEnd = Instant.now().plus(Duration.ofHours(24));
        Specification<Activity> spec = Specification.where(activityAccessService.activityVisibility())
                .and((root, query, cb) -> cb.isNotNull(root.get("followUpAt")))
                .and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("followUpAt"), windowEnd));
        return activityRepository.findAll(spec, pageable);
    }

    public Map<String, Object> summary() {
        Specification<Activity> visible = activityAccessService.activityVisibility();
        long total = activityRepository.count(visible);
        long completed = activityRepository.count(Specification.where(visible)
                .and((root, query, cb) -> cb.equal(root.get("status"), ActivityStatus.COMPLETED)));
        long missed = activityRepository.count(Specification.where(visible)
                .and((root, query, cb) -> cb.equal(root.get("status"), ActivityStatus.MISSED)));
        long dueToday = activityRepository.count(Specification.where(visible)
                .and((root, query, cb) -> cb.isNotNull(root.get("followUpAt")))
                .and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("followUpAt"),
                        Instant.now().plus(Duration.ofHours(24)))));

        Specification<Activity> completedSpec = Specification.where(visible)
                .and((root, query, cb) -> cb.equal(root.get("status"), ActivityStatus.COMPLETED))
                .and((root, query, cb) -> root.get("durationSeconds").isNotNull());
        double avgDurationSeconds = activityRepository.findAll(completedSpec).stream()
                .mapToInt(Activity::getDurationSeconds).average().orElse(0.0);

        return Map.of("total", total, "completed", completed, "missed", missed,
                "dueToday", dueToday, "avgDurationSeconds", avgDurationSeconds);
    }
}