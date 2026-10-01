import { apiClient } from "./client";
import type { ApiResponse, PageResponse } from "@/types/api";
import type { AddNoteRequest, Note } from "@/types/note";

export const notesApi = {
  async add(payload: AddNoteRequest): Promise<Note> {
    const { data } = await apiClient.post<ApiResponse<Note>>("/notes", payload);
    return data.data;
  },

  async byLead(leadId: string, page = 0, size = 20): Promise<PageResponse<Note>> {
    const { data } = await apiClient.get<ApiResponse<PageResponse<Note>>>(
      `/notes/by-lead/${leadId}`,
      { params: { page, size } },
    );
    return data.data;
  },
};
