import { apiClient } from "./client";
import type { ApiResponse, PageResponse } from "@/types/api";
import type { CrmDocument, DocumentParentType, DocumentType } from "@/types/document";

const PARENT_PARAM: Record<DocumentParentType, string> = {
  LEAD: "leadId",
  CUSTOMER: "customerId",
  SUPPLIER: "supplierId",
  REQUIREMENT: "requirementId",
  QUOTATION: "quotationId",
  ENQUIRY_REPLY: "enquiryReplyId",
};

export const documentsApi = {
  async upload(parentType: DocumentParentType, parentId: string, file: File, type?: DocumentType, description?: string): Promise<CrmDocument> {
    const form = new FormData();
    form.append(PARENT_PARAM[parentType], parentId);
    form.append("file", file);
    if (type) form.append("type", type);
    if (description) form.append("description", description);

    const { data } = await apiClient.post<ApiResponse<CrmDocument>>("/documents", form, {
      headers: { "Content-Type": "multipart/form-data" },
    });
    return data.data;
  },

  async byParent(parentType: DocumentParentType, parentId: string, page = 0, size = 20): Promise<PageResponse<CrmDocument>> {
    const { data } = await apiClient.get<ApiResponse<PageResponse<CrmDocument>>>(
      `/documents/by/${parentType}/${parentId}`,
      { params: { page, size } },
    );
    return data.data;
  },

  /** Triggers a browser download by fetching the blob and clicking a temp <a>,
   * since the endpoint requires the Authorization header (a plain <a href> can't
   * carry it). */
  async download(id: string, fileName: string): Promise<void> {
    const response = await apiClient.get(`/documents/${id}/download`, {
      responseType: "blob",
    });
    const url = window.URL.createObjectURL(new Blob([response.data]));
    const link = document.createElement("a");
    link.href = url;
    link.download = fileName;
    document.body.appendChild(link);
    link.click();
    link.remove();
    window.URL.revokeObjectURL(url);
  },

  async remove(id: string): Promise<void> {
    await apiClient.delete<ApiResponse<null>>(`/documents/${id}`);
  },
};