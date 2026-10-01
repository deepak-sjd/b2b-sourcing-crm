import { apiClient } from "./client";
import type { ApiResponse, PageResponse } from "@/types/api";
import type { CreateTaskRequest, Task, TaskListFilters, TaskSummary } from "@/types/task";

export const tasksApi = {
  async create(payload: CreateTaskRequest): Promise<Task> {
    const { data } = await apiClient.post<ApiResponse<Task>>("/tasks", payload);
    return data.data;
  },

  async complete(id: string): Promise<Task> {
    const { data } = await apiClient.patch<ApiResponse<Task>>(`/tasks/${id}/complete`);
    return data.data;
  },

  /** Global, visibility-scoped task list (GET /tasks) — backs the standalone Tasks page. */
  async list(filters: TaskListFilters): Promise<PageResponse<Task>> {
    const { data } = await apiClient.get<ApiResponse<PageResponse<Task>>>("/tasks", {
      params: { status: filters.status, page: filters.page ?? 0, size: filters.size ?? 20 },
    });
    return data.data;
  },

  async byLead(leadId: string, page = 0, size = 20): Promise<PageResponse<Task>> {
    const { data } = await apiClient.get<ApiResponse<PageResponse<Task>>>(
      `/tasks/by-lead/${leadId}`,
      { params: { page, size } },
    );
    return data.data;
  },

    async summary(): Promise<TaskSummary> {
    const { data } = await apiClient.get<ApiResponse<TaskSummary>>("/tasks/summary");
    return data.data;
  },
};
