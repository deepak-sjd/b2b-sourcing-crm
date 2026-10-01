import type { Role } from "./user";

// Mirrors com.doctutorials.crm.auth.dto.LoginRequest
export interface LoginRequest {
  email: string;
  password: string;
}

// Mirrors com.doctutorials.crm.auth.dto.AuthResponse
export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  userId: string;
  fullName: string;
  email: string;
  role: Role;
}

export interface AuthenticatedUser {
  id: string;
  fullName: string;
  email: string;
  role: Role;
}
