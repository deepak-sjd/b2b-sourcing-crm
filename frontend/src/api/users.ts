import { apiClient } from "./client";
import type { ApiResponse } from "@/types/api";
import type { RegisterRequest, UserResponse } from "@/types/user";

export const usersApi = {
  async create(payload: RegisterRequest): Promise<UserResponse> {
    const { data } = await apiClient.post<ApiResponse<UserResponse>>("/users", payload);
    return data.data;
  },

  async listAll(): Promise<UserResponse[]> {
    const { data } = await apiClient.get<ApiResponse<UserResponse[]>>("/users");
    return data.data;
  },

  async getById(id: string): Promise<UserResponse> {
    const { data } = await apiClient.get<ApiResponse<UserResponse>>(`/users/${id}`);
    return data.data;
  },

  async deactivate(id: string): Promise<void> {
    await apiClient.patch<ApiResponse<null>>(`/users/${id}/deactivate`);
  },
};
