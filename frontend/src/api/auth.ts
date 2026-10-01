import { apiClient } from "./client";
import type { ApiResponse } from "@/types/api";
import type { AuthResponse, LoginRequest } from "@/types/auth";
import type { PublicRegisterRequest, UserResponse } from "@/types/user";

export const authApi = {
  async login(payload: LoginRequest): Promise<AuthResponse> {
    const { data } = await apiClient.post<ApiResponse<AuthResponse>>("/auth/login", payload);
    return data.data;
  },

  async register(payload: PublicRegisterRequest): Promise<UserResponse> {
    const { data } = await apiClient.post<ApiResponse<UserResponse>>("/auth/register", payload);
    return data.data;
  },

  async logout(refreshToken: string): Promise<void> {
    await apiClient.post<ApiResponse<null>>("/auth/logout", { refreshToken });
  },

  async forgotPassword(email: string): Promise<string> {
    const { data } = await apiClient.post<ApiResponse<null>>("/auth/forgot-password", { email });
    return data.message;
  },

  async resetPassword(token: string, newPassword: string): Promise<string> {
    const { data } = await apiClient.post<ApiResponse<null>>("/auth/reset-password", {
      token,
      newPassword,
    });
    return data.message;
  },
};
