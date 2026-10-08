import type { LeadStage } from "./lead";

// Mirrors ReportService.GroupBy
export type ReportGroupBy = "NONE" | "STAGE" | "SOURCE" | "CITY";
export const REPORT_GROUP_BY_OPTIONS: { value: ReportGroupBy; label: string }[] = [
  { value: "NONE", label: "None (Raw Data)" },
  { value: "STAGE", label: "Stage" },
  { value: "SOURCE", label: "Source" },
  { value: "CITY", label: "City" },
];

/** ReportService.runLeadReport returns Map<String, Long> — group label -> count. */
export type LeadReportResult = Record<string, number>;

// Mirrors ReportService.CounselorPerformance
export interface CounselorPerformance {
  ownerId: string;
  ownerName: string;
  assigned: number;
  converted: number;
  conversionRatePercent: number;
}

// Mirrors AiReportingAssistantService.AiAnswer
export interface AiAnswer {
  narrative: string;
  data: LeadReportResult | CounselorPerformance[] | unknown;
}

// Mirrors ReportService.DashboardSummary
export interface DashboardSummary {
  totalLeads: number;
  openLeads: number;
  wonLeads: number;
  lostLeads: number;
  hotLeads: number;
  openPipelineValue: number;
  avgScore: number;
}

/** Every PRD §15 report endpoint accepts the same optional window — omit both for "last 30 days". */
export interface ReportDateRange {
  from?: string; // ISO yyyy-MM-dd
  to?: string; // ISO yyyy-MM-dd
}

// Mirrors ReportService.FunnelStageBucket / FunnelOwnerBucket / FunnelReport
export interface FunnelStageBucket {
  stage: LeadStage;
  count: number;
  value: number;
}
export interface FunnelOwnerBucket {
  ownerId: string | null;
  ownerName: string;
  count: number;
  value: number;
}
export interface FunnelReport {
  byStage: FunnelStageBucket[];
  byOwner: FunnelOwnerBucket[];
}

// Mirrors ReportService.reasonGroup(LostReason) — PRD §14.2's 5 analysis groups.
export type LostReasonGroup = "PRICE_RELATED" | "SUPPLIER_PRICING" | "CUSTOMER_RESPONSE" | "INTERNAL_DELAY" | "OTHER";
export const LOST_REASON_GROUP_LABELS: Record<LostReasonGroup, string> = {
  PRICE_RELATED: "Price-related",
  SUPPLIER_PRICING: "Supplier pricing",
  CUSTOMER_RESPONSE: "Customer response",
  INTERNAL_DELAY: "Internal delay",
  OTHER: "Other",
};

// Mirrors ReportService.LostReasonBucket / LostGroupBucket / LostAnalysis
export interface LostReasonBucket {
  reason: string; // LostReason name
  count: number;
  value: number;
}
export interface LostGroupBucket {
  group: LostReasonGroup;
  count: number;
  value: number;
}
export interface LostAnalysis {
  totalLostCount: number;
  totalLostValue: number;
  byReason: LostReasonBucket[];
  byGroup: LostGroupBucket[];
}

// Mirrors ReportService.SourceBucket
export interface SourceBucket {
  source: string;
  total: number;
  newCount: number;
  active: number;
  won: number;
}

// Mirrors ReportService.SupplierBucket / SupplierReport
export interface SupplierBucket {
  supplierId: string;
  supplierName: string;
  enquiries: number;
  replies: number;
  chosen: number;
}
export interface SupplierReport {
  activeSuppliers: number;
  enquiriesSent: number;
  pendingReplies: number;
  repliesReceived: number;
  bySupplier: SupplierBucket[];
}

// Mirrors ReportService.QuotationReport
export interface QuotationReport {
  generated: number;
  pending: number;
  accepted: number;
  acceptedValue: number;
  rejected: number;
  expired: number;
}

// Mirrors ReportService.TeamPerformanceRow
export interface TeamPerformanceRow {
  userId: string;
  userName: string;
  assignedLeads: number;
  callsCompleted: number;
  meetingsCompleted: number;
  followUpsCompleted: number;
  pendingTasks: number;
  quotationsHandled: number;
  won: number;
  lost: number;
}

// Mirrors ReportService.OverviewReport
export interface OverviewReport {
  totalCustomers: number;
  newCustomers: number;
  activeCustomers: number;
  leadsNeedingFollowUp: number;
  scrapCount: number;
  customersContacted: number;
  customersQualified: number;
  meetingsScheduled: number;
}