import { apiClient } from "./client";
import type { ApiResponse, PageResponse } from "@/types/api";
import type {
  LeadListFilters,
  LeadRequest,
  LeadResponse,
  LeadStage,
  MarkLostRequest,
  MarkWonRequest,
  ScrapRequest,
} from "@/types/lead";

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

  /** PRD §14.3 Scrap Records list - deliberately separate from list(), which
   *  never includes scrapped leads. */
  async listScrapped(page = 0, size = 20): Promise<PageResponse<LeadResponse>> {
    const { data } = await apiClient.get<ApiResponse<PageResponse<LeadResponse>>>("/leads/scrap", {
      params: { page, size },
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

  /** Only the four open-pipeline stages - the backend rejects WON/LOST here
   *  and directs the caller to markWon/markLost instead. */
  async changeStage(id: string, stage: LeadStage): Promise<LeadResponse> {
    const { data } = await apiClient.patch<ApiResponse<LeadResponse>>(
      `/leads/${id}/stage`,
      null,
      { params: { stage } },
    );
    return data.data;
  },

  async markWon(id: string, payload: MarkWonRequest): Promise<LeadResponse> {
    const { data } = await apiClient.post<ApiResponse<LeadResponse>>(`/leads/${id}/won`, payload);
    return data.data;
  },

  async markLost(id: string, payload: MarkLostRequest): Promise<LeadResponse> {
    const { data } = await apiClient.post<ApiResponse<LeadResponse>>(`/leads/${id}/lost`, payload);
    return data.data;
  },

  /** Replaces the old archive(id) - same soft-delete-and-keep-history idea,
   *  now with a required reason. See LeadService.moveToScrap. */
  async scrap(id: string, payload: ScrapRequest): Promise<LeadResponse> {
    const { data } = await apiClient.post<ApiResponse<LeadResponse>>(`/leads/${id}/scrap`, payload);
    return data.data;
  },

  async reactivate(id: string): Promise<LeadResponse> {
    const { data } = await apiClient.post<ApiResponse<LeadResponse>>(`/leads/${id}/reactivate`);
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

  async atRisk(): Promise<LeadResponse[]> {
    const { data } = await apiClient.get<ApiResponse<LeadResponse[]>>("/leads/at-risk");
    return data.data;
  },
};
