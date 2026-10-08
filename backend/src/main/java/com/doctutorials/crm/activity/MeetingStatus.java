package com.doctutorials.crm.activity;

/** PRD §9. Distinct from the generic ActivityStatus (SCHEDULED/COMPLETED/...)
 *  used by calls - a meeting's lifecycle has states (RESCHEDULED, NO_SHOW)
 *  that don't apply to a call. Only set when activityType == MEETING. */
public enum MeetingStatus { SCHEDULED, COMPLETED, RESCHEDULED, CANCELLED, NO_SHOW }