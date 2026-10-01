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

// Mirrors com.doctutorials.crm.activity.Activity entity as serialized by Jackson
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
}

// Mirrors ActivityService.summary() Map keys exactly
export interface ActivitySummary {
  total: number; completed: number; missed: number; dueToday: number; avgDurationSeconds: number;
}

export interface ActivityListFilters {
  type?: ActivityType; status?: ActivityStatus; page?: number; size?: number;
}