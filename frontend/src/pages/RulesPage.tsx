import React, { useState, useEffect, useMemo } from 'react';
import {
  Plus,
  Search,
  Filter,
  Play,
  History,
  Trash2,
  Edit2,
  Sliders,
  Code,
  ShieldAlert,
  ShieldCheck,
  CheckCircle2,
  XCircle,
} from 'lucide-react';
import { CustomRule, RuleRequest, RuleSummary } from '../types/rules';
import { rulesApi } from '../api/rulesApi';
import { RuleFormModal } from '../components/rules/RuleFormModal';
import { RuleSimulationDrawer } from '../components/rules/RuleSimulationDrawer';
import { RuleTracesModal } from '../components/rules/RuleTracesModal';
import { useI18n } from '../context/I18nContext';

export const RulesPage: React.FC = () => {
  const { t } = useI18n();

  const [rules, setRules] = useState<RuleSummary[]>([]);
  const [loading, setLoading] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [triggerFilter, setTriggerFilter] = useState<string>('ALL');

  // Modal / Drawer states
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [selectedRule, setSelectedRule] = useState<CustomRule | null>(null);

  const [isSimulationOpen, setIsSimulationOpen] = useState(false);
  const [simulationRule, setSimulationRule] = useState<RuleRequest | null>(null);
  const [simulationRuleId, setSimulationRuleId] = useState<string | undefined>();

  const [isTracesOpen, setIsTracesOpen] = useState(false);
  const [tracesRuleId, setTracesRuleId] = useState<string | undefined>();

  const fetchRules = async () => {
    setLoading(true);
    try {
      const data = await rulesApi.listRules(0, 100);
      setRules(data.content || []);
    } catch (e) {
      console.error('Failed to load rules', e);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchRules();
  }, []);

  const handleCreateNew = () => {
    setSelectedRule(null);
    setIsFormOpen(true);
  };

  const handleEdit = async (ruleSummary: RuleSummary) => {
    try {
      const fullRule = await rulesApi.getRule(ruleSummary.id);
      setSelectedRule(fullRule);
      setIsFormOpen(true);
    } catch (e) {
      console.error('Failed to fetch full rule', e);
    }
  };

  const handleToggle = async (id: string) => {
    try {
      await rulesApi.toggleRule(id);
      await fetchRules();
    } catch (e) {
      console.error('Failed to toggle rule', e);
    }
  };

  const handleDelete = async (id: string, name: string) => {
    if (!window.confirm(`Delete rule "${name}"?`)) return;
    try {
      await rulesApi.deleteRule(id);
      await fetchRules();
    } catch (e) {
      console.error('Failed to delete rule', e);
    }
  };

  const handleSave = async (payload: RuleRequest) => {
    if (selectedRule) {
      await rulesApi.updateRule(selectedRule.id, payload);
    } else {
      await rulesApi.createRule(payload);
    }
    await fetchRules();
  };

  const handleOpenSimulation = (rule: RuleRequest, id?: string) => {
    setSimulationRule(rule);
    setSimulationRuleId(id);
    setIsSimulationOpen(true);
  };

  const handleSimulateExisting = async (ruleSummary: RuleSummary) => {
    try {
      const fullRule = await rulesApi.getRule(ruleSummary.id);
      handleOpenSimulation(fullRule, fullRule.id);
    } catch (e) {
      console.error('Failed to load rule for simulation', e);
    }
  };

  const filteredRules = useMemo(() => {
    return rules.filter((rule) => {
      const matchesSearch =
        rule.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
        (rule.description && rule.description.toLowerCase().includes(searchQuery.toLowerCase()));
      const matchesTrigger = triggerFilter === 'ALL' || rule.triggerEvent === triggerFilter;
      return matchesSearch && matchesTrigger;
    });
  }, [rules, searchQuery, triggerFilter]);

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 dark:text-white">
            {t('rules', 'title')}
          </h1>
          <p className="text-sm text-slate-500 mt-1">
            {t('rules', 'description')}
          </p>
        </div>

        <div className="flex items-center space-x-3">
          <button
            onClick={() => {
              setTracesRuleId(undefined);
              setIsTracesOpen(true);
            }}
            className="inline-flex items-center space-x-2 px-4 py-2 text-sm font-semibold text-slate-700 dark:text-slate-300 bg-white dark:bg-slate-800 border border-slate-300 dark:border-slate-700 rounded-xl hover:bg-slate-50 dark:hover:bg-slate-750 transition-colors shadow-sm"
          >
            <History className="w-4 h-4 text-slate-500" />
            <span>{t('rules', 'viewTraces')}</span>
          </button>

          <button
            onClick={handleCreateNew}
            className="inline-flex items-center space-x-2 px-4 py-2 text-sm font-semibold text-white bg-blue-600 hover:bg-blue-700 rounded-xl transition-colors shadow-md"
          >
            <Plus className="w-4 h-4" />
            <span>{t('rules', 'createRule')}</span>
          </button>
        </div>
      </div>

      {/* Filter / Search Bar */}
      <div className="flex flex-col sm:flex-row gap-3">
        <div className="relative flex-1">
          <Search className="w-4 h-4 absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400" />
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder={t('common', 'search')}
            className="w-full pl-10 pr-4 py-2 text-sm bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-xl text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500 shadow-sm"
          />
        </div>

        <div className="flex items-center space-x-2">
          <Filter className="w-4 h-4 text-slate-400" />
          <select
            value={triggerFilter}
            onChange={(e) => setTriggerFilter(e.target.value)}
            className="px-3 py-2 text-sm bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-xl text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500 shadow-sm"
          >
            <option value="ALL">All Trigger Points</option>
            <option value="sale.item_added">sale.item_added</option>
            <option value="sale.finalized">sale.finalized</option>
            <option value="product.created">product.created</option>
            <option value="product.updated">product.updated</option>
            <option value="financial.title_created">financial.title_created</option>
            <option value="financial.title_settled">financial.title_settled</option>
          </select>
        </div>
      </div>

      {/* Table Container */}
      <div className="bg-white dark:bg-slate-800/80 rounded-2xl shadow-sm border border-slate-200 dark:border-slate-700/60 overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm">
            <thead>
              <tr className="border-b border-slate-200 dark:border-slate-700 bg-slate-50/50 dark:bg-slate-900/50 text-xs font-semibold text-slate-500 uppercase tracking-wider">
                <th className="py-3.5 pl-6 pr-3">Rule Name</th>
                <th className="py-3.5 px-3">Trigger & Timing</th>
                <th className="py-3.5 px-3">Mode</th>
                <th className="py-3.5 px-3">Action</th>
                <th className="py-3.5 px-3">Policy</th>
                <th className="py-3.5 px-3">Priority</th>
                <th className="py-3.5 px-3">Status</th>
                <th className="py-3.5 pl-3 pr-6 text-right">{t('common', 'actions')}</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 dark:divide-slate-700/50">
              {filteredRules.length === 0 ? (
                <tr>
                  <td colSpan={8} className="py-12 text-center text-slate-400">
                    {loading ? t('common', 'loading') : t('rules', 'noRules')}
                  </td>
                </tr>
              ) : (
                filteredRules.map((rule) => (
                  <tr
                    key={rule.id}
                    className="hover:bg-slate-50/70 dark:hover:bg-slate-750/30 transition-colors"
                  >
                    <td className="py-4 pl-6 pr-3">
                      <div className="font-semibold text-slate-900 dark:text-white">
                        {rule.name}
                      </div>
                      {rule.description && (
                        <div className="text-xs text-slate-400 truncate max-w-xs mt-0.5">
                          {rule.description}
                        </div>
                      )}
                    </td>

                    <td className="py-4 px-3">
                      <span className="font-mono text-xs px-2 py-0.5 rounded bg-blue-50 dark:bg-blue-950/40 text-blue-700 dark:text-blue-300 border border-blue-200 dark:border-blue-900">
                        {rule.triggerEvent}
                      </span>
                      <span className="ml-1.5 text-xs text-slate-400 font-semibold">
                        ({rule.timing})
                      </span>
                    </td>

                    <td className="py-4 px-3">
                      {rule.authoringMode === 'VISUAL_BUILDER' ? (
                        <span className="inline-flex items-center space-x-1 text-xs text-purple-600 dark:text-purple-400">
                          <Sliders className="w-3.5 h-3.5" />
                          <span>Visual</span>
                        </span>
                      ) : (
                        <span className="inline-flex items-center space-x-1 text-xs text-emerald-600 dark:text-emerald-400">
                          <Code className="w-3.5 h-3.5" />
                          <span>SpEL DSL</span>
                        </span>
                      )}
                    </td>

                    <td className="py-4 px-3 font-mono text-xs text-slate-600 dark:text-slate-300">
                      {rule.actionType}
                    </td>

                    <td className="py-4 px-3">
                      {rule.errorHandlingPolicy === 'ABORT_TRANSACTION' ? (
                        <span className="inline-flex items-center space-x-1 text-xs text-amber-600 dark:text-amber-400 font-medium">
                          <ShieldAlert className="w-3.5 h-3.5" />
                          <span>Strict</span>
                        </span>
                      ) : (
                        <span className="inline-flex items-center space-x-1 text-xs text-slate-500 font-medium">
                          <ShieldCheck className="w-3.5 h-3.5" />
                          <span>Permissive</span>
                        </span>
                      )}
                    </td>

                    <td className="py-4 px-3 font-mono text-xs text-slate-500">
                      {rule.priority}
                    </td>

                    <td className="py-4 px-3">
                      <button
                        type="button"
                        onClick={() => handleToggle(rule.id)}
                        className={`inline-flex items-center space-x-1 px-2.5 py-1 rounded-full text-xs font-semibold transition-colors ${
                          rule.isActive
                            ? 'bg-emerald-50 text-emerald-700 dark:bg-emerald-950/40 dark:text-emerald-400 border border-emerald-200 dark:border-emerald-800'
                            : 'bg-slate-100 text-slate-500 dark:bg-slate-800 dark:text-slate-400 border border-slate-200 dark:border-slate-700'
                        }`}
                      >
                        {rule.isActive ? (
                          <>
                            <CheckCircle2 className="w-3 h-3" />
                            <span>{t('common', 'active')}</span>
                          </>
                        ) : (
                          <>
                            <XCircle className="w-3 h-3" />
                            <span>{t('common', 'inactive')}</span>
                          </>
                        )}
                      </button>
                    </td>

                    <td className="py-4 pl-3 pr-6 text-right">
                      <div className="flex items-center justify-end space-x-1">
                        <button
                          title="Simulate / Dry-run"
                          onClick={() => handleSimulateExisting(rule)}
                          className="p-1.5 text-slate-400 hover:text-blue-600 dark:hover:text-blue-400 hover:bg-slate-100 dark:hover:bg-slate-700 rounded-lg transition-colors"
                        >
                          <Play className="w-4 h-4" />
                        </button>
                        <button
                          title="View Traces"
                          onClick={() => {
                            setTracesRuleId(rule.id);
                            setIsTracesOpen(true);
                          }}
                          className="p-1.5 text-slate-400 hover:text-purple-600 dark:hover:text-purple-400 hover:bg-slate-100 dark:hover:bg-slate-700 rounded-lg transition-colors"
                        >
                          <History className="w-4 h-4" />
                        </button>
                        <button
                          title="Edit Rule"
                          onClick={() => handleEdit(rule)}
                          className="p-1.5 text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-700 rounded-lg transition-colors"
                        >
                          <Edit2 className="w-4 h-4" />
                        </button>
                        <button
                          title="Delete Rule"
                          onClick={() => handleDelete(rule.id, rule.name)}
                          className="p-1.5 text-slate-400 hover:text-red-600 dark:hover:text-red-400 hover:bg-red-50 dark:hover:bg-red-950/30 rounded-lg transition-colors"
                        >
                          <Trash2 className="w-4 h-4" />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Form Modal */}
      <RuleFormModal
        isOpen={isFormOpen}
        onClose={() => setIsFormOpen(false)}
        onSave={handleSave}
        onOpenSimulation={(draft) => handleOpenSimulation(draft)}
        initialRule={selectedRule}
      />

      {/* Dry-run simulation drawer */}
      {simulationRule && (
        <RuleSimulationDrawer
          isOpen={isSimulationOpen}
          onClose={() => setIsSimulationOpen(false)}
          rule={simulationRule}
          ruleId={simulationRuleId}
        />
      )}

      {/* Execution traces modal */}
      <RuleTracesModal
        isOpen={isTracesOpen}
        onClose={() => setIsTracesOpen(false)}
        ruleId={tracesRuleId}
      />
    </div>
  );
};
