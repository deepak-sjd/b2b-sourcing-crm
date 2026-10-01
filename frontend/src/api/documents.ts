import { apiClient } from "./client";
import type { ApiResponse, PageResponse } from "@/types/api";
import type { CrmDocument, DocumentType } from "@/types/document";

export const documentsApi = {
  async upload(leadId: string, file: File, type?: DocumentType): Promise<CrmDocument> {
    const form = new FormData();
    form.append("leadId", leadId);
    form.append("file", file);
    if (type) form.append("type", type);

    const { data } = await apiClient.post<ApiResponse<CrmDocument>>("/documents", form, {
      headers: { "Content-Type": "multipart/form-data" },
    });
    return data.data;
  },

  async byLead(leadId: string, page = 0, size = 20): Promise<PageResponse<CrmDocument>> {
    const { data } = await apiClient.get<ApiResponse<PageResponse<CrmDocument>>>(
      `/documents/by-lead/${leadId}`,
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
