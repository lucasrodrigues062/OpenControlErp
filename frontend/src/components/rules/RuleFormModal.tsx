import React, { useState, useEffect } from 'react';
import { X, Play, Sliders, Code, Maximize2, Minimize2 } from 'lucide-react';
import {
  AuthoringMode,
  CustomRule,
  ErrorHandlingPolicy,
  RuleActionType,
  RuleRequest,
  RuleTiming,
  TriggerPoint,
} from '../../types/rules';
import { VisualConditionBuilder } from './VisualConditionBuilder';
import { DslCodeEditor } from './DslCodeEditor';
import { useI18n } from '../../context/I18nContext';

interface RuleFormModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSave: (payload: RuleRequest) => Promise<void>;
  onOpenSimulation: (rule: RuleRequest) => void;
  initialRule?: CustomRule | null;
}

const TRIGGER_OPTIONS: { value: TriggerPoint; label: string; domain: string }[] = [
  { value: 'sale.item_added', label: 'Sale: Item Added', domain: 'Sales' },
  { value: 'sale.item_removed', label: 'Sale: Item Removed', domain: 'Sales' },
  { value: 'sale.created', label: 'Sale: Order Created', domain: 'Sales' },
  { value: 'sale.finalized', label: 'Sale: Finalized / Checkout', domain: 'Sales' },
  { value: 'sale.cancelled', label: 'Sale: Cancelled', domain: 'Sales' },
  { value: 'product.created', label: 'Product: Created', domain: 'Catalog' },
  { value: 'product.updated', label: 'Product: Updated', domain: 'Catalog' },
  { value: 'stock.movement_recorded', label: 'Stock: Movement Recorded', domain: 'Inventory' },
  { value: 'financial.title_created', label: 'Financial: Title Created', domain: 'Finance' },
  { value: 'financial.title_settled', label: 'Financial: Title Settled', domain: 'Finance' },
  { value: 'financial.title_cancelled', label: 'Financial: Title Cancelled', domain: 'Finance' },
];

const ACTION_OPTIONS: { value: RuleActionType; label: string; hint: string }[] = [
  {
    value: 'BLOCK_OPERATION',
    label: 'Block Operation (Validation Error)',
    hint: 'Aborts the transaction and surfaces a custom violation message to the user.',
  },
  {
    value: 'APPLY_DISCOUNT',
    label: 'Apply Automatic Discount',
    hint: 'Calculates and injects a discount rate into the current transaction payload.',
  },
  {
    value: 'SET_FIELD_VALUE',
    label: 'Set / Mutate Field Value',
    hint: 'Transforms or auto-assigns a property (e.g. shipping fee, status tag) before saving.',
  },
  {
    value: 'DISPATCH_NOTIFICATION',
    label: 'Dispatch Event Notification',
    hint: 'Triggers an asynchronous notification or webhook after transaction commits.',
  },
  {
    value: 'CUSTOM_ACTION',
    label: 'Custom Webhook Action',
    hint: 'Executes integration hooks with third-party systems.',
  },
];

