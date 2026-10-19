import type { Priority } from "./lead";
import type { EntityUserRef } from "./common";

// Mirrors com.doctutorials.crm.task.TaskStatus
export type TaskStatus = "PENDING" | "IN_PROGRESS" | "COMPLETED" | "OVERDUE" | "CANCELLED";
export const TASK_STATUSES: TaskStatus[] = ["PENDING", "IN_PROGRESS", "COMPLETED", "OVERDUE", "CANCELLED"];
export const TASK_STATUS_LABELS: Record<TaskStatus, string> = {
  PENDING: "Pending",
  IN_PROGRESS: "In progress",
  COMPLETED: "Completed",
  OVERDUE: "Overdue",
  CANCELLED: "Cancelled",
};

// Mirrors com.doctutorials.crm.task.TaskType (PRD §11)
export type TaskType =
  | "CALL" | "MEETING" | "QUOTATION" | "CUSTOMER_RESPONSE"
  | "SUPPLIER_RESPONSE" | "REQUIREMENT_CLARIFICATION" | "INTERNAL";
export const TASK_TYPES: TaskType[] = [
  "CALL", "MEETING", "QUOTATION", "CUSTOMER_RESPONSE", "SUPPLIER_RESPONSE", "REQUIREMENT_CLARIFICATION", "INTERNAL",
];
export const TASK_TYPE_LABELS: Record<TaskType, string> = {
  CALL: "Call",
  MEETING: "Meeting",
  QUOTATION: "Quotation",
  CUSTOMER_RESPONSE: "Customer response",
  SUPPLIER_RESPONSE: "Supplier response",
  REQUIREMENT_CLARIFICATION: "Requirement clarification",
  INTERNAL: "Internal",
};

export type TaskBucket = "OVERDUE" | "TODAY" | "UPCOMING";

// Mirrors com.doctutorials.crm.task.dto.TaskResponse
export interface Task {
  id: string;
  title: string;
  description: string | null;
  type: TaskType;
  lead: { id: string; fullName: string; phone?: string | null; email?: string | null; stage?: string } | null;
  customer: { id: string; companyName: string } | null;
  requirementId: string | null;
  quotationId: string | null;
  enquiryId: string | null;
  activityId: string | null;
  assignedTo: EntityUserRef | null;
  createdBy: EntityUserRef | null;
  completedBy: EntityUserRef | null;
  priority: Priority;
  status: TaskStatus;
  dueDate: string | null;
  reminderAt: string | null;
  completedAt: string | null;
  remarks: string | null;
  cancelReason: string | null;
  overdue: boolean;
  createdAt: string;
  updatedAt: string;
}

// Mirrors com.doctutorials.crm.task.dto.TaskRequest (links are read on create only; no actor fields)
export interface CreateTaskRequest {
  title: string;
  description?: string | null;
  type?: TaskType;
  leadId?: string | null;
  customerId?: string | null;
  requirementId?: string | null;
  quotationId?: string | null;
  enquiryId?: string | null;
  activityId?: string | null;
  assignedToId?: string | null;
  dueDate?: string | null;
  priority?: Priority;
  reminderAt?: string | null;
  remarks?: string | null;
}

export interface TaskSummary {
  pending: number;
  completed: number;
  overdue: number;
}

export interface TaskCounts {
  overdue: number;
  today: number;
  upcoming: number;
}

export interface TaskListFilters {
  status?: TaskStatus;
  type?: TaskType;
  priority?: Priority;
  assigneeId?: string;
  leadId?: string;
  dueFrom?: string;
  dueTo?: string;
  page?: number;
  size?: number;
}
