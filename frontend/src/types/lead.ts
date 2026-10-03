// Mirrors com.doctutorials.crm.lead.LeadStage — order matters for the pipeline UI.
// Generic sales terminology now (not coaching-specific) - a given deployment can
// relabel these in LEAD_STAGE_LABELS below (e.g. "Enrolled"/"Dropped" for a
// coaching institute) without touching the underlying stored values.
export type LeadStage =
  | "NEW"
  | "CONTACTED"
  | "QUALIFIED"
  | "OPPORTUNITY"
  | "WON"
  | "LOST";

export const LEAD_STAGES: LeadStage[] = [
  "NEW",
  "CONTACTED",
  "QUALIFIED",
  "OPPORTUNITY",
  "WON",
  "LOST",
];

export const LEAD_STAGE_LABELS: Record<LeadStage, string> = {
  NEW: "New",
  CONTACTED: "Contacted",
  QUALIFIED: "Qualified",
  OPPORTUNITY: "Opportunity",
  WON: "Won",
  LOST: "Lost",
};

/** Stages a lead cannot be moved out of once reached (backend-enforced guard). */
export const CLOSED_STAGES: LeadStage[] = ["WON", "LOST"];

// Mirrors com.doctutorials.crm.lead.Priority
export type Priority = "LOW" | "MEDIUM" | "HIGH" | "URGENT";
export const PRIORITIES: Priority[] = ["LOW", "MEDIUM", "HIGH", "URGENT"];

// Mirrors com.doctutorials.crm.lead.BuyingIntent
export type BuyingIntent = "COLD" | "COOL" | "WARM" | "HOT";

// Mirrors com.doctutorials.crm.lead.LostReason — exact taxonomy from PRD §14.2.
export type LostReason =
  | "PRICE_TOO_HIGH"
  | "SUPPLIER_PRICE_TOO_HIGH"
  | "CUSTOMER_SELECTED_ANOTHER_SUPPLIER"
  | "CUSTOMER_STOPPED_RESPONDING"
  | "LATE_RESPONSE"
  | "DELAY_FROM_OUR_SIDE"
  | "REQUIREMENT_CANCELLED"
  | "REQUIREMENT_POSTPONED"
  | "REQUIREMENT_CHANGED"
  | "CUSTOMER_NOT_INTERESTED"
  | "INCORRECT_REQUIREMENT"
  | "OTHER";

export const LOST_REASONS: LostReason[] = [
  "PRICE_TOO_HIGH",
  "SUPPLIER_PRICE_TOO_HIGH",
  "CUSTOMER_SELECTED_ANOTHER_SUPPLIER",
  "CUSTOMER_STOPPED_RESPONDING",
  "LATE_RESPONSE",
  "DELAY_FROM_OUR_SIDE",
  "REQUIREMENT_CANCELLED",
  "REQUIREMENT_POSTPONED",
  "REQUIREMENT_CHANGED",
  "CUSTOMER_NOT_INTERESTED",
  "INCORRECT_REQUIREMENT",
  "OTHER",
];

export const LOST_REASON_LABELS: Record<LostReason, string> = {
  PRICE_TOO_HIGH: "Price too high",
  SUPPLIER_PRICE_TOO_HIGH: "Supplier price too high",
  CUSTOMER_SELECTED_ANOTHER_SUPPLIER: "Customer selected another supplier",
  CUSTOMER_STOPPED_RESPONDING: "Customer stopped responding",
  LATE_RESPONSE: "Late response",
  DELAY_FROM_OUR_SIDE: "Delay from our side",
  REQUIREMENT_CANCELLED: "Requirement cancelled",
  REQUIREMENT_POSTPONED: "Requirement postponed",
  REQUIREMENT_CHANGED: "Requirement changed",
  CUSTOMER_NOT_INTERESTED: "Customer not interested",
  INCORRECT_REQUIREMENT: "Incorrect requirement",
  OTHER: "Other",
};

// Mirrors com.doctutorials.crm.lead.ScrapReason
export type ScrapReason =
  | "NOT_SUITABLE"
  | "REQUIREMENT_MISMATCH"
  | "INACTIVE"
  | "DUPLICATE"
  | "INCORRECT_DATA"
  | "NO_LONGER_WORKING_ON"
  | "OTHER";

export const SCRAP_REASONS: ScrapReason[] = [
  "NOT_SUITABLE",
  "REQUIREMENT_MISMATCH",
  "INACTIVE",
  "DUPLICATE",
  "INCORRECT_DATA",
  "NO_LONGER_WORKING_ON",
  "OTHER",
];

export const SCRAP_REASON_LABELS: Record<ScrapReason, string> = {
  NOT_SUITABLE: "Not suitable",
  REQUIREMENT_MISMATCH: "Doesn't match requirement",
  INACTIVE: "Inactive",
  DUPLICATE: "Duplicate",
  INCORRECT_DATA: "Incorrect data",
  NO_LONGER_WORKING_ON: "No longer working on",
  OTHER: "Other",
};

// Mirrors com.doctutorials.crm.lead.dto.LeadResponse
export interface LeadResponse {
  id: string;
  fullName: string;
  phone: string | null;
  email: string | null;
  city: string | null;
  interest: string | null;
  budget: number | null;
  estimatedValue: number | null;
  stage: LeadStage;
  priority: Priority;
  score: number;
  buyingIntent: BuyingIntent | null;
  convertProbability: number | null;
  source: string | null;
  ownerName: string | null;
  lastTouchpointAt: string | null;
  nextFollowUpAt: string | null;
  daysSinceLastTouchpoint: number;
  atRisk: boolean;
  aiSummary: string | null;
  /** Single short, actionable recommendation (null once a lead is WON/LOST). */
  nextBestAction: string | null;
  aiSummaryGeneratedAt: string | null;
  // Won/Lost closure
  closedAt: string | null;
  finalValue: number | null;
  closureRemarks: string | null;
  lostReason: LostReason | null;
  lostComment: string | null;
  // Scrap lifecycle
  scrapped: boolean;
  scrapReason: ScrapReason | null;
  scrapRemarks: string | null;
  scrappedAt: string | null;
}

// Mirrors com.doctutorials.crm.lead.dto.LeadRequest
export interface LeadRequest {
  fullName: string;
  phone?: string | null;
  email?: string | null;
  city?: string | null;
  interest?: string | null;
  budget?: number | null;
  priority?: Priority;
  source?: string | null;
  campaign?: string | null;
  ownerId?: string | null;
}

export interface LeadListFilters {
  stage?: LeadStage;
  ownerId?: string;
  page?: number;
  size?: number;
}

// Mirrors com.doctutorials.crm.lead.dto.MarkWonRequest
export interface MarkWonRequest {
  finalValue?: number | null;
  remarks?: string | null;
}

// Mirrors com.doctutorials.crm.lead.dto.MarkLostRequest
export interface MarkLostRequest {
  lostReason: LostReason;
  comment?: string | null;
}

// Mirrors com.doctutorials.crm.lead.dto.ScrapRequest
export interface ScrapRequest {
  scrapReason: ScrapReason;
  remarks?: string | null;
}
