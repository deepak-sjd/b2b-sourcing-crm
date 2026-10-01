import { apiClient } from "./client";
import type { ApiResponse, PageResponse } from "@/types/api";
import type {
  ContactListFilters,
  ContactRequest,
  ContactResponse,
  ContactStatsResponse,
  DuplicateCheckResponse,
} from "@/types/contact";

export const contactsApi = {
  async list(filters: ContactListFilters): Promise<PageResponse<ContactResponse>> {
    const { data } = await apiClient.get<ApiResponse<PageResponse<ContactResponse>>>("/contacts", {
      params: {
        search: filters.search || undefined,
        engagement: filters.engagement,
        ownerId: filters.ownerId,
        page: filters.page ?? 0,
        size: filters.size ?? 20,
      },
    });
    return data.data;
  },

  async stats(): Promise<ContactStatsResponse> {
    const { data } = await apiClient.get<ApiResponse<ContactStatsResponse>>("/contacts/stats");
    return data.data;
  },

  async getById(id: string): Promise<ContactResponse> {
    const { data } = await apiClient.get<ApiResponse<ContactResponse>>(`/contacts/${id}`);
    return data.data;
  },

  async create(payload: ContactRequest): Promise<ContactResponse> {
    const { data } = await apiClient.post<ApiResponse<ContactResponse>>("/contacts", payload);
    return data.data;
  },

  async update(id: string, payload: ContactRequest): Promise<ContactResponse> {
    const { data } = await apiClient.put<ApiResponse<ContactResponse>>(`/contacts/${id}`, payload);
    return data.data;
  },

  async assign(id: string, ownerId: string): Promise<ContactResponse> {
    const { data } = await apiClient.patch<ApiResponse<ContactResponse>>(
      `/contacts/${id}/assign`,
      null,
      { params: { ownerId } },
    );
    return data.data;
  },

  /** Fires on every call/WhatsApp/email quick-action click, not just the explicit
   * "Log activity" button — this is what keeps "Last activity" and the AI
   * engagement score current without any manual bookkeeping. */
  async recordTouchpoint(id: string): Promise<ContactResponse> {
    const { data } = await apiClient.post<ApiResponse<ContactResponse>>(`/contacts/${id}/touchpoint`);
    return data.data;
  },

  async archive(id: string): Promise<void> {
    await apiClient.delete<ApiResponse<null>>(`/contacts/${id}`);
  },

  /** Live duplicate-detection while filling out the create form. */
  async checkDuplicate(params: { phone?: string; email?: string; excludeId?: string }): Promise<DuplicateCheckResponse> {
    const { data } = await apiClient.get<ApiResponse<DuplicateCheckResponse>>("/contacts/check-duplicate", {
      params: {
        phone: params.phone || undefined,
        email: params.email || undefined,
        excludeId: params.excludeId,
      },
    });
    return data.data;
  },
};
