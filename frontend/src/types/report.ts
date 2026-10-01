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