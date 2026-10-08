package com.doctutorials.crm.activity.dto;

import com.doctutorials.crm.activity.*;

import java.time.Instant;

/** PUT /activities/{id} - editing a not-yet-happened activity (mainly a
 *  scheduled meeting's agenda/time/type before it happens). leadId can't
 *  change here; create a new activity instead. */
public record UpdateActivityRequest(
        ActivityDirection direction,
        ActivityStatus status,
        Instant scheduledAt,
        String contactPerson,
        String location,
        Integer durationSeconds,
        ActivitySentiment sentiment,
        String requirements,
        String notes,
        Instant followUpAt,
        CallOutcome callOutcome,
        String customerResponse,
        CustomerInterest customerInterest,
        String nextAction,
        MeetingType meetingType,
        String agenda
) {}