import type { Priority } from "./lead";
import type { EntityLeadRef, EntityUserRef } from "./common";

// Mirrors com.doctutorials.crm.task.TaskStatus
export type TaskStatus = "PENDING" | "IN_PROGRESS" | "COMPLETED" | "OVERDUE" | "CANCELLED";

export const TASK_STATUSES: TaskStatus[] = [
  "PENDING",
  "IN_PROGRESS",
  "COMPLETED",
  "OVERDUE",
  "CANCELLED",
];

export const TASK_STATUS_LABELS: Record<TaskStatus, string> = {
  PENDING: "Pending",
  IN_PROGRESS: "In progress",
  COMPLETED: "Completed",
  OVERDUE: "Overdue",
  CANCELLED: "Cancelled",
};

// Mirrors com.doctutorials.crm.task.Task entity as serialized by Jackson
export interface Task {
  id: string;
  title: string;
  description: string | null;
  lead: EntityLeadRef;
  assignedTo: EntityUserRef | null;
  priority: Priority;
  status: TaskStatus;
  dueDate: string | null;
  completedAt: string | null;
  overdue: boolean; // isOverdue() -> Jackson serializes boolean getter as "overdue"
  createdAt: string;
  updatedAt: string;
}

// Mirrors TaskController.CreateTaskRequest
export interface CreateTaskRequest {
  title: string;
  description?: string | null;
  leadId?: string | null;
  assignedToId?: string | null;
  dueDate?: string | null;
}

export interface TaskSummary {
  pending: number;
  completed: number;
  overdue: number;
}

export interface TaskListFilters {
  status?: TaskStatus;
  page?: number;
  size?: number;
}
