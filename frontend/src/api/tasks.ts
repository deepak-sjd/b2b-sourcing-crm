import { apiClient } from "./client";
import type { ApiResponse, PageResponse } from "@/types/api";
import type {
  CreateTaskRequest, Task, TaskBucket, TaskCounts, TaskListFilters, TaskSummary,
} from "@/types/task";

const browserTz = () => Intl.DateTimeFormat().resolvedOptions().timeZone || "UTC";

// Query keys for this module all start with ["tasks", ...] so one invalidation refreshes every view.
export const tasksApi = {
  async create(payload: CreateTaskRequest): Promise<Task> {
    const { data } = await apiClient.post<ApiResponse<Task>>("/tasks", payload);
    return data.data;
  },

  async update(id: string, payload: CreateTaskRequest): Promise<Task> {
    const { data } = await apiClient.put<ApiResponse<Task>>(`/tasks/${id}`, payload);
    return data.data;
  },

  async complete(id: string): Promise<Task> {
    const { data } = await apiClient.patch<ApiResponse<Task>>(`/tasks/${id}/complete`);
    return data.data;
  },

  async bulkComplete(ids: string[]): Promise<Task[]> {
    const { data } = await apiClient.post<ApiResponse<Task[]>>("/tasks/bulk-complete", { ids });
    return data.data;
  },

  async reopen(id: string): Promise<Task> {
    const { data } = await apiClient.patch<ApiResponse<Task>>(`/tasks/${id}/reopen`);
    return data.data;
  },

  async snooze(id: string, until: string): Promise<Task> {
    const { data } = await apiClient.patch<ApiResponse<Task>>(`/tasks/${id}/snooze`, { until });
    return data.data;
  },

  async cancel(id: string, reason?: string): Promise<Task> {
    const { data } = await apiClient.patch<ApiResponse<Task>>(`/tasks/${id}/cancel`, { reason });
    return data.data;
  },

  async reassign(id: string, assigneeId: string): Promise<Task> {
    const { data } = await apiClient.patch<ApiResponse<Task>>(`/tasks/${id}/assign`, { assigneeId });
    return data.data;
  },

  async getById(id: string): Promise<Task> {
    const { data } = await apiClient.get<ApiResponse<Task>>(`/tasks/${id}`);
    return data.data;
  },

  /** Global, visibility-scoped task list — backs the standalone Tasks page. */
  async list(filters: TaskListFilters): Promise<PageResponse<Task>> {
    const { data } = await apiClient.get<ApiResponse<PageResponse<Task>>>("/tasks", {
      params: { ...filters, page: filters.page ?? 0, size: filters.size ?? 20 },
    });
    return data.data;
  },

  /** My Day sections. "Today" is the browser's today, not the server's. */
  async my(bucket: TaskBucket, page = 0, size = 50): Promise<PageResponse<Task>> {
    const { data } = await apiClient.get<ApiResponse<PageResponse<Task>>>("/tasks/my", {
      params: { bucket, tz: browserTz(), page, size },
    });
    return data.data;
  },

  async myCounts(): Promise<TaskCounts> {
    const { data } = await apiClient.get<ApiResponse<TaskCounts>>("/tasks/my/counts", { params: { tz: browserTz() } });
    return data.data;
  },

  async byLead(leadId: string, page = 0, size = 20): Promise<PageResponse<Task>> {
    const { data } = await apiClient.get<ApiResponse<PageResponse<Task>>>(`/tasks/by-lead/${leadId}`, {
      params: { page, size },
    });
    return data.data;
  },

  async summary(): Promise<TaskSummary> {
    const { data } = await apiClient.get<ApiResponse<TaskSummary>>("/tasks/summary");
    return data.data;
  },
};
