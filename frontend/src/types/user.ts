// Mirrors com.doctutorials.crm.user.Role
export type Role = "SUPER_ADMIN" | "ADMIN" | "MANAGER" | "COUNSELOR" | "VIEWER";

export const ROLES: Role[] = ["SUPER_ADMIN", "ADMIN", "MANAGER", "COUNSELOR", "VIEWER"];

export const ROLE_LABELS: Record<Role, string> = {
  SUPER_ADMIN: "Super Admin",
  ADMIN: "Admin",
  MANAGER: "Manager",
  COUNSELOR: "Counselor",
  VIEWER: "Viewer",
};

// Mirrors com.doctutorials.crm.user.dto.UserResponse
export interface UserResponse {
  id: string;
  fullName: string;
  email: string;
  role: Role;
  teamName?: string | null;
  active: boolean;
}

// Mirrors com.doctutorials.crm.user.dto.PublicRegisterRequest
// (self-registration is always created as COUNSELOR, no team — enforced server-side)
export interface PublicRegisterRequest {
  fullName: string;
  email: string;
  password: string;
}

// Mirrors com.doctutorials.crm.user.dto.RegisterRequest (admin-only user creation)
export interface RegisterRequest {
  fullName: string;
  email: string;
  password: string;
  role: Role;
  teamId?: string | null;
}
