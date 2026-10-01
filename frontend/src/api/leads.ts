import { apiClient } from "./client";
import type { ApiResponse, PageResponse } from "@/types/api";
import type { LeadListFilters, LeadRequest, LeadResponse, LeadStage } from "@/types/lead";

export const leadsApi = {
  async list(filters: LeadListFilters): Promise<PageResponse<LeadResponse>> {
    const { data } = await apiClient.get<ApiResponse<PageResponse<LeadResponse>>>("/leads", {
      params: {
        stage: filters.stage,
        ownerId: filters.ownerId,
        page: filters.page ?? 0,
        size: filters.size ?? 20,
      },
    });
    return data.data;
  },

  async getById(id: string): Promise<LeadResponse> {
    const { data } = await apiClient.get<ApiResponse<LeadResponse>>(`/leads/${id}`);
    return data.data;
  },

  async create(payload: LeadRequest): Promise<LeadResponse> {
    const { data } = await apiClient.post<ApiResponse<LeadResponse>>("/leads", payload);
    return data.data;
  },

  async update(id: string, payload: LeadRequest): Promise<LeadResponse> {
    const { data } = await apiClient.put<ApiResponse<LeadResponse>>(`/leads/${id}`, payload);
    return data.data;
  },

  async changeStage(id: string, stage: LeadStage): Promise<LeadResponse> {
    const { data } = await apiClient.patch<ApiResponse<LeadResponse>>(
      `/leads/${id}/stage`,
      null,
      { params: { stage } },
    );
    return data.data;
  },

  async assign(id: string, ownerId: string): Promise<LeadResponse> {
    const { data } = await apiClient.patch<ApiResponse<LeadResponse>>(
      `/leads/${id}/assign`,
      null,
      { params: { ownerId } },
    );
    return data.data;
  },

  async recordTouchpoint(id: string): Promise<void> {
    await apiClient.post<ApiResponse<null>>(`/leads/${id}/touchpoint`);
  },

  /** Re-runs scoring + AI summary/next-best-action generation on demand. */
  async refreshAiInsight(id: string): Promise<LeadResponse> {
    const { data } = await apiClient.post<ApiResponse<LeadResponse>>(`/leads/${id}/ai/refresh`);
    return data.data;
  },

  async archive(id: string): Promise<void> {
    await apiClient.delete<ApiResponse<null>>(`/leads/${id}`);
  },

  async atRisk(): Promise<LeadResponse[]> {
    const { data } = await apiClient.get<ApiResponse<LeadResponse[]>>("/leads/at-risk");
    return data.data;
  },
};
