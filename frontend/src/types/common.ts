import type { Role } from "./user";

/** Task/Call/Note/Document endpoints return the raw JPA entity (not a slim
 * DTO like Lead does), so nested `lead` / `assignedTo` / `author` fields
 * come back as (partial) entity objects. Model only what we render. */
export interface EntityUserRef {
  id: string;
  fullName: string;
  email: string;
  role?: Role;
}

export interface EntityLeadRef {
  id: string;
  fullName: string;
  phone?: string | null;
  email?: string | null;
  stage?: string;
}
