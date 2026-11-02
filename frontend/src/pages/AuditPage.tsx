import React, { useState, useEffect } from 'react';
import { auditApi } from '../api/auditApi';
import { AuditLogEntry } from '../types/audit';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '../components/ui/card';
import { Input } from '../components/ui/input';
import { Button } from '../components/ui/button';
import { Badge } from '../components/ui/badge';
import { Drawer } from '../components/ui/drawer';
import { useI18n } from '../context/I18nContext';
import { Eye } from 'lucide-react';

export const AuditPage: React.FC = () => {
  const { t, formatDate } = useI18n();
  const [logs, setLogs] = useState<AuditLogEntry[]>([]);
  const [entityType, setEntityType] = useState('');
  const [action, setAction] = useState('');
  const [loading, setLoading] = useState(false);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [selectedLog, setSelectedLog] = useState<AuditLogEntry | null>(null);

  const loadLogs = async () => {
    setLoading(true);
    try {
      const data = await auditApi.searchLogs({
        entityType: entityType || undefined,
        action: action || undefined,
        page,
        size: 15,
      });
      setLogs(data.items);
      setTotalPages(data.totalPages);
    } catch (err) {
      console.error('Failed to load audit logs', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadLogs();
  }, [entityType, action, page]);

  const getActionBadgeVariant = (act: string) => {
    switch (act) {
      case 'CREATE':
        return 'success';
      case 'UPDATE':
        return 'warning';
      case 'DELETE':
        return 'destructive';
      case 'LOGIN':
        return 'default';
      default:
        return 'secondary';
    }
  };

  const formatJson = (raw?: string) => {
    if (!raw) return t('audit.noChanges');
    try {
      const parsed = JSON.parse(raw);
      return JSON.stringify(parsed, null, 2);
    } catch {
      return raw;
    }
  };

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-2xl font-bold tracking-tight text-slate-900 dark:text-white">
          {t('audit.title')}
        </h2>
        <p className="text-sm text-slate-500 dark:text-slate-400">
          {t('audit.description')}
        </p>
      </div>

      <Card>
        <CardHeader className="pb-3">
          <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
            <div>
              <CardTitle>{t('audit.title')}</CardTitle>
              <CardDescription>{t('common.actions')}: {logs.length}</CardDescription>
            </div>
            <div className="flex items-center gap-2">
              <div className="relative">
                <Input
                  placeholder={t('audit.filterByEntity')}
                  className="w-56 h-9"
                  value={entityType}
                  onChange={(e) => setEntityType(e.target.value)}
                />
              </div>
              <select
                className="h-9 rounded-md border border-input bg-background px-3 py-1 text-sm shadow-sm focus:outline-none focus:ring-1 focus:ring-ring"
                value={action}
                onChange={(e) => setAction(e.target.value)}
              >
                <option value="">{t('audit.allActions')}</option>
                <option value="CREATE">CREATE</option>
                <option value="UPDATE">UPDATE</option>
                <option value="DELETE">DELETE</option>
                <option value="LOGIN">LOGIN</option>
              </select>
            </div>
          </div>
        </CardHeader>
        <CardContent>
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="bg-slate-50 dark:bg-slate-900/50 text-slate-500 uppercase text-[11px] font-semibold border-y">
                <tr>
                  <th className="py-3 px-4">{t('audit.timestamp')}</th>
                  <th className="py-3 px-4">{t('audit.actor')}</th>
                  <th className="py-3 px-4">{t('audit.action')}</th>
                  <th className="py-3 px-4">{t('audit.entityType')}</th>
                  <th className="py-3 px-4">{t('audit.details')}</th>
                  <th className="py-3 px-4 text-right">{t('audit.stateBefore')} / {t('audit.stateAfter')}</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 dark:divide-slate-800">
                {logs.map((log) => (
                  <tr key={log.id} className="hover:bg-slate-50/50 dark:hover:bg-slate-900/50">
                    <td className="py-3 px-4 text-xs font-mono text-slate-500 whitespace-nowrap">
                      {formatDate(log.createdAt)}
                    </td>
                    <td className="py-3 px-4 font-medium text-slate-900 dark:text-white">
                      @{log.actorUsername}
                    </td>
                    <td className="py-3 px-4">
                      <Badge variant={getActionBadgeVariant(log.action)} className="text-[10px]">
                        {log.action}
                      </Badge>
                    </td>
                    <td className="py-3 px-4">
                      <span className="font-semibold text-slate-800 dark:text-slate-200">
                        {log.entityType}
                      </span>
                      <span className="text-xs text-slate-400 ml-1">#{log.entityId.slice(0, 8)}</span>
                    </td>
                    <td className="py-3 px-4 text-slate-600 dark:text-slate-400 max-w-xs truncate">
                      {log.details || '-'}
                    </td>
                    <td className="py-3 px-4 text-right">
                      {(log.stateBefore || log.stateAfter) && (
                        <Button
                          variant="outline"
                          size="sm"
                          className="gap-1 text-xs"
                          onClick={() => setSelectedLog(log)}
                        >
                          <Eye className="h-3.5 w-3.5" />
                          {t('audit.viewDiff')}
                        </Button>
                      )}
                    </td>
                  </tr>
                ))}
                {logs.length === 0 && !loading && (
                  <tr>
                    <td colSpan={6} className="py-8 text-center text-slate-500">
                      {t('common.noData')}
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>

          {/* Pagination */}
          {totalPages > 1 && (
            <div className="flex items-center justify-between pt-4 border-t mt-4">
              <Button
                variant="outline"
                size="sm"
                disabled={page === 0}
                onClick={() => setPage((p) => Math.max(0, p - 1))}
              >
                Previous
              </Button>
              <span className="text-xs text-slate-500">
                Page {page + 1} of {totalPages}
              </span>
              <Button
                variant="outline"
                size="sm"
                disabled={page >= totalPages - 1}
                onClick={() => setPage((p) => p + 1)}
              >
                Next
              </Button>
            </div>
          )}
        </CardContent>
      </Card>

      {/* State Diff Drawer */}
      <Drawer
        isOpen={!!selectedLog}
        onClose={() => setSelectedLog(null)}
        title={`${t('audit.inspectChanges')}: ${selectedLog?.action} (${selectedLog?.entityType})`}
        description={`Event ID: ${selectedLog?.id}`}
      >
        {selectedLog && (
          <div className="space-y-6">
            <div className="grid grid-cols-2 gap-3 p-3 bg-slate-50 dark:bg-slate-900 rounded-lg text-xs">
              <div>
                <span className="text-slate-400">{t('audit.actor')}:</span>
                <p className="font-semibold text-slate-900 dark:text-white">
                  @{selectedLog.actorUsername}
                </p>
              </div>
              <div>
                <span className="text-slate-400">{t('audit.timestamp')}:</span>
                <p className="font-semibold text-slate-900 dark:text-white">
                  {formatDate(selectedLog.createdAt)}
                </p>
              </div>
              <div>
                <span className="text-slate-400">{t('audit.entityType')}:</span>
                <p className="font-semibold text-slate-900 dark:text-white">
                  {selectedLog.entityType} ({selectedLog.entityId})
                </p>
              </div>
              <div>
                <span className="text-slate-400">{t('audit.details')}:</span>
                <p className="font-semibold text-slate-900 dark:text-white">
                  {selectedLog.details || '-'}
                </p>
              </div>
            </div>

            <div className="space-y-4">
              {selectedLog.stateBefore && (
                <div>
                  <h4 className="text-xs font-bold uppercase tracking-wider text-rose-500 mb-1">
                    {t('audit.stateBefore')}
                  </h4>
                  <pre className="p-3 rounded-lg bg-slate-900 text-rose-300 text-xs font-mono overflow-x-auto max-h-60 border border-slate-800">
                    {formatJson(selectedLog.stateBefore)}
                  </pre>
                </div>
              )}

              {selectedLog.stateAfter && (
                <div>
                  <h4 className="text-xs font-bold uppercase tracking-wider text-emerald-500 mb-1">
                    {t('audit.stateAfter')}
                  </h4>
                  <pre className="p-3 rounded-lg bg-slate-900 text-emerald-300 text-xs font-mono overflow-x-auto max-h-60 border border-slate-800">
                    {formatJson(selectedLog.stateAfter)}
                  </pre>
                </div>
              )}
            </div>
          </div>
        )}
      </Drawer>
    </div>
  );
};
