package com.doctutorials.crm.activity;

/** SCHEDULED covers a forward-dated activity (e.g. a meeting booked for next
 *  week) that hasn't happened yet - the other four are unchanged from the
 *  original Call-only status set. */
public enum ActivityStatus { SCHEDULED, COMPLETED, MISSED, NO_ANSWER, BUSY }