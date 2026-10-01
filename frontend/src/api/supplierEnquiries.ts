import { apiClient } from "./client";
import type { ApiResponse, PageResponse } from "@/types/api";
import type {
  SupplierEnquiryReplyRequest,
  SupplierEnquiryReplyResponse,
  SupplierEnquiryRequest,
  SupplierEnquiryResponse,
} from "@/types/supplierEnquiry";

export const supplierEnquiriesApi = {
  async create(payload: SupplierEnquiryRequest): Promise<SupplierEnquiryResponse> {
    const { data } = await apiClient.post<ApiResponse<SupplierEnquiryResponse>>("/supplier-enquiries", payload);
    return data.data;
  },

  async getById(id: string): Promise<SupplierEnquiryResponse> {
    const { data } = await apiClient.get<ApiResponse<SupplierEnquiryResponse>>(`/supplier-enquiries/${id}`);
    return data.data;
  },

  async byLead(leadId: string): Promise<PageResponse<SupplierEnquiryResponse>> {
    const { data } = await apiClient.get<ApiResponse<PageResponse<SupplierEnquiryResponse>>>(
      `/supplier-enquiries/by-lead/${leadId}`,
    );
    return data.data;
  },

  async bySupplier(supplierId: string): Promise<PageResponse<SupplierEnquiryReplyResponse>> {
    const { data } = await apiClient.get<ApiResponse<PageResponse<SupplierEnquiryReplyResponse>>>(
      `/supplier-enquiries/by-supplier/${supplierId}`,
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

  async chooseReply(enquiryId: string, replyId: string): Promise<SupplierEnquiryResponse> {
    const { data } = await apiClient.post<ApiResponse<SupplierEnquiryResponse>>(
      `/supplier-enquiries/${enquiryId}/replies/${replyId}/choose`,
    );
    return data.data;
  },
};
