import { apiClient } from './client';
import { AuditLogPage } from '../types/audit';

export const auditApi = {
  searchLogs: async (params?: {
    entityType?: string;
    entityId?: string;
    actorId?: string;
    action?: string;
    fromDate?: string;
    toDate?: string;
    page?: number;
    size?: number;
  }): Promise<AuditLogPage> => {
    const res = await apiClient.get<AuditLogPage>('/audit/logs', { params });
    return res.data;
  },
};