export const RuleFormModal: React.FC<RuleFormModalProps> = ({
  isOpen,
  onClose,
  onSave,
  onOpenSimulation,
  initialRule,
}) => {
  const { t } = useI18n();

  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [triggerEvent, setTriggerEvent] = useState<TriggerPoint>('sale.item_added');
  const [timing, setTiming] = useState<RuleTiming>('BEFORE');
  const [authoringMode, setAuthoringMode] = useState<AuthoringMode>('VISUAL_BUILDER');
  const [conditionExpression, setConditionExpression] = useState('');
  const [actionType, setActionType] = useState<RuleActionType>('BLOCK_OPERATION');
  const [actionPayload, setActionPayload] = useState('');
  const [priority, setPriority] = useState(100);
  const [isActive, setIsActive] = useState(true);
  const [errorHandlingPolicy, setErrorHandlingPolicy] = useState<ErrorHandlingPolicy>('ABORT_TRANSACTION');
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Fullscreen / Expanded modal state
  const [isFullscreen, setIsFullscreen] = useState(false);
  const [isConditionExpanded, setIsConditionExpanded] = useState(false);

  useEffect(() => {
    if (initialRule) {
      setName(initialRule.name);
      setDescription(initialRule.description || '');
      setTriggerEvent(initialRule.triggerEvent);
      setTiming(initialRule.timing);
      setAuthoringMode(initialRule.authoringMode);
      setConditionExpression(initialRule.conditionExpression);
      setActionType(initialRule.actionType);
      setActionPayload(initialRule.actionPayload || '');
      setPriority(initialRule.priority);
      setIsActive(initialRule.isActive);
      setErrorHandlingPolicy(initialRule.errorHandlingPolicy);
    } else {
      setName('');
      setDescription('');
      setTriggerEvent('sale.item_added');
      setTiming('BEFORE');
      setAuthoringMode('VISUAL_BUILDER');
      setConditionExpression(
        JSON.stringify(
          {
            conjunction: 'AND',
            conditions: [{ field: 'sale.discountPercentage', operator: 'GREATER_THAN', value: 20 }],
          },
          null,
          2
        )
      );
      setActionType('BLOCK_OPERATION');
      setActionPayload('{"message": "Discount exceeds maximum allowed of 20%"}');
      setPriority(100);
      setIsActive(true);
      setErrorHandlingPolicy('ABORT_TRANSACTION');
    }
  }, [initialRule, isOpen]);

  if (!isOpen) return null;

  const currentPayload: RuleRequest = {
    name,
    description: description.trim() ? description : undefined,
    triggerEvent,
    timing,
    authoringMode,
    conditionExpression,
    actionType,
    actionPayload: actionPayload.trim() ? actionPayload : undefined,
    priority,
    isActive,
    errorHandlingPolicy,
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsSubmitting(true);
    try {
      await onSave(currentPayload);
      onClose();
    } finally {
      setIsSubmitting(false);
    }
  };

  const selectedActionInfo = ACTION_OPTIONS.find((a) => a.value === actionType);

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-2 sm:p-4 bg-slate-900/60 backdrop-blur-sm animate-fadeIn">
      <div
        className={`w-full bg-white dark:bg-slate-900 rounded-2xl shadow-2xl border border-slate-200 dark:border-slate-800 flex flex-col transition-all duration-200 ${
          isFullscreen
            ? 'w-[98vw] h-[96vh] max-w-none max-h-none'
            : 'max-w-5xl max-h-[90vh]'
        }`}
      >
        {/* Header */}
        <div className="px-6 py-4 border-b border-slate-200 dark:border-slate-800 flex items-center justify-between">
          <div className="flex items-center space-x-3">
            <div className="p-2 bg-blue-50 dark:bg-blue-950/40 border border-blue-200 dark:border-blue-900 rounded-xl text-blue-600 dark:text-blue-400">
              <Sliders className="w-5 h-5" />
            </div>
            <div>
              <h2 className="text-base sm:text-lg font-bold text-slate-900 dark:text-white">
                {initialRule ? t('rules', 'editRule') : t('rules', 'createRule')}
              </h2>
              <p className="text-xs text-slate-500">
                {t('rules', 'description')}
              </p>
            </div>
          </div>

          <div className="flex items-center space-x-1.5">
            <button
              type="button"
              onClick={() => setIsFullscreen(!isFullscreen)}
              title={isFullscreen ? t('rules', 'normalView') : t('rules', 'fullscreen')}
              className="p-2 rounded-lg text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors"
            >
              {isFullscreen ? (
                <Minimize2 className="w-4 h-4" />
              ) : (
                <Maximize2 className="w-4 h-4" />
              )}
            </button>
            <button
              type="button"
              onClick={onClose}
              className="p-2 rounded-lg text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors"
            >
              <X className="w-5 h-5" />
            </button>
          </div>
        </div>

        {/* Form Body: responsive 2-column when expanded */}
        <form onSubmit={handleSubmit} className="overflow-y-auto p-6 flex-1 flex flex-col">
          <div
            className={`gap-6 ${
              isFullscreen
                ? 'grid grid-cols-1 lg:grid-cols-12 flex-1'
                : 'space-y-5'
            }`}
          >
            {/* Column 1 / Top Section: Rule Identity & Metadata */}
            <div
              className={`space-y-4 ${
                isFullscreen ? 'lg:col-span-5 border-r border-slate-200 dark:border-slate-800 pr-6 overflow-y-auto' : ''
              }`}
            >
              {/* Name & Trigger */}
              <div className="space-y-3">
                <div>
                  <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
                    {t('rules', 'ruleName')} *
                  </label>
                  <input
                    type="text"
                    required
                    value={name}
                    placeholder={t('rules', 'ruleNamePlaceholder')}
                    onChange={(e) => setName(e.target.value)}
                    className="w-full px-3.5 py-2 text-sm bg-slate-50 dark:bg-slate-800 border border-slate-300 dark:border-slate-700 rounded-xl text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500 font-medium"
                  />
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
                    {t('rules', 'trigger')} *
                  </label>
                  <select
                    value={triggerEvent}
                    onChange={(e) => setTriggerEvent(e.target.value as TriggerPoint)}
                    className="w-full px-3.5 py-2 text-sm bg-slate-50 dark:bg-slate-800 border border-slate-300 dark:border-slate-700 rounded-xl text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500 font-medium"
                  >
                    {TRIGGER_OPTIONS.map((opt) => (
                      <option key={opt.value} value={opt.value}>
                        [{opt.domain}] {opt.label}
                      </option>
                    ))}
                  </select>
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
                    {t('rules', 'ruleDescription')}
                  </label>
                  <textarea
                    rows={2}
                    value={description}
                    placeholder={t('rules', 'ruleDescriptionPlaceholder')}
                    onChange={(e) => setDescription(e.target.value)}
                    className="w-full px-3.5 py-2 text-sm bg-slate-50 dark:bg-slate-800 border border-slate-300 dark:border-slate-700 rounded-xl text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500"
                  />
                </div>
              </div>

              {/* Execution Timing & Policy Settings */}
              <div className="grid grid-cols-2 gap-3 p-3.5 bg-slate-50 dark:bg-slate-800/50 rounded-xl border border-slate-200 dark:border-slate-800 text-xs">
                <div>
                  <label className="block font-semibold text-slate-600 dark:text-slate-400 mb-1">
                    {t('rules', 'timing')}
                  </label>
                  <select
                    value={timing}
                    onChange={(e) => setTiming(e.target.value as RuleTiming)}
                    className="w-full px-2.5 py-1.5 bg-white dark:bg-slate-800 border border-slate-300 dark:border-slate-700 rounded-lg text-slate-900 dark:text-white font-medium"
                  >
                    <option value="BEFORE">BEFORE (Pre-Commit)</option>
                    <option value="AFTER">AFTER (Post-Commit)</option>
                  </select>
                </div>

                <div>
                  <label className="block font-semibold text-slate-600 dark:text-slate-400 mb-1">
                    {t('rules', 'policy')}
                  </label>
                  <select
                    value={errorHandlingPolicy}
                    onChange={(e) => setErrorHandlingPolicy(e.target.value as ErrorHandlingPolicy)}
                    className="w-full px-2.5 py-1.5 bg-white dark:bg-slate-800 border border-slate-300 dark:border-slate-700 rounded-lg text-slate-900 dark:text-white font-medium"
                  >
                    <option value="ABORT_TRANSACTION">Strict (Abort)</option>
                    <option value="LOG_AND_CONTINUE">Permissive (Log)</option>
                  </select>
                </div>

                <div>
                  <label className="block font-semibold text-slate-600 dark:text-slate-400 mb-1">
                    {t('rules', 'priority')}
                  </label>
                  <input
                    type="number"
                    value={priority}
                    onChange={(e) => setPriority(Number(e.target.value))}
                    className="w-full px-2.5 py-1.5 bg-white dark:bg-slate-800 border border-slate-300 dark:border-slate-700 rounded-lg text-slate-900 dark:text-white font-mono"
                  />
                </div>

                <div className="flex flex-col justify-center">
                  <label className="block font-semibold text-slate-600 dark:text-slate-400 mb-1">
                    Status
                  </label>
                  <label className="inline-flex items-center space-x-2 cursor-pointer mt-1">
                    <input
                      type="checkbox"
                      checked={isActive}
                      onChange={(e) => setIsActive(e.target.checked)}
                      className="rounded text-blue-600 focus:ring-blue-500 w-4 h-4"
                    />
                    <span className="font-medium text-slate-700 dark:text-slate-300">
                      {isActive ? t('common', 'active') : t('common', 'inactive')}
                    </span>
                  </label>
                </div>
              </div>

              {/* Action Block */}
              <div className="space-y-2 p-3.5 bg-slate-50 dark:bg-slate-800/50 rounded-xl border border-slate-200 dark:border-slate-800">
                <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300">
                  {t('rules', 'actionType')} *
                </label>
                <select
                  value={actionType}
                  onChange={(e) => setActionType(e.target.value as RuleActionType)}
                  className="w-full px-3 py-2 text-xs bg-white dark:bg-slate-800 border border-slate-300 dark:border-slate-700 rounded-lg text-slate-900 dark:text-white font-medium"
                >
                  {ACTION_OPTIONS.map((act) => (
                    <option key={act.value} value={act.value}>
                      {act.label}
                    </option>
                  ))}
                </select>

                {selectedActionInfo && (
                  <p className="text-[11px] text-slate-500 italic">
                    {selectedActionInfo.hint}
                  </p>
                )}

                <div className="pt-2">
                  <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
                    {t('rules', 'actionPayload')}
                  </label>
                  <input
                    type="text"
                    value={actionPayload}
                    placeholder={t('rules', 'actionPayloadPlaceholder')}
                    onChange={(e) => setActionPayload(e.target.value)}
                    className="w-full px-3 py-2 text-xs bg-white dark:bg-slate-800 border border-slate-300 dark:border-slate-700 rounded-lg text-slate-900 dark:text-white font-mono"
                  />
                </div>
              </div>
            </div>

            {/* Column 2 / Condition Workspace */}
            <div
              className={`space-y-3 flex flex-col ${
                isFullscreen ? 'lg:col-span-7 overflow-y-auto' : ''
              }`}
            >
              {/* Mode switch header */}
              <div className="flex items-center justify-between">
                <div>
                  <h3 className="text-sm font-bold text-slate-900 dark:text-white flex items-center gap-1.5">
                    <span>{t('rules', 'condition')}</span>
                  </h3>
                  <p className="text-[11px] text-slate-500">
                    Define criteria that evaluate to true or false.
                  </p>
                </div>

                <div className="inline-flex rounded-lg border border-slate-200 dark:border-slate-700 bg-slate-100 dark:bg-slate-800 p-0.5">
                  <button
                    type="button"
                    onClick={() => setAuthoringMode('VISUAL_BUILDER')}
                    className={`flex items-center space-x-1.5 px-3 py-1 text-xs font-semibold rounded-md transition-colors ${
                      authoringMode === 'VISUAL_BUILDER'
                        ? 'bg-white dark:bg-slate-900 text-blue-600 dark:text-blue-400 shadow-sm'
                        : 'text-slate-500 hover:text-slate-900 dark:hover:text-white'
                    }`}
                  >
                    <Sliders className="w-3.5 h-3.5" />
                    <span>{t('rules', 'visualMode')}</span>
                  </button>
                  <button
                    type="button"
                    onClick={() => setAuthoringMode('ADVANCED_DSL')}
                    className={`flex items-center space-x-1.5 px-3 py-1 text-xs font-semibold rounded-md transition-colors ${
                      authoringMode === 'ADVANCED_DSL'
                        ? 'bg-white dark:bg-slate-900 text-blue-600 dark:text-blue-400 shadow-sm'
                        : 'text-slate-500 hover:text-slate-900 dark:hover:text-white'
                    }`}
                  >
                    <Code className="w-3.5 h-3.5" />
                    <span>{t('rules', 'dslMode')}</span>
                  </button>
                </div>
              </div>

              {/* Condition Editor */}
              <div className="flex-1">
                {authoringMode === 'VISUAL_BUILDER' ? (
                  <VisualConditionBuilder
                    initialJson={conditionExpression}
                    triggerEvent={triggerEvent}
                    onChange={setConditionExpression}
                    isExpanded={isConditionExpanded}
                    onToggleExpand={() => setIsConditionExpanded(!isConditionExpanded)}
                  />
                ) : (
                  <DslCodeEditor
                    expression={conditionExpression}
                    onChange={setConditionExpression}
                  />
                )}
              </div>
            </div>
          </div>

          {/* Modal Footer */}
          <div className="flex items-center justify-between pt-5 mt-4 border-t border-slate-200 dark:border-slate-800">
            <button
              type="button"
              onClick={() => onOpenSimulation(currentPayload)}
              className="inline-flex items-center space-x-1.5 px-3.5 py-2 text-xs font-semibold text-blue-600 dark:text-blue-400 bg-blue-50 dark:bg-blue-950/40 hover:bg-blue-100 dark:hover:bg-blue-900/40 rounded-xl transition-colors border border-blue-200 dark:border-blue-900"
            >
              <Play className="w-3.5 h-3.5 fill-current" />
              <span>{t('rules', 'testRule')}</span>
            </button>

            <div className="flex items-center space-x-2.5">
              <button
                type="button"
                onClick={onClose}
                className="px-4 py-2 text-sm font-semibold text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800 rounded-xl transition-colors"
              >
                {t('common', 'cancel')}
              </button>
              <button
                type="submit"
                disabled={isSubmitting || !name || !conditionExpression}
                className="px-5 py-2 text-sm font-semibold text-white bg-blue-600 hover:bg-blue-700 rounded-xl shadow-md transition-colors disabled:opacity-50"
              >
                {isSubmitting ? t('common', 'loading') : t('common', 'save')}
              </button>
            </div>
          </div>
        </form>
      </div>
    </div>
  );
};
