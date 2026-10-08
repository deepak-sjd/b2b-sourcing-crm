import { apiClient } from "./client";
import type { ApiResponse, PageResponse } from "@/types/api";
import type {
  Activity, ActivityListFilters, ActivitySummary, LogActivityRequest, UpdateActivityRequest,
  MeetingStatusRequest, ActivityType,
} from "@/types/activity";

export const activitiesApi = {
  async log(payload: LogActivityRequest): Promise<Activity> {
    const { data } = await apiClient.post<ApiResponse<Activity>>("/activities", payload);
    return data.data;
  },

  async update(id: string, payload: UpdateActivityRequest): Promise<Activity> {
    const { data } = await apiClient.put<ApiResponse<Activity>>(`/activities/${id}`, payload);
    return data.data;
  },

  /** PRD §9 - complete/reschedule/cancel/no-show a meeting. */
  async changeMeetingStatus(id: string, payload: MeetingStatusRequest): Promise<Activity> {
    const { data } = await apiClient.patch<ApiResponse<Activity>>(`/activities/${id}/meeting-status`, payload);
    return data.data;
  },

  /** Global, visibility-scoped feed — backs the standalone Activities page. */
  async list(filters: ActivityListFilters): Promise<PageResponse<Activity>> {
    const { data } = await apiClient.get<ApiResponse<PageResponse<Activity>>>("/activities", {
      params: { type: filters.type, status: filters.status, page: filters.page ?? 0, size: filters.size ?? 20 },
    });
    return data.data;
  },

  async byLead(leadId: string, type?: ActivityType, page = 0, size = 20): Promise<PageResponse<Activity>> {
    const { data } = await apiClient.get<ApiResponse<PageResponse<Activity>>>(
      `/activities/by-lead/${leadId}`, { params: { type, page, size } },
    );
    return data.data;
  },

  /** Powers the "Due soon" panel that auto-loads on the Activities page. */
  async dueFollowUps(page = 0, size = 10): Promise<PageResponse<Activity>> {
    const { data } = await apiClient.get<ApiResponse<PageResponse<Activity>>>("/activities/due-follow-ups", {
      params: { page, size },
    });
    return data.data;
  },

  async summary(): Promise<ActivitySummary> {
    const { data } = await apiClient.get<ApiResponse<ActivitySummary>>("/activities/summary");
    return data.data;
  },
};