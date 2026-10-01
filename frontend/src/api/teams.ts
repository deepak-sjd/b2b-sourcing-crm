import { apiClient } from "./client";
import type { ApiResponse } from "@/types/api";
import type { CreateTeamRequest, Team } from "@/types/team";

export const teamsApi = {
  async create(payload: CreateTeamRequest): Promise<Team> {
    const { data } = await apiClient.post<ApiResponse<Team>>("/teams", payload);
    return data.data;
  },

  async getById(id: string): Promise<Team> {
    const { data } = await apiClient.get<ApiResponse<Team>>(`/teams/${id}`);
    return data.data;
  },

  async listAll(): Promise<Team[]> {
    const { data } = await apiClient.get<ApiResponse<Team[]>>("/teams");
    return data.data;
  },
};
