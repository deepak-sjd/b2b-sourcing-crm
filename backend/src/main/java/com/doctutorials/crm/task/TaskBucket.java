package com.doctutorials.crm.task;

import java.time.Instant;
import java.time.ZoneId;

/** My Day sections. Decided by due date vs. now (not by status) so the list is right even if the sweep job lags. */
public enum TaskBucket {
    OVERDUE, TODAY, UPCOMING;

    /** Start of tomorrow in the user's zone = exclusive upper bound of "today". */
    public static Instant endOfToday(Instant now, ZoneId zone) {
        return now.atZone(zone).toLocalDate().plusDays(1).atStartOfDay(zone).toInstant();
    }

    /** A task without a due date is never overdue or due today - it sits in Upcoming. */
    public static TaskBucket of(Instant due, Instant now, ZoneId zone) {
        if (due == null) return UPCOMING;
        if (due.isBefore(now)) return OVERDUE;
        return due.isBefore(endOfToday(now, zone)) ? TODAY : UPCOMING;
    }
}
