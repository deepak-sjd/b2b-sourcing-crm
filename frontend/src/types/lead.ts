// Mirrors com.doctutorials.crm.lead.LeadStage — order matters for the pipeline UI.
// Generic sales terminology now (not coaching-specific) - a given deployment can
// relabel these in LEAD_STAGE_LABELS below (e.g. "Enrolled"/"Dropped" for a
// coaching institute) without touching the underlying stored values.
// MEETING and QUOTATION sit between QUALIFIED and WON/LOST, per the PRD funnel:
// New > Contacted > Qualified > Meeting > Opportunity > Quotation > Won/Lost.
export type LeadStage =
  | "NEW"
  | "CONTACTED"
  | "QUALIFIED"
  | "MEETING"
  | "OPPORTUNITY"
  | "QUOTATION"
  | "WON"
  | "LOST";

export const LEAD_STAGES: LeadStage[] = [
  "NEW",
  "CONTACTED",
  "QUALIFIED",
  "MEETING",
  "OPPORTUNITY",
  "QUOTATION",
  "WON",
  "LOST",
];

export const LEAD_STAGE_LABELS: Record<LeadStage, string> = {
  NEW: "New",
  CONTACTED: "Contacted",
  QUALIFIED: "Qualified",
  MEETING: "Meeting",
  OPPORTUNITY: "Opportunity",
  QUOTATION: "Quotation",
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