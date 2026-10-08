import type { EntityLeadRef, EntityUserRef } from "./common";

export type ActivityType = "CALL" | "MEETING" | "SITE_VISIT" | "EMAIL" | "WHATSAPP" | "OTHER";
export const ACTIVITY_TYPES: ActivityType[] = ["CALL", "MEETING", "SITE_VISIT", "EMAIL", "WHATSAPP", "OTHER"];
export const ACTIVITY_TYPE_LABELS: Record<ActivityType, string> = {
  CALL: "Call", MEETING: "Meeting", SITE_VISIT: "Site visit", EMAIL: "Email", WHATSAPP: "WhatsApp", OTHER: "Other",
};

export type ActivityDirection = "INBOUND" | "OUTBOUND";
export const ACTIVITY_DIRECTIONS: ActivityDirection[] = ["INBOUND", "OUTBOUND"];

export type ActivityStatus = "SCHEDULED" | "COMPLETED" | "MISSED" | "NO_ANSWER" | "BUSY";
export const ACTIVITY_STATUSES: ActivityStatus[] = ["SCHEDULED", "COMPLETED", "MISSED", "NO_ANSWER", "BUSY"];

export type ActivitySentiment = "POSITIVE" | "NEUTRAL" | "NEGATIVE";
export const ACTIVITY_SENTIMENTS: ActivitySentiment[] = ["POSITIVE", "NEUTRAL", "NEGATIVE"];

// Mirrors com.doctutorials.crm.activity.CallOutcome - exact PRD §7 taxonomy.
export type CallOutcome =
  | "CONNECTED" | "NOT_CONNECTED" | "BUSY" | "CALL_BACK_REQUIRED" | "INTERESTED" | "NOT_INTERESTED"
  | "REQUIREMENT_RECEIVED" | "MEETING_REQUIRED" | "WRONG_NUMBER" | "OTHER";
export const CALL_OUTCOMES: CallOutcome[] = [
  "CONNECTED", "NOT_CONNECTED", "BUSY", "CALL_BACK_REQUIRED", "INTERESTED", "NOT_INTERESTED",
  "REQUIREMENT_RECEIVED", "MEETING_REQUIRED", "WRONG_NUMBER", "OTHER",
];
export const CALL_OUTCOME_LABELS: Record<CallOutcome, string> = {
  CONNECTED: "Connected", NOT_CONNECTED: "Not connected", BUSY: "Busy",
  CALL_BACK_REQUIRED: "Call back required", INTERESTED: "Interested", NOT_INTERESTED: "Not interested",
  REQUIREMENT_RECEIVED: "Requirement received", MEETING_REQUIRED: "Meeting required",
  WRONG_NUMBER: "Wrong number", OTHER: "Other",
};

// Mirrors com.doctutorials.crm.activity.CustomerInterest
export type CustomerInterest = "LOW" | "MEDIUM" | "HIGH";
export const CUSTOMER_INTERESTS: CustomerInterest[] = ["LOW", "MEDIUM", "HIGH"];

// Mirrors com.doctutorials.crm.activity.MeetingStatus (PRD §9)
export type MeetingStatus = "SCHEDULED" | "COMPLETED" | "RESCHEDULED" | "CANCELLED" | "NO_SHOW";
export const MEETING_STATUSES: MeetingStatus[] = ["SCHEDULED", "COMPLETED", "RESCHEDULED", "CANCELLED", "NO_SHOW"];
export const MEETING_STATUS_LABELS: Record<MeetingStatus, string> = {
  SCHEDULED: "Scheduled", COMPLETED: "Completed", RESCHEDULED: "Rescheduled", CANCELLED: "Cancelled", NO_SHOW: "No show",
};

// Mirrors com.doctutorials.crm.activity.MeetingType
export type MeetingType = "IN_PERSON" | "VIDEO" | "PHONE" | "SITE_VISIT";
export const MEETING_TYPES: MeetingType[] = ["IN_PERSON", "VIDEO", "PHONE", "SITE_VISIT"];
export const MEETING_TYPE_LABELS: Record<MeetingType, string> = {
  IN_PERSON: "In person", VIDEO: "Video", PHONE: "Phone", SITE_VISIT: "Site visit",
};

// Mirrors activity.dto.ActivityResponse (was raw Activity entity - see
// ActivityService's assumption #3 for why that changed; same field
// names/shapes so existing `.lead`/`.loggedBy` reads keep working).
export interface Activity {
  id: string;
  lead: EntityLeadRef;
  loggedBy: EntityUserRef | null;
  taggedResource: EntityUserRef | null;
  activityType: ActivityType;
  direction: ActivityDirection | null;
  status: ActivityStatus;
  scheduledAt: string;
  contactPerson: string | null;
  location: string | null;
  durationSeconds: number | null;
  sentiment: ActivitySentiment | null;
  recordingUrl: string | null;
  requirements: string | null;
  notes: string | null;
  followUpAt: string | null;
  // Call fields (PRD §7)
  callOutcome: CallOutcome | null;
  customerResponse: string | null;
  customerInterest: CustomerInterest | null;
  nextAction: string | null;
  // Meeting fields (PRD §9)
  meetingStatus: MeetingStatus | null;
  meetingType: MeetingType | null;
  agenda: string | null;
  createdAt: string;
  updatedAt: string;
}

// Mirrors ActivityService.LogActivityRequest
export interface LogActivityRequest {
  leadId: string;
  activityType: ActivityType;
  direction?: ActivityDirection | null;
  status: ActivityStatus;
  scheduledAt?: string | null;
  contactPerson?: string | null;
  location?: string | null;
  durationSeconds?: number | null;
  sentiment?: ActivitySentiment | null;
  requirements?: string | null;
  notes?: string | null;
  followUpAt?: string | null;
  taggedResourceEmail?: string | null;
  callOutcome?: CallOutcome | null;
  customerResponse?: string | null;
  customerInterest?: CustomerInterest | null;
  nextAction?: string | null;
  /** "Is this a real lead?" - undefined/null means not answered yet; the
   *  backend only acts (scrap or follow-up task) when this is explicitly set. */
  realLead?: boolean | null;
  meetingType?: MeetingType | null;
  agenda?: string | null;
}

// Mirrors activity.dto.UpdateActivityRequest
export interface UpdateActivityRequest {
  direction?: ActivityDirection | null;
  status?: ActivityStatus | null;
  scheduledAt?: string | null;
  contactPerson?: string | null;
  location?: string | null;
  durationSeconds?: number | null;
  sentiment?: ActivitySentiment | null;
  requirements?: string | null;
  notes?: string | null;
  followUpAt?: string | null;
  callOutcome?: CallOutcome | null;
  customerResponse?: string | null;
  customerInterest?: CustomerInterest | null;
  nextAction?: string | null;
  meetingType?: MeetingType | null;
  agenda?: string | null;
}

// Mirrors activity.dto.MeetingStatusRequest
export interface MeetingStatusRequest {
  status: MeetingStatus;
  newScheduledAt?: string | null;
  notes?: string | null;
}

export interface ActivitySummary {
  total: number; completed: number; missed: number; dueToday: number; avgDurationSeconds: number;
}

export interface ActivityListFilters {
  type?: ActivityType; status?: ActivityStatus; callOutcome?: CallOutcome; meetingStatus?: MeetingStatus;
  fromDate?: string; page?: number; size?: number;
}