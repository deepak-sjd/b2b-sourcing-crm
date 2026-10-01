import { apiClient } from "./client";
import type { ApiResponse, PageResponse } from "@/types/api";
import type {
  QuotationListFilters,
  QuotationRequest,
  QuotationResponse,
  QuotationStatus,
  QuotationSummary,
} from "@/types/quotation";

export const quotationsApi = {
  async list(filters: QuotationListFilters): Promise<PageResponse<QuotationResponse>> {
    const { data } = await apiClient.get<ApiResponse<PageResponse<QuotationResponse>>>("/quotations", {
      params: {
        q: filters.q || undefined,
        customerId: filters.customerId,
        status: filters.status,
        page: filters.page ?? 0,
        size: filters.size ?? 20,
      },
    });
    return data.data;
  },

  async byCustomer(customerId: string, page = 0, size = 10): Promise<PageResponse<QuotationResponse>> {
    const { data } = await apiClient.get<ApiResponse<PageResponse<QuotationResponse>>>(
      `/quotations/by-customer/${customerId}`,
      { params: { page, size } },
    );
    return data.data;
  },

  async getById(id: string): Promise<QuotationResponse> {
    const { data } = await apiClient.get<ApiResponse<QuotationResponse>>(`/quotations/${id}`);
    return data.data;
  },

  async create(payload: QuotationRequest): Promise<QuotationResponse> {
    const { data } = await apiClient.post<ApiResponse<QuotationResponse>>("/quotations", payload);
    return data.data;
  },

  async update(id: string, payload: QuotationRequest): Promise<QuotationResponse> {
    const { data } = await apiClient.put<ApiResponse<QuotationResponse>>(`/quotations/${id}`, payload);
    return data.data;
  },

  async changeStatus(id: string, status: QuotationStatus): Promise<QuotationResponse> {
    const { data } = await apiClient.patch<ApiResponse<QuotationResponse>>(
      `/quotations/${id}/status`,
      null,
      { params: { status } },
    );
    return data.data;
  },

  async remove(id: string): Promise<void> {
    await apiClient.delete<ApiResponse<null>>(`/quotations/${id}`);
  },

  async summary(): Promise<QuotationSummary> {
    const { data } = await apiClient.get<ApiResponse<QuotationSummary>>("/quotations/summary");
    return data.data;
  },
};
