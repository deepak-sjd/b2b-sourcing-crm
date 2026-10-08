import { apiClient } from "./client";
import type { ApiResponse, PageResponse } from "@/types/api";
import type {
  SupplierEnquiryReplyRequest,
  SupplierEnquiryReplyResponse,
  SupplierEnquiryRequest,
  SupplierEnquiryResponse,
} from "@/types/supplierEnquiry";

// React Query keys for this module all start with ["supplier-enquiries", ...]
// so one invalidation (queryKey: ["supplier-enquiries"]) refreshes every view:
// the lead tab, the requirement sourcing dialog, and supplier history alike.
export const supplierEnquiriesApi = {
  async create(payload: SupplierEnquiryRequest): Promise<SupplierEnquiryResponse> {
    const { data } = await apiClient.post<ApiResponse<SupplierEnquiryResponse>>("/supplier-enquiries", payload);
    return data.data;
  },

  async getById(id: string): Promise<SupplierEnquiryResponse> {
    const { data } = await apiClient.get<ApiResponse<SupplierEnquiryResponse>>(`/supplier-enquiries/${id}`);
    return data.data;
  },

  async byRequirement(requirementId: string): Promise<SupplierEnquiryResponse[]> {
    const { data } = await apiClient.get<ApiResponse<SupplierEnquiryResponse[]>>(
      `/supplier-enquiries/by-requirement/${requirementId}`,
    );
    return data.data;
  },

  async byLead(leadId: string): Promise<PageResponse<SupplierEnquiryResponse>> {
    const { data } = await apiClient.get<ApiResponse<PageResponse<SupplierEnquiryResponse>>>(
      `/supplier-enquiries/by-lead/${leadId}`,
    );
    return data.data;
  },

  async bySupplier(supplierId: string, page = 0, size = 20): Promise<PageResponse<SupplierEnquiryReplyResponse>> {
    const { data } = await apiClient.get<ApiResponse<PageResponse<SupplierEnquiryReplyResponse>>>(
      `/supplier-enquiries/by-supplier/${supplierId}`,
      { params: { page, size } },
    );
    return data.data;
  },

  async recordReply(
    enquiryId: string,
    replyId: string,
    payload: SupplierEnquiryReplyRequest,
  ): Promise<SupplierEnquiryResponse> {
    const { data } = await apiClient.patch<ApiResponse<SupplierEnquiryResponse>>(
      `/supplier-enquiries/${enquiryId}/replies/${replyId}`,
      payload,
    );
    return data.data;
  },

  async choose(enquiryId: string, replyId: string): Promise<SupplierEnquiryResponse> {
    const { data } = await apiClient.post<ApiResponse<SupplierEnquiryResponse>>(
      `/supplier-enquiries/${enquiryId}/replies/${replyId}/choose`,
    );
    return data.data;
  },

  async unchoose(enquiryId: string, replyId: string): Promise<SupplierEnquiryResponse> {
    const { data } = await apiClient.delete<ApiResponse<SupplierEnquiryResponse>>(
      `/supplier-enquiries/${enquiryId}/replies/${replyId}/choose`,
    );
    return data.data;
  },

  async resend(enquiryId: string, replyId: string): Promise<SupplierEnquiryReplyResponse> {
    const { data } = await apiClient.post<ApiResponse<SupplierEnquiryReplyResponse>>(
      `/supplier-enquiries/${enquiryId}/replies/${replyId}/resend`,
    );
    return data.data;
  },
};
