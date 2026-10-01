import { apiClient } from "./client";
import type { ApiResponse } from "@/types/api";
import type {
  AiAnswer,
  CounselorPerformance,
  DashboardSummary,
  LeadReportResult,
  ReportGroupBy,
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
};