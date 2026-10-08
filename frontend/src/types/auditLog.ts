// Mirrors com.doctutorials.crm.audit.AuditAction
export type AuditAction = "CREATE" | "UPDATE" | "DELETE" | "STAGE_CHANGE" | "LOGIN" | "EXPORT" | "STATUS_CHANGE";
export const AUDIT_ACTIONS: AuditAction[] = ["CREATE", "UPDATE", "DELETE", "STAGE_CHANGE", "LOGIN", "EXPORT", "STATUS_CHANGE"];

// Known entityType strings used across every auditService.log(...) call site
// in the backend (Lead, Customer, Contact, Supplier, Requirement,
// SupplierEnquiry, Quotation, Activity, Task, Note, Document) - kept as a
// plain list rather than importing from each module, since this is just UI
// filter options, not a type contract.
export const AUDIT_ENTITY_TYPES = [
  "Lead", "Customer", "Contact", "Supplier", "Requirement",
  "SupplierEnquiry", "Quotation", "Activity", "Task", "Note", "Document",
] as const;

// Mirrors com.doctutorials.crm.audit.AuditLog entity as serialized by Jackson
export interface AuditLogEntry {
  id: string;
  actorUserId: string | null;
  actorName: string | null;
  entityType: string;
  entityId: string;
  action: AuditAction;
  description: string;
  oldValues: string | null; // JSON string, e.g. {"stage":"NEW"}
  newValues: string | null;
  changedFields: string | null; // comma-joined field names
  createdAt: string;
}

// Mirrors com.doctutorials.crm.audit.dto.AuditTimelineEntry - `at` not `createdAt`
export interface AuditTimelineEntry {
  id: string;
  at: string;
  actorName: string | null;
  entityType: string;
  entityId: string;
  action: AuditAction;
  description: string;
  changedFields: string | null;
  oldValues: string | null;
  newValues: string | null;
}

export interface AuditLogFilters {
  entityType?: string;
  action?: AuditAction;
  actorUserId?: string;
  from?: string; // ISO date-time
  to?: string;
  page?: number;
  size?: number;
}

export interface LeadTimelineFilters {
  entityType?: string;
  from?: string;
  to?: string;
  page?: number;
  size?: number;
}

/** Parses the JSON-string old/new value maps into a plain object for rendering. */
export function parseValueMap(json: string | null): Record<string, string> {
  if (!json) return {};
  try {
    return JSON.parse(json);
  } catch {
    return {};
  }
}