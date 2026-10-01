import { apiClient } from "./client";
import type { ApiResponse, PageResponse } from "@/types/api";
import type {
  Requirement,
  RequirementListFilters,
  RequirementRequest,
  RequirementStatus,
  RequirementSummary,
} from "@/types/requirement";

export const requirementsApi = {
  async create(payload: RequirementRequest): Promise<Requirement> {
    const { data } = await apiClient.post<ApiResponse<Requirement>>("/requirements", payload);
    return data.data;
  },

  async update(id: string, payload: RequirementRequest): Promise<Requirement> {
    const { data } = await apiClient.put<ApiResponse<Requirement>>(`/requirements/${id}`, payload);
    return data.data;
  },

  async changeStatus(id: string, status: RequirementStatus): Promise<Requirement> {
    const { data } = await apiClient.patch<ApiResponse<Requirement>>(`/requirements/${id}/status`, null, {
      params: { status },
    });
    return data.data;
  },

  async getById(id: string): Promise<Requirement> {
    const { data } = await apiClient.get<ApiResponse<Requirement>>(`/requirements/${id}`);
    return data.data;
  },

  /** Global, visibility-scoped list — backs the Requirements page. */
  async list(filters: RequirementListFilters): Promise<PageResponse<Requirement>> {
    const { data } = await apiClient.get<ApiResponse<PageResponse<Requirement>>>("/requirements", {
      params: {
        q: filters.q || undefined,
        status: filters.status,
        priority: filters.priority,
        needsSupplier: filters.needsSupplier,
        page: filters.page ?? 0,
        size: filters.size ?? 20,
      },
    });
    return data.data;
  },

  async byLead(leadId: string, page = 0, size = 50): Promise<PageResponse<Requirement>> {
    const { data } = await apiClient.get<ApiResponse<PageResponse<Requirement>>>(
      `/requirements/by-lead/${leadId}`,
      { params: { page, size } },
    );
    return data.data;
  },

  async summary(): Promise<RequirementSummary> {
    const { data } = await apiClient.get<ApiResponse<RequirementSummary>>("/requirements/summary");
    return data.data;
  },
};
