import type { Priority } from "./lead";

// Mirrors com.doctutorials.crm.requirement.RequirementStatus
export type RequirementStatus = "OPEN" | "SOURCING" | "QUOTED" | "CLOSED" | "CANCELLED";

export const REQUIREMENT_STATUSES: RequirementStatus[] = ["OPEN", "SOURCING", "QUOTED", "CLOSED", "CANCELLED"];

export const REQUIREMENT_STATUS_LABELS: Record<RequirementStatus, string> = {
  OPEN: "Open",
  SOURCING: "Sourcing",
  QUOTED: "Quoted",
  CLOSED: "Closed",
  CANCELLED: "Cancelled",
};

/** Statuses a requirement can no longer be edited in (backend-enforced). */
export const LOCKED_REQUIREMENT_STATUSES: RequirementStatus[] = ["CLOSED", "CANCELLED"];

// Mirrors com.doctutorials.crm.requirement.dto.RequirementResponse
export interface Requirement {
  id: string;
  leadId: string;
  leadName: string;
  productName: string;
  description: string | null;
  quantity: number | null;
  unit: string | null;
  expectedTimeline: string | null;
  budget: number | null;
  priority: Priority;
  requiredDeliveryDate: string | null; // yyyy-MM-dd
  additionalSpecifications: string | null;
  remarks: string | null;
  needsSupplier: boolean;
  status: RequirementStatus;
  assignedToId: string | null;
  assignedToName: string | null;
  createdAt: string;
  updatedAt: string;
}

// Mirrors com.doctutorials.crm.requirement.dto.RequirementRequest
export interface RequirementRequest {
  leadId?: string;
  productName: string;
  description?: string | null;
  quantity?: number | null;
  unit?: string | null;
  expectedTimeline?: string | null;
  budget?: number | null;
  priority?: Priority;
  requiredDeliveryDate?: string | null;
  additionalSpecifications?: string | null;
  remarks?: string | null;
  needsSupplier?: boolean;
  assignedToId?: string | null;
}

// Keys match RequirementService.summary()
export interface RequirementSummary {
  total: number;
  open: number;
  sourcing: number;
  quoted: number;
  closed: number;
  cancelled: number;
  awaitingSupplier: number;
}

export interface RequirementListFilters {
  q?: string;
  status?: RequirementStatus;
  priority?: Priority;
  needsSupplier?: boolean;
  page?: number;
  size?: number;
}
