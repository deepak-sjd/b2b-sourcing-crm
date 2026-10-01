// Mirrors com.doctutorials.crm.audit.AuditAction
export type AuditAction = "CREATE" | "UPDATE" | "DELETE" | "STAGE_CHANGE" | "LOGIN" | "EXPORT" | "STATUS_CHANGE";

// Mirrors com.doctutorials.crm.audit.AuditLog entity as serialized by Jackson
export interface AuditLogEntry {
  id: string;
  actorUserId: string | null;
  actorName: string | null;
  entityType: string;
  entityId: string;
  action: AuditAction;
  description: string;
  createdAt: string;
}

export interface AuditLogFilters {
  entityType?: string;
  page?: number;
  size?: number;
}
