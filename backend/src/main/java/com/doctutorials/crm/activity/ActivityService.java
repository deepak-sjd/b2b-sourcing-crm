package com.doctutorials.crm.activity;

import com.doctutorials.crm.activity.dto.ActivityResponse;
import com.doctutorials.crm.activity.dto.MeetingStatusRequest;
import com.doctutorials.crm.activity.dto.UpdateActivityRequest;
import com.doctutorials.crm.audit.AuditAction;
import com.doctutorials.crm.audit.AuditService;
import com.doctutorials.crm.common.exception.BadRequestException;
import com.doctutorials.crm.common.exception.ResourceNotFoundException;
import com.doctutorials.crm.lead.Lead;
import com.doctutorials.crm.lead.LeadRepository;
import com.doctutorials.crm.lead.LeadScoringService;
import com.doctutorials.crm.lead.LeadService;
import com.doctutorials.crm.lead.LeadStage;
import com.doctutorials.crm.lead.ScrapReason;
import com.doctutorials.crm.task.TaskService;
import com.doctutorials.crm.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ActivityService {

    private static final List<LeadStage> FORWARD_STAGES =
            List.of(LeadStage.NEW, LeadStage.CONTACTED, LeadStage.QUALIFIED, LeadStage.OPPORTUNITY);

    private final ActivityRepository activityRepository;
    private final LeadRepository leadRepository;
    private final LeadService leadService;
    private final LeadScoringService scoringService;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final ActivityAccessService activityAccessService;
    private final TaskService taskService;

    public record LogActivityRequest(
            UUID leadId, ActivityType activityType, ActivityDirection direction, ActivityStatus status,
            Instant scheduledAt, String contactPerson, String location, Integer durationSeconds,
            ActivitySentiment sentiment, String requirements, String notes, Instant followUpAt,
            String taggedResourceEmail,
            // Call fields (PRD §7)
            CallOutcome callOutcome, String customerResponse, CustomerInterest customerInterest, String nextAction,
            /** Null = no lead-check happened. true/false = explicit answer to
             *  "Is this a real lead?" - never inferred, only ever acted on when set. */
            Boolean realLead,
            // Meeting fields (PRD §9)
            MeetingType meetingType, String agenda) {}

    @Transactional
    public ActivityResponse logActivity(LogActivityRequest req) {
        // getById (not the repository directly) throws 404 if the caller can't
        // see this lead - same rule CallService used.
        Lead lead = leadService.getById(req.leadId());

        Activity activity = new Activity();
        activity.setLead(lead);
        ActivityType type = req.activityType() != null ? req.activityType() : ActivityType.CALL;
        activity.setActivityType(type);
        activity.setDirection(req.direction());
        activity.setStatus(req.status());
        activity.setScheduledAt(req.scheduledAt() != null ? req.scheduledAt() : Instant.now());
        activity.setContactPerson(blankToNull(req.contactPerson()));
        activity.setLocation(blankToNull(req.location()));
        activity.setDurationSeconds(req.durationSeconds());
        activity.setSentiment(req.sentiment());
        activity.setRequirements(blankToNull(req.requirements()));
        activity.setNotes(blankToNull(req.notes()));

        if (type == ActivityType.CALL) {
            applyCallFields(activity, req.status(), req.callOutcome(), req.customerResponse(),
                    req.customerInterest(), req.nextAction());
        } else if (type == ActivityType.MEETING) {
            activity.setMeetingType(req.meetingType());
            activity.setAgenda(blankToNull(req.agenda()));
            // A brand-new meeting always starts SCHEDULED; status transitions
            // from here on go through changeMeetingStatus, never this method.
            activity.setMeetingStatus(MeetingStatus.SCHEDULED);
        }

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

        // Funnel + lead-check only apply to a completed call - a SCHEDULED
        // call being logged (rare, but the type allows it) has no outcome yet.
        if (type == ActivityType.CALL && saved.getStatus() == ActivityStatus.COMPLETED) {
            advanceStageIfBehind(lead, LeadStage.CONTACTED, saved.getCallOutcome() == CallOutcome.CONNECTED);
            applyLeadCheck(lead, saved, req.realLead(), followUpAt);
        }
        if (type == ActivityType.MEETING) {
            // Scheduling OR completing a meeting both count as "a meeting
            // happened/will happen" for funnel purposes.
            advanceStageIfBehind(lead, LeadStage.QUALIFIED, true);
        }

        return ActivityResponse.from(saved);
    }

    @Transactional
    public ActivityResponse update(UUID id, UpdateActivityRequest req) {
        Activity activity = load(id);
        if (activity.getMeetingStatus() != null
                && activity.getMeetingStatus() != MeetingStatus.SCHEDULED
                && activity.getMeetingStatus() != MeetingStatus.RESCHEDULED) {
            throw new BadRequestException("A " + activity.getMeetingStatus() + " meeting can't be edited");
        }

        if (req.direction() != null) activity.setDirection(req.direction());
        if (req.status() != null) activity.setStatus(req.status());
        if (req.scheduledAt() != null) activity.setScheduledAt(req.scheduledAt());
        if (req.contactPerson() != null) activity.setContactPerson(blankToNull(req.contactPerson()));
        if (req.location() != null) activity.setLocation(blankToNull(req.location()));
        if (req.durationSeconds() != null) activity.setDurationSeconds(req.durationSeconds());
        if (req.sentiment() != null) activity.setSentiment(req.sentiment());
        if (req.requirements() != null) activity.setRequirements(blankToNull(req.requirements()));
        if (req.notes() != null) activity.setNotes(blankToNull(req.notes()));
        if (req.followUpAt() != null) activity.setFollowUpAt(req.followUpAt());

        if (activity.getActivityType() == ActivityType.CALL) {
            if (req.callOutcome() != null || req.customerResponse() != null
                    || req.customerInterest() != null || req.nextAction() != null) {
                applyCallFields(activity, activity.getStatus(),
                        req.callOutcome() != null ? req.callOutcome() : activity.getCallOutcome(),
                        req.customerResponse(), req.customerInterest(), req.nextAction());
            }
        } else if (activity.getActivityType() == ActivityType.MEETING) {
            if (req.meetingType() != null) activity.setMeetingType(req.meetingType());
            if (req.agenda() != null) activity.setAgenda(blankToNull(req.agenda()));
        }

        Activity saved = activityRepository.save(activity);
        auditService.log("Activity", id, AuditAction.UPDATE, "Activity updated");
        return ActivityResponse.from(saved);
    }

    /**
     * PRD §9 meeting lifecycle. Separate from the generic update() because a
     * status change has its own rules (RESCHEDULED needs a new time and an
     * audit trail of old -> new; the others don't).
     */
    @Transactional
    public ActivityResponse changeMeetingStatus(UUID id, MeetingStatusRequest req) {
        Activity activity = load(id);
        if (activity.getActivityType() != ActivityType.MEETING) {
            throw new BadRequestException("Only a MEETING activity has a meeting status");
        }
        MeetingStatus old = activity.getMeetingStatus();
        if (old == MeetingStatus.COMPLETED || old == MeetingStatus.CANCELLED || old == MeetingStatus.NO_SHOW) {
            throw new BadRequestException("A " + old + " meeting's status can't be changed further");
        }

        if (req.status() == MeetingStatus.RESCHEDULED) {
            if (req.newScheduledAt() == null) {
                throw new BadRequestException("newScheduledAt is required to reschedule a meeting");
            }
            Instant oldWhen = activity.getScheduledAt();
            activity.setScheduledAt(req.newScheduledAt());
            activity.setMeetingStatus(MeetingStatus.RESCHEDULED);
            Activity saved = activityRepository.save(activity);
            auditService.log("Activity", id, AuditAction.UPDATE,
                    "Meeting rescheduled: " + oldWhen + " -> " + req.newScheduledAt());
            return ActivityResponse.from(saved);
        }

        activity.setMeetingStatus(req.status());
        if (req.notes() != null && !req.notes().isBlank()) {
            activity.setNotes(blankToNull(req.notes()));
        }

        Activity saved = activityRepository.save(activity);
        auditService.log("Activity", id, AuditAction.STATUS_CHANGE, "Meeting status " + old + " -> " + req.status());

        if (req.status() == MeetingStatus.COMPLETED) {
            scoringService.markTouched(saved.getLead());
            leadRepository.save(saved.getLead());
        }
        return ActivityResponse.from(saved);
    }

    /** Validates CALL's cross-field rule (outcome required once COMPLETED)
     *  and sets the four call-specific fields. Shared by create and update. */
    private void applyCallFields(Activity activity, ActivityStatus status, CallOutcome outcome,
            String customerResponse, CustomerInterest interest, String nextAction) {
        if (status == ActivityStatus.COMPLETED && outcome == null) {
            throw new BadRequestException("callOutcome is required once a call is marked COMPLETED");
        }
        activity.setCallOutcome(outcome);
        if (customerResponse != null) activity.setCustomerResponse(blankToNull(customerResponse));
        if (interest != null) activity.setCustomerInterest(interest);
        if (nextAction != null) activity.setNextAction(blankToNull(nextAction));
    }

    /**
     * PRD §7's "lead check" + §14.3 Scrap / follow-up task. Only acts when
     * realLead is explicitly true or false - null means the rep hasn't
     * answered that yet, and nothing automatic happens (no silent scrap, no
     * silent task).
     */
    private void applyLeadCheck(Lead lead, Activity call, Boolean realLead, Instant followUpAt) {
        if (realLead == null) return;

        if (!realLead) {
            ScrapReason reason = switch (call.getCallOutcome()) {
                case WRONG_NUMBER -> ScrapReason.INCORRECT_DATA;
                case NOT_INTERESTED -> ScrapReason.NOT_SUITABLE;
                default -> ScrapReason.NOT_SUITABLE;
            };
            String remarks = "From call outcome " + call.getCallOutcome()
                    + (call.getNotes() != null ? ": " + call.getNotes() : "");
            leadService.moveToScrap(lead.getId(), reason, remarks);
            return;
        }

        if (call.getCallOutcome() == CallOutcome.MEETING_REQUIRED || call.getCallOutcome() == CallOutcome.INTERESTED) {
            taskService.create(
                    lead.getId(),
                    "Follow up: " + call.getCallOutcome().name().replace('_', ' ').toLowerCase(),
                    call.getNextAction() != null ? call.getNextAction() : call.getNotes(),
                    lead.getOwner() != null ? lead.getOwner().getId() : null,
                    followUpAt != null ? followUpAt : Instant.now().plus(Duration.ofDays(1)));
        }
    }

    /** Moves a lead forward to `target` only if it's currently behind it in
     *  FORWARD_STAGES, and only if `trigger` is true. Never touches WON/LOST
     *  (changeStage itself would reject that - this just avoids calling it
     *  pointlessly) and never moves backwards (the index comparison is the
     *  whole point of this method). */
    private void advanceStageIfBehind(Lead lead, LeadStage target, boolean trigger) {
        if (!trigger) return;
        int currentIdx = FORWARD_STAGES.indexOf(lead.getStage());
        int targetIdx = FORWARD_STAGES.indexOf(target);
        if (currentIdx < 0 || targetIdx < 0) return; // WON/LOST or unknown - leave it alone
        if (currentIdx < targetIdx) {
            leadService.changeStage(lead.getId(), target);
        }
    }

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

    /** Visibility-checked fetch; call only inside a transaction. */
    private Activity load(UUID id) {
        Activity activity = activityRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Activity", id));
        leadService.getById(activity.getLead().getId());
        return activity;
    }

    @Transactional(readOnly = true)
    public ActivityResponse getById(UUID id) {
        return ActivityResponse.from(load(id));
    }

    @Transactional(readOnly = true)
    public Page<ActivityResponse> listByLead(UUID leadId, ActivityType type, Pageable pageable) {
        leadService.getById(leadId);
        Specification<Activity> spec = (root, query, cb) -> cb.equal(root.get("lead").get("id"), leadId);
        if (type != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("activityType"), type));
        return activityRepository.findAll(spec, pageable).map(ActivityResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<ActivityResponse> listAll(ActivityType type, ActivityStatus status, Pageable pageable) {
        Specification<Activity> spec = Specification.where(activityAccessService.activityVisibility());
        if (type != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("activityType"), type));
        if (status != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        return activityRepository.findAll(spec, pageable).map(ActivityResponse::from);
    }

    /**
     * AUTOMATION #2 - backs a "Due today" panel that loads automatically
     * (no filter click) when the Activities page opens: every activity with
     * a follow-up in the past or within the next 24h.
     */
    @Transactional(readOnly = true)
    public Page<ActivityResponse> listDueFollowUps(Pageable pageable) {
        Instant windowEnd = Instant.now().plus(Duration.ofHours(24));
        Specification<Activity> spec = Specification.where(activityAccessService.activityVisibility())
                .and((root, query, cb) -> cb.isNotNull(root.get("followUpAt")))
                .and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("followUpAt"), windowEnd));
        return activityRepository.findAll(spec, pageable).map(ActivityResponse::from);
    }

    @Transactional(readOnly = true)
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