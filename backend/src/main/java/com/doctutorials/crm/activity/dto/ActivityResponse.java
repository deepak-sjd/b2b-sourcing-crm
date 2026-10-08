package com.doctutorials.crm.activity.dto;

import com.doctutorials.crm.activity.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Fixes a pre-existing bug: the controller used to return Activity entities
 * directly, whose lazy lead/loggedBy/taggedResource throw
 * LazyInitializationException once the (open-in-view: false) transaction
 * that loaded them has closed - i.e. on every list/by-lead/due-follow-ups
 * call. from() is only safe to call inside an open transaction (every
 * ActivityService method that returns this already is one).
 * lead/loggedBy/taggedResource keep the exact field names/shapes the
 * frontend already expects (EntityLeadRef/EntityUserRef).
 */
public record ActivityResponse(
        UUID id,
        LeadRef lead,
        UserRef loggedBy,
        UserRef taggedResource,
        ActivityType activityType,
        ActivityDirection direction,
        ActivityStatus status,
        Instant scheduledAt,
        String contactPerson,
        String location,
        Integer durationSeconds,
        ActivitySentiment sentiment,
        String recordingUrl,
        String requirements,
        String notes,
        Instant followUpAt,
        CallOutcome callOutcome,
        String customerResponse,
        CustomerInterest customerInterest,
        String nextAction,
        MeetingStatus meetingStatus,
        MeetingType meetingType,
        String agenda,
        Instant createdAt,
        Instant updatedAt
) {
    public record LeadRef(UUID id, String fullName, String phone, String email, String stage) {}
    public record UserRef(UUID id, String fullName, String email) {}

    public static ActivityResponse from(Activity a) {
        var lead = a.getLead();
        LeadRef leadRef = new LeadRef(
                lead.getId(), lead.getFullName(), lead.getPhone(), lead.getEmail(),
                lead.getStage() != null ? lead.getStage().name() : null);

        return new ActivityResponse(
                a.getId(), leadRef, toUserRef(a.getLoggedBy()), toUserRef(a.getTaggedResource()),
                a.getActivityType(), a.getDirection(), a.getStatus(), a.getScheduledAt(),
                a.getContactPerson(), a.getLocation(), a.getDurationSeconds(), a.getSentiment(),
                a.getRecordingUrl(), a.getRequirements(), a.getNotes(), a.getFollowUpAt(),
                a.getCallOutcome(), a.getCustomerResponse(), a.getCustomerInterest(), a.getNextAction(),
                a.getMeetingStatus(), a.getMeetingType(), a.getAgenda(),
                a.getCreatedAt(), a.getUpdatedAt());
    }

    private static UserRef toUserRef(com.doctutorials.crm.user.User u) {
        return u == null ? null : new UserRef(u.getId(), u.getFullName(), u.getEmail());
    }
}