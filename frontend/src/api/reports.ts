import { apiClient } from "./client";
import type { ApiResponse } from "@/types/api";
import type {
  AiAnswer,
  CounselorPerformance,
  DashboardSummary,
  FunnelReport,
  LeadReportResult,
  LostAnalysis,
  OverviewReport,
  QuotationReport,
  ReportDateRange,
  ReportGroupBy,
  SourceBucket,
  SupplierReport,
  TeamPerformanceRow,
} from "@/types/report";

export const reportsApi = {
  async dashboardSummary(): Promise<DashboardSummary> {
    const { data } = await apiClient.get<ApiResponse<DashboardSummary>>("/reports/dashboard-summary");
    return data.data;
  },

  async leadReport(groupBy: ReportGroupBy): Promise<LeadReportResult> {
    const { data } = await apiClient.get<ApiResponse<LeadReportResult>>("/reports/leads", {
      params: { groupBy },
    });
    return data.data;
  },

  async counselorPerformance(): Promise<CounselorPerformance[]> {
    const { data } = await apiClient.get<ApiResponse<CounselorPerformance[]>>(
      "/reports/counselor-performance",
    );
    return data.data;
  },

  async ask(question: string): Promise<AiAnswer> {
    const { data } = await apiClient.post<ApiResponse<AiAnswer>>("/reports/ask", { question });
    return data.data;
  },

  async funnel(range: ReportDateRange): Promise<FunnelReport> {
    const { data } = await apiClient.get<ApiResponse<FunnelReport>>("/reports/funnel", { params: range });
    return data.data;
  },

  async lostAnalysis(range: ReportDateRange): Promise<LostAnalysis> {
    const { data } = await apiClient.get<ApiResponse<LostAnalysis>>("/reports/lost-analysis", { params: range });
    return data.data;
  },

  async leadSources(range: ReportDateRange): Promise<SourceBucket[]> {
    const { data } = await apiClient.get<ApiResponse<SourceBucket[]>>("/reports/lead-sources", { params: range });
    return data.data;
  },

  async suppliers(range: ReportDateRange): Promise<SupplierReport> {
    const { data } = await apiClient.get<ApiResponse<SupplierReport>>("/reports/suppliers", { params: range });
    return data.data;
  },

  async quotations(range: ReportDateRange): Promise<QuotationReport> {
    const { data } = await apiClient.get<ApiResponse<QuotationReport>>("/reports/quotations", { params: range });
    return data.data;
  },

  async teamPerformance(range: ReportDateRange): Promise<TeamPerformanceRow[]> {
    const { data } = await apiClient.get<ApiResponse<TeamPerformanceRow[]>>("/reports/team-performance", {
      params: range,
    });
    return data.data;
  },

  async overview(range: ReportDateRange): Promise<OverviewReport> {
    const { data } = await apiClient.get<ApiResponse<OverviewReport>>("/reports/overview", { params: range });
    return data.data;
  },
};