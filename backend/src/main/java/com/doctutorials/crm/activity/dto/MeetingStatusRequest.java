package com.doctutorials.crm.activity.dto;

import com.doctutorials.crm.activity.MeetingStatus;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

/** PATCH /activities/{id}/meeting-status. newScheduledAt is required when
 *  status == RESCHEDULED (validated in ActivityService, not here - it's a
 *  cross-field rule). notes optionally appends completion/cancellation notes. */
public record MeetingStatusRequest(
        @NotNull MeetingStatus status,
        Instant newScheduledAt,
        String notes
) {}