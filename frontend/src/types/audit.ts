export interface AuditLogEntry {
  id: string;
  tenantId: string;
  actorId?: string;
  actorUsername: string;
  action: string;
  entityType: string;
  entityId: string;
  details?: string;
  stateBefore?: string;
  stateAfter?: string;
  clientIp?: string;
  userAgent?: string;
  createdAt: string;
}

export interface AuditLogPage {
  items: AuditLogEntry[];
  totalElements: number;
  totalPages: number;
  currentPage: number;
}
