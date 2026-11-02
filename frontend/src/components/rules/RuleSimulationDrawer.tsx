import React, { useState } from 'react';
import { X, Play, CheckCircle2, XCircle, AlertTriangle, Clock } from 'lucide-react';
import { RuleRequest, RuleSimulationResponse } from '../../types/rules';
import { rulesApi } from '../../api/rulesApi';
import { useI18n } from '../../context/I18nContext';

interface RuleSimulationDrawerProps {
  isOpen: boolean;
  onClose: () => void;
  rule: RuleRequest;
  ruleId?: string;
}

export const RuleSimulationDrawer: React.FC<RuleSimulationDrawerProps> = ({
  isOpen,
  onClose,
  rule,
  ruleId,
}) => {
  const { t } = useI18n();
  const [contextJson, setContextJson] = useState<string>(() => {
    return JSON.stringify(
      {
        sale: {
          totalAmount: 1500.0,
          discountPercentage: 20.0,
          customer: { tier: 'VIP' },
        },
        item: {
          price: 150.0,
          quantity: 4,
          discount: 15.0,
          category: 'ELECTRONICS',
        },
      },
      null,
      2
    );
  });

  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState<RuleSimulationResponse | null>(null);
  const [parseError, setParseError] = useState<string | null>(null);

  if (!isOpen) return null;

  const handleSimulate = async () => {
    setParseError(null);
    let parsedContext: Record<string, unknown> = {};
    try {
      parsedContext = JSON.parse(contextJson);
    } catch (e: unknown) {
      setParseError((e as Error).message);
      return;
    }

    setLoading(true);
    try {
      const resp = await rulesApi.simulateRule({
        rule: ruleId ? undefined : rule,
        ruleId: ruleId,
        context: parsedContext,
      });
      setResult(resp);
    } catch (err: unknown) {
      setResult({
        matched: false,
        durationMs: 0,
        errorDetail: (err as Error).message,
      });
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 overflow-hidden bg-slate-900/50 backdrop-blur-sm flex justify-end animate-fadeIn">
      <div className="w-full max-w-xl bg-white dark:bg-slate-900 h-full shadow-2xl flex flex-col border-l border-slate-200 dark:border-slate-800">
        {/* Header */}
        <div className="p-5 border-b border-slate-200 dark:border-slate-800 flex items-center justify-between">
          <div>
            <h3 className="text-base font-semibold text-slate-900 dark:text-white">
              {t('rules', 'simulationTitle')}
            </h3>
            <p className="text-xs text-slate-500 mt-0.5">
              {t('rules', 'simulationDescription')}
            </p>
          </div>
          <button
            onClick={onClose}
            className="p-1.5 rounded-lg text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Content */}
        <div className="p-6 overflow-y-auto flex-1 space-y-5">
          {/* Rule preview */}
          <div className="p-3 bg-slate-50 dark:bg-slate-800/60 rounded-xl border border-slate-200 dark:border-slate-700 text-xs space-y-1">
            <div className="font-semibold text-slate-800 dark:text-slate-200">{rule.name}</div>
            <div className="text-slate-500 font-mono">
              Trigger: <span className="text-blue-600 dark:text-blue-400">{rule.triggerEvent}</span> • Timing: {rule.timing} • Action: {rule.actionType}
            </div>
          </div>

          {/* Context JSON input */}
          <div className="space-y-2">
            <div className="flex items-center justify-between">
              <label className="text-xs font-semibold text-slate-700 dark:text-slate-300">
                {t('rules', 'contextJson')}
              </label>
              {parseError && (
                <span className="text-xs text-red-500 flex items-center gap-1">
                  <AlertTriangle className="w-3.5 h-3.5" /> Invalid JSON
                </span>
              )}
            </div>
            <textarea
              rows={8}
              value={contextJson}
              onChange={(e) => setContextJson(e.target.value)}
              className="w-full px-3.5 py-2.5 font-mono text-xs bg-slate-900 text-emerald-400 border border-slate-700 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500 shadow-inner"
            />
          </div>

          {/* Simulate Action Button */}
          <button
            type="button"
            disabled={loading}
            onClick={handleSimulate}
            className="w-full py-2.5 px-4 bg-blue-600 hover:bg-blue-700 text-white text-sm font-semibold rounded-xl shadow-md flex items-center justify-center space-x-2 transition-colors disabled:opacity-50"
          >
            <Play className="w-4 h-4 fill-white" />
            <span>{loading ? t('rules', 'simulating') : t('rules', 'simulateBtn')}</span>
          </button>

          {/* Result view */}
          {result && (
            <div className="space-y-3 pt-3 border-t border-slate-200 dark:border-slate-800">
              <h4 className="text-xs font-semibold text-slate-500 uppercase tracking-wider">
                {t('rules', 'simulatedResult')}
              </h4>

              <div
                className={`p-4 rounded-xl border ${
                  result.matched
                    ? 'bg-emerald-50/70 border-emerald-200 dark:bg-emerald-950/20 dark:border-emerald-800'
                    : 'bg-slate-50 border-slate-200 dark:bg-slate-800/60 dark:border-slate-700'
                }`}
              >
                <div className="flex items-center justify-between">
                  <div className="flex items-center space-x-2">
                    {result.matched ? (
                      <CheckCircle2 className="w-5 h-5 text-emerald-600 dark:text-emerald-400" />
                    ) : (
                      <XCircle className="w-5 h-5 text-slate-400" />
                    )}
                    <span
                      className={`text-sm font-semibold ${
                        result.matched
                          ? 'text-emerald-700 dark:text-emerald-400'
                          : 'text-slate-600 dark:text-slate-400'
                      }`}
                    >
                      {result.matched ? t('rules', 'matched') : t('rules', 'notMatched')}
                    </span>
                  </div>

                  <div className="flex items-center space-x-1 text-xs text-slate-500">
                    <Clock className="w-3.5 h-3.5" />
                    <span>{result.durationMs}ms</span>
                  </div>
                </div>

                {result.simulatedError && (
                  <div className="mt-3 p-3 bg-red-50 dark:bg-red-950/30 border border-red-200 dark:border-red-900 rounded-lg text-xs text-red-700 dark:text-red-300 font-mono">
                    <strong>Blocked:</strong> {result.simulatedError}
                  </div>
                )}

                {result.mutatedContext && (
                  <div className="mt-3">
                    <div className="text-xs font-semibold text-slate-600 dark:text-slate-400 mb-1">
                      Mutated Context:
                    </div>
                    <pre className="p-3 bg-slate-900 text-emerald-400 rounded-lg text-xs font-mono overflow-x-auto">
                      {JSON.stringify(result.mutatedContext, null, 2)}
                    </pre>
                  </div>
                )}

                {result.errorDetail && (
                  <div className="mt-3 p-3 bg-amber-50 dark:bg-amber-950/30 border border-amber-200 dark:border-amber-900 rounded-lg text-xs text-amber-700 dark:text-amber-300 font-mono">
                    {result.errorDetail}
                  </div>
                )}
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
