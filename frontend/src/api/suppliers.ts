import { apiClient } from "./client";
import type { ApiResponse, PageResponse } from "@/types/api";
import type {
  GstLookupResponse,
  SupplierDuplicateMatch,
  SupplierListFilters,
  SupplierRequest,
  SupplierResponse,
  SupplierStatus,
  SupplierSummary,
} from "@/types/supplier";

export const suppliersApi = {
  async list(filters: SupplierListFilters): Promise<PageResponse<SupplierResponse>> {
    const { data } = await apiClient.get<ApiResponse<PageResponse<SupplierResponse>>>("/suppliers", {
      params: {
        q: filters.q || undefined,
        status: filters.status,
        category: filters.category,
        page: filters.page ?? 0,
        size: filters.size ?? 20,
      },
    });
    return data.data;
  },

  async getById(id: string): Promise<SupplierResponse> {
    const { data } = await apiClient.get<ApiResponse<SupplierResponse>>(`/suppliers/${id}`);
    return data.data;
  },

  async create(payload: SupplierRequest): Promise<SupplierResponse> {
    const { data } = await apiClient.post<ApiResponse<SupplierResponse>>("/suppliers", payload);
    return data.data;
  },

  async update(id: string, payload: SupplierRequest): Promise<SupplierResponse> {
    const { data } = await apiClient.put<ApiResponse<SupplierResponse>>(`/suppliers/${id}`, payload);
    return data.data;
  },

  async changeStatus(id: string, status: SupplierStatus): Promise<SupplierResponse> {
    const { data } = await apiClient.patch<ApiResponse<SupplierResponse>>(
      `/suppliers/${id}/status`,
      null,
      { params: { status } },
    );
    return data.data;
  },

  async archive(id: string): Promise<void> {
    await apiClient.delete<ApiResponse<null>>(`/suppliers/${id}`);
  },

  async summary(): Promise<SupplierSummary> {
    const { data } = await apiClient.get<ApiResponse<SupplierSummary>>("/suppliers/summary");
    return data.data;
  },

  /** Live GST structure/checksum validation + state auto-fill, called as the user types a GSTIN. */
  async gstLookup(gstin: string): Promise<GstLookupResponse> {
    const { data } = await apiClient.get<ApiResponse<GstLookupResponse>>("/suppliers/gst-lookup", {
      params: { gstin },
    });
    return data.data;
  },

  /** Called on blur (name/GST/phone) before save to surface a "possible duplicate" warning. */
  async checkDuplicates(payload: {
    companyName: string;
    gstNumber?: string | null;
    contactPhone?: string | null;
    excludeId?: string | null;
  }): Promise<SupplierDuplicateMatch[]> {
    const { data } = await apiClient.post<ApiResponse<SupplierDuplicateMatch[]>>(
      "/suppliers/check-duplicate",
      payload,
    );
    return data.data;
  },
};
