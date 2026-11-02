import React, { useState, useEffect } from 'react';
import { X, Clock, AlertTriangle, CheckCircle2, MinusCircle, RefreshCw } from 'lucide-react';
import { RuleExecutionTrace } from '../../types/rules';
import { rulesApi } from '../../api/rulesApi';
import { useI18n } from '../../context/I18nContext';

interface RuleTracesModalProps {
  isOpen: boolean;
  onClose: () => void;
  ruleId?: string;
}

export const RuleTracesModal: React.FC<RuleTracesModalProps> = ({
  isOpen,
  onClose,
  ruleId,
}) => {
  const { t, formatDate } = useI18n();
  const [traces, setTraces] = useState<RuleExecutionTrace[]>([]);
  const [loading, setLoading] = useState(false);

  const fetchTraces = async () => {
    setLoading(true);
    try {
      const data = await rulesApi.listTraces(ruleId, 0, 50);
      setTraces(data.content || []);
    } catch (e) {
      console.error('Failed to load traces', e);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (isOpen) {
      fetchTraces();
    }
  }, [isOpen, ruleId]);

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/50 backdrop-blur-sm animate-fadeIn">
      <div className="w-full max-w-4xl bg-white dark:bg-slate-900 rounded-2xl shadow-2xl border border-slate-200 dark:border-slate-800 flex flex-col max-h-[85vh]">
        {/* Header */}
        <div className="p-6 border-b border-slate-200 dark:border-slate-800 flex items-center justify-between">
          <div className="flex items-center space-x-3">
            <div>
              <h2 className="text-lg font-bold text-slate-900 dark:text-white">
                {t('rules', 'tracesTitle')}
              </h2>
              <p className="text-xs text-slate-500 mt-0.5">
                {t('rules', 'tracesDescription')}
              </p>
            </div>
            <button
              onClick={fetchTraces}
              disabled={loading}
              className="p-1.5 rounded-lg text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors"
            >
              <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin text-blue-600' : ''}`} />
            </button>
          </div>
          <button
            onClick={onClose}
            className="p-2 rounded-lg text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Content Table */}
        <div className="p-6 overflow-y-auto flex-1">
          {traces.length === 0 ? (
            <div className="text-center py-12 text-slate-400 text-sm">
              {loading ? t('common', 'loading') : t('rules', 'noTraces')}
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs">
                <thead>
                  <tr className="border-b border-slate-200 dark:border-slate-800 text-slate-400 uppercase tracking-wider font-semibold">
                    <th className="pb-3 pl-2">Time</th>
                    <th className="pb-3">Event / Trigger</th>
                    <th className="pb-3">Entity</th>
                    <th className="pb-3">Latency</th>
                    <th className="pb-3">Result</th>
                    <th className="pb-3">Details</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100 dark:divide-slate-800/60 font-mono">
                  {traces.map((trace) => (
                    <tr key={trace.id} className="hover:bg-slate-50/50 dark:hover:bg-slate-800/30">
                      <td className="py-3 pl-2 text-slate-500">
                        {formatDate(trace.createdAt, {
                          hour: '2-digit',
                          minute: '2-digit',
                          second: '2-digit',
                        })}
                      </td>
                      <td className="py-3 font-semibold text-slate-800 dark:text-slate-200">
                        {trace.triggerEvent}
                      </td>
                      <td className="py-3 text-slate-500">
                        {trace.entityType}:{trace.entityId}
                      </td>
                      <td className="py-3">
                        <span className="inline-flex items-center space-x-1 text-slate-600 dark:text-slate-400">
                          <Clock className="w-3 h-3 text-slate-400" />
                          <span>{trace.durationMs}ms</span>
                        </span>
                      </td>
                      <td className="py-3">
                        {trace.result === 'MATCHED' && (
                          <span className="inline-flex items-center space-x-1 px-2 py-0.5 rounded-full text-xs font-semibold bg-emerald-50 text-emerald-700 dark:bg-emerald-950/40 dark:text-emerald-400 border border-emerald-200 dark:border-emerald-800">
                            <CheckCircle2 className="w-3 h-3" />
                            <span>MATCHED</span>
                          </span>
                        )}
                        {trace.result === 'SKIPPED' && (
                          <span className="inline-flex items-center space-x-1 px-2 py-0.5 rounded-full text-xs font-semibold bg-slate-100 text-slate-600 dark:bg-slate-800 dark:text-slate-400 border border-slate-200 dark:border-slate-700">
                            <MinusCircle className="w-3 h-3" />
                            <span>SKIPPED</span>
                          </span>
                        )}
                        {trace.result === 'ERROR' && (
                          <span className="inline-flex items-center space-x-1 px-2 py-0.5 rounded-full text-xs font-semibold bg-red-50 text-red-700 dark:bg-red-950/40 dark:text-red-400 border border-red-200 dark:border-red-800">
                            <AlertTriangle className="w-3 h-3" />
                            <span>ERROR</span>
                          </span>
                        )}
                      </td>
                      <td className="py-3 text-slate-500 max-w-xs truncate">
                        {trace.errorDetail || '—'}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
