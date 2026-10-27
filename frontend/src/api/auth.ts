import { apiClient } from "./client";
import type { ApiResponse } from "@/types/api";
import type { AuthResponse, LoginRequest } from "@/types/auth";

// Mirrors com.doctutorials.crm.auth.dto.SignupRequest
export interface SignupRequest {
  companyName: string;
  fullName: string;
  email: string;
  password: string;
}

export const authApi = {
  async login(payload: LoginRequest): Promise<AuthResponse> {
    const { data } = await apiClient.post<ApiResponse<AuthResponse>>("/auth/login", payload);
    return data.data;
  },

  async signup(payload: SignupRequest): Promise<AuthResponse> {
    const { data } = await apiClient.post<ApiResponse<AuthResponse>>("/auth/signup", payload);
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