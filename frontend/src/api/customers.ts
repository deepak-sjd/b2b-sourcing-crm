import { apiClient } from "./client";
import type { ApiResponse, PageResponse } from "@/types/api";
import type {
  AccountManagerSuggestion,
  ApprovalStatus,
  CustomerListFilters,
  CustomerRequest,
  CustomerResponse,
  DuplicateMatch,
  GstInfo,
} from "@/types/customer";

export const customersApi = {
  async list(filters: CustomerListFilters): Promise<PageResponse<CustomerResponse>> {
    const { data } = await apiClient.get<ApiResponse<PageResponse<CustomerResponse>>>("/customers", {
      params: {
        approvalStatus: filters.approvalStatus,
        accountManagerId: filters.accountManagerId,
        page: filters.page ?? 0,
        size: filters.size ?? 20,
      },
    });
    return data.data;
  },

  async getById(id: string): Promise<CustomerResponse> {
    const { data } = await apiClient.get<ApiResponse<CustomerResponse>>(`/customers/${id}`);
    return data.data;
  },

  async create(payload: CustomerRequest): Promise<CustomerResponse> {
    const { data } = await apiClient.post<ApiResponse<CustomerResponse>>("/customers", payload);
    return data.data;
  },

  async update(id: string, payload: CustomerRequest): Promise<CustomerResponse> {
    const { data } = await apiClient.put<ApiResponse<CustomerResponse>>(`/customers/${id}`, payload);
    return data.data;
  },

  async changeApprovalStatus(id: string, status: ApprovalStatus): Promise<CustomerResponse> {
    const { data } = await apiClient.patch<ApiResponse<CustomerResponse>>(
      `/customers/${id}/approval-status`,
      null,
      { params: { status } },
    );
    return data.data;
  },

  async assignAccountManager(id: string, accountManagerId: string): Promise<CustomerResponse> {
    const { data } = await apiClient.patch<ApiResponse<CustomerResponse>>(
      `/customers/${id}/account-manager`,
      null,
      { params: { accountManagerId } },
    );
    return data.data;
  },

  async archive(id: string): Promise<void> {
    await apiClient.delete<ApiResponse<null>>(`/customers/${id}`);
  },

  // --- AI automation ---

  /** Powers the duplicate-warning banner in the create/edit form. */
  async checkDuplicate(companyName: string, gstNo?: string, excludingId?: string): Promise<DuplicateMatch[]> {
    const { data } = await apiClient.get<ApiResponse<DuplicateMatch[]>>("/customers/duplicate-check", {
      params: { companyName, gstNo: gstNo || undefined, excludingId },
    });
    return data.data;
  },

  /** Powers the "detected state: ..." hint under the GST No. field. */
  async gstLookup(gstNo: string): Promise<GstInfo> {
    const { data } = await apiClient.get<ApiResponse<GstInfo>>("/customers/gst-lookup", { params: { gstNo } });
    return data.data;
  },

  /** Powers the "Suggest: <name>" chip next to the Account Manager field. */
  async suggestAccountManager(): Promise<AccountManagerSuggestion | null> {
    const { data } = await apiClient.get<ApiResponse<AccountManagerSuggestion | null>>(
      "/customers/suggest-account-manager",
    );
    return data.data;
  },
};
