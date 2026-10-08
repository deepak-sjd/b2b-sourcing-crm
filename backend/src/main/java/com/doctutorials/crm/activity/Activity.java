package com.doctutorials.crm.activity;

import com.doctutorials.crm.common.BaseEntity;
import com.doctutorials.crm.lead.Lead;
import com.doctutorials.crm.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Generalized from the original Call entity (see V7 migration) to cover
 * every kind of customer-facing touchpoint, not just phone calls - this is
 * what the frontend's Timeline/Emails/WhatsApp/Meetings tabs were always
 * meant to be backed by (they shipped as ComingSoon stubs ahead of this
 * table existing).
 */
@Entity
@Table(name = "activities", indexes = {
        @Index(name = "idx_activities_lead_id", columnList = "lead_id"),
        @Index(name = "idx_activities_created_at", columnList = "createdAt"),
        @Index(name = "idx_activities_status", columnList = "status"),
        @Index(name = "idx_activities_direction", columnList = "direction"),
        @Index(name = "idx_activities_type", columnList = "activityType"),
        @Index(name = "idx_activities_follow_up_at", columnList = "followUpAt"),
        @Index(name = "idx_activities_call_outcome", columnList = "callOutcome"),
        @Index(name = "idx_activities_meeting_status", columnList = "meetingStatus")
})
@Getter
@Setter
public class Activity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lead_id")
    private Lead lead;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "logged_by_id")
    private User loggedBy;

    /** Someone else looped in for help on this specific touchpoint (e.g. a
     *  subject-matter counselor joining a call) - independent of the lead's
     *  owner/manager. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tagged_resource_id")
    private User taggedResource;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActivityType activityType = ActivityType.CALL;

    /** Only meaningful for CALL/EMAIL/WHATSAPP - null for MEETING/SITE_VISIT/OTHER. */
    @Enumerated(EnumType.STRING)
    private ActivityDirection direction;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActivityStatus status;

    /** When the touchpoint happened (or is scheduled for) - distinct from
     *  createdAt, which is purely "when the record was saved". */
    @Column(nullable = false)
    private Instant scheduledAt;

    /** Overrides the lead's own contact when whoever picked up wasn't the
     *  lead themself (e.g. a parent/guardian). Null means "the lead". */
    private String contactPerson;

    /** Free text, mainly used for MEETING/SITE_VISIT. */
    private String location;

    private Integer durationSeconds;

    @Enumerated(EnumType.STRING)
    private ActivitySentiment sentiment;

    private String recordingUrl;

    /** What the customer asked for/needs - kept distinct from `notes` (the
     *  rep's own account of the interaction). Doubles as "requirements
     *  discussed" for a MEETING. */
    @Column(length = 2000)
    private String requirements;

    /** Doubles as "meeting notes" for a MEETING (PRD §9). */
    @Column(length = 2000)
    private String notes;

    /** Next time this lead needs a touch. Nullable. When set, ActivityService
     *  mirrors it onto Lead.nextFollowUpAt. Doubles as a meeting's "next
     *  follow-up date" (PRD §9). */
    private Instant followUpAt;

    // --- Call fields (PRD §7) - only meaningful when activityType == CALL ---

    @Enumerated(EnumType.STRING)
    private CallOutcome callOutcome;

    @Column(length = 2000)
    private String customerResponse;

    @Enumerated(EnumType.STRING)
    private CustomerInterest customerInterest;

    @Column(length = 500)
    private String nextAction;

    // --- Meeting fields (PRD §9) - only meaningful when activityType == MEETING ---

    @Enumerated(EnumType.STRING)
    private MeetingStatus meetingStatus;

    @Enumerated(EnumType.STRING)
    private MeetingType meetingType;

    @Column(length = 2000)
    private String agenda;
}