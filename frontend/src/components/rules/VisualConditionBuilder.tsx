import React, { useState, useEffect } from 'react';
import { Plus, Trash2, ChevronDown, ChevronUp, Eye, Search } from 'lucide-react';
import { ComparisonOperator, TriggerPoint, VisualCondition } from '../../types/rules';
import { EntityField } from '../../types/entityFields';
import { FieldSearchModal } from './FieldSearchModal';
import { useI18n } from '../../context/I18nContext';

interface VisualConditionBuilderProps {
  initialJson: string;
  triggerEvent?: TriggerPoint;
  onChange: (jsonStr: string) => void;
  isExpanded?: boolean;
  onToggleExpand?: () => void;
}

const OPERATORS: { value: ComparisonOperator; label: string; symbol: string }[] = [
  { value: 'EQUALS', label: 'Equals (==)', symbol: '==' },
  { value: 'NOT_EQUALS', label: 'Not Equals (!=)', symbol: '!=' },
  { value: 'GREATER_THAN', label: 'Greater Than (>)', symbol: '>' },
  { value: 'LESS_THAN', label: 'Less Than (<)', symbol: '<' },
  { value: 'GREATER_OR_EQUAL', label: 'Greater or Equal (>=)', symbol: '>=' },
  { value: 'LESS_OR_EQUAL', label: 'Less or Equal (<=)', symbol: '<=' },
  { value: 'CONTAINS', label: 'Contains text', symbol: 'contains' },
  { value: 'IN', label: 'Matches any in list', symbol: 'in' },
];

export const VisualConditionBuilder: React.FC<VisualConditionBuilderProps> = ({
  initialJson,
  triggerEvent = 'sale.item_added',
  onChange,
  isExpanded = false,
  onToggleExpand,
}) => {
  const { t } = useI18n();
  const [conjunction, setConjunction] = useState<'AND' | 'OR'>('AND');
  const [conditions, setConditions] = useState<VisualCondition[]>([
    { field: 'sale.totalAmount', operator: 'GREATER_THAN', value: 1000 },
  ]);

  // Field Catalog search modal
  const [isSearchOpen, setIsSearchOpen] = useState(false);
  const [searchTargetIndex, setSearchTargetIndex] = useState<number | null>(null);

  useEffect(() => {
    try {
      if (initialJson && initialJson.trim().startsWith('{')) {
        const parsed = JSON.parse(initialJson);
        if (parsed.conditions && Array.isArray(parsed.conditions)) {
          setConjunction(parsed.conjunction || 'AND');
          setConditions(parsed.conditions);
        } else if (parsed.field) {
          setConditions([parsed]);
        }
      }
    } catch {
      // Ignore parse error on raw expressions
    }
  }, [initialJson]);

  const updateSerialized = (newConjunction: 'AND' | 'OR', newConditions: VisualCondition[]) => {
    const payload = {
      conjunction: newConjunction,
      conditions: newConditions,
    };
    onChange(JSON.stringify(payload, null, 2));
  };

  const handleConjunctionChange = (c: 'AND' | 'OR') => {
    setConjunction(c);
    updateSerialized(c, conditions);
  };

  const handleConditionChange = (
    index: number,
    field: keyof VisualCondition,
    val: string | number | boolean
  ) => {
    const next = [...conditions];
    next[index] = { ...next[index], [field]: val };
    setConditions(next);
    updateSerialized(conjunction, next);
  };

  const addCondition = (defaultField?: string) => {
    const next: VisualCondition[] = [
      ...conditions,
      { field: defaultField || 'sale.totalAmount', operator: 'GREATER_THAN', value: '' },
    ];
    setConditions(next);
    updateSerialized(conjunction, next);
  };

  const removeCondition = (index: number) => {
    if (conditions.length <= 1) return;
    const next = conditions.filter((_, i) => i !== index);
    setConditions(next);
    updateSerialized(conjunction, next);
  };

  const handleSelectFieldFromCatalog = (field: EntityField) => {
    if (searchTargetIndex !== null && searchTargetIndex < conditions.length) {
      const next = [...conditions];
      next[searchTargetIndex] = {
        field: field.path,
        operator: field.defaultOperator,
        value: field.example !== undefined ? String(field.example) : '',
      };
      setConditions(next);
      updateSerialized(conjunction, next);
    } else {
      addCondition(field.path);
    }
    setSearchTargetIndex(null);
  };

  // Compile simulated preview string
  const previewExpression = conditions
    .map((c) => {
      const op = OPERATORS.find((o) => o.value === c.operator)?.symbol || '==';
      const val = typeof c.value === 'string' && isNaN(Number(c.value)) ? `'${c.value}'` : c.value;
      return `#${c.field} ${op} ${val}`;
    })
    .join(conjunction === 'AND' ? ' and ' : ' or ');

  return (
    <div
      className={`space-y-4 p-5 bg-slate-50 dark:bg-slate-900/60 rounded-2xl border border-slate-200 dark:border-slate-800 transition-all ${
        isExpanded ? 'shadow-lg ring-1 ring-blue-500/20' : ''
      }`}
    >
      {/* Controls Bar */}
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="flex items-center space-x-3">
          <span className="text-xs font-semibold text-slate-500 uppercase tracking-wider">
            Match:
          </span>
          <div className="inline-flex rounded-lg border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 p-0.5 shadow-sm">
            <button
              type="button"
              onClick={() => handleConjunctionChange('AND')}
              className={`px-3 py-1 text-xs font-semibold rounded-md transition-colors ${
                conjunction === 'AND'
                  ? 'bg-blue-600 text-white shadow-sm'
                  : 'text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white'
              }`}
            >
              ALL (AND)
            </button>
            <button
              type="button"
              onClick={() => handleConjunctionChange('OR')}
              className={`px-3 py-1 text-xs font-semibold rounded-md transition-colors ${
                conjunction === 'OR'
                  ? 'bg-blue-600 text-white shadow-sm'
                  : 'text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white'
              }`}
            >
              ANY (OR)
            </button>
          </div>
        </div>

        <div className="flex items-center space-x-2">
          {/* Browse Fields Catalog button */}
          <button
            type="button"
            onClick={() => {
              setSearchTargetIndex(null);
              setIsSearchOpen(true);
            }}
            className="inline-flex items-center space-x-1.5 px-3 py-1 text-xs font-semibold text-slate-700 dark:text-slate-200 bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-lg hover:border-blue-400 transition-colors shadow-2xs"
          >
            <Search className="w-3.5 h-3.5 text-blue-500" />
            <span>{t('rules', 'browseFields')}</span>
          </button>

          {onToggleExpand && (
            <button
              type="button"
              onClick={onToggleExpand}
              className="inline-flex items-center space-x-1 px-2.5 py-1 text-xs font-medium text-slate-600 dark:text-slate-300 bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-lg hover:bg-slate-100 transition-colors"
            >
              {isExpanded ? (
                <>
                  <ChevronUp className="w-3.5 h-3.5 text-blue-500" />
                  <span>{t('rules', 'collapse')}</span>
                </>
              ) : (
                <>
                  <ChevronDown className="w-3.5 h-3.5 text-blue-500" />
                  <span>{t('rules', 'expand')}</span>
                </>
              )}
            </button>
          )}

          <button
            type="button"
            onClick={() => addCondition()}
            className="inline-flex items-center space-x-1.5 px-3 py-1 text-xs font-semibold text-blue-600 hover:text-blue-700 dark:text-blue-400 bg-blue-50 dark:bg-blue-950/40 border border-blue-200 dark:border-blue-900/60 rounded-lg shadow-sm hover:bg-blue-100 transition-colors"
          >
            <Plus className="w-3.5 h-3.5" />
            <span>{t('rules', 'addCondition')}</span>
          </button>
        </div>
      </div>

      {/* Conditions Rows */}
      <div className="space-y-3 pt-1">
        {conditions.map((cond, idx) => (
          <div
            key={idx}
            className="relative flex flex-col sm:flex-row items-stretch sm:items-center gap-2.5 bg-white dark:bg-slate-800 p-3.5 rounded-xl border border-slate-200 dark:border-slate-700 shadow-sm transition-all hover:border-slate-300 dark:hover:border-slate-600"
          >
            {/* Condition index / connector badge */}
            <div className="flex items-center justify-between sm:justify-start space-x-2">
              <span className="w-6 h-6 rounded-full bg-slate-100 dark:bg-slate-700 flex items-center justify-center text-[11px] font-bold text-slate-600 dark:text-slate-300 font-mono">
                {idx + 1}
              </span>
              <span className="text-xs text-slate-400 sm:hidden">
                {idx === 0 ? 'IF' : conjunction}
              </span>
            </div>

            {/* Field Input + Search button */}
            <div className="flex-1 flex items-center space-x-1">
              <input
                type="text"
                value={cond.field}
                placeholder="e.g. sale.totalAmount"
                onChange={(e) => handleConditionChange(idx, 'field', e.target.value)}
                className="w-full px-3 py-2 text-xs bg-slate-50 dark:bg-slate-900 border border-slate-300 dark:border-slate-700 rounded-lg text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500 font-mono font-medium"
              />
              <button
                type="button"
                title={t('rules', 'browseFields')}
                onClick={() => {
                  setSearchTargetIndex(idx);
                  setIsSearchOpen(true);
                }}
                className="p-2 text-slate-400 hover:text-blue-600 dark:hover:text-blue-400 bg-slate-100 dark:bg-slate-900 rounded-lg border border-slate-300 dark:border-slate-700 hover:border-blue-400 transition-colors"
              >
                <Search className="w-3.5 h-3.5" />
              </button>
            </div>

            {/* Operator Dropdown */}
            <select
              value={cond.operator}
              onChange={(e) =>
                handleConditionChange(idx, 'operator', e.target.value as ComparisonOperator)
              }
              className="px-3 py-2 text-xs bg-slate-50 dark:bg-slate-900 border border-slate-300 dark:border-slate-700 rounded-lg text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500 font-medium"
            >
              {OPERATORS.map((op) => (
                <option key={op.value} value={op.value}>
                  {op.label}
                </option>
              ))}
            </select>

            {/* Expected Value Input */}
            <div className="flex-1">
              <input
                type="text"
                value={String(cond.value)}
                placeholder="1000.0 or 'VIP'"
                onChange={(e) => handleConditionChange(idx, 'value', e.target.value)}
                className="w-full px-3 py-2 text-xs bg-slate-50 dark:bg-slate-900 border border-slate-300 dark:border-slate-700 rounded-lg text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500 font-mono"
              />
            </div>

            {/* Delete button */}
            <button
              type="button"
              disabled={conditions.length <= 1}
              onClick={() => removeCondition(idx)}
              className={`p-2 rounded-lg text-slate-400 hover:text-red-500 hover:bg-red-50 dark:hover:bg-red-950/30 transition-colors self-end sm:self-center ${
                conditions.length <= 1 ? 'opacity-20 cursor-not-allowed' : ''
              }`}
            >
              <Trash2 className="w-4 h-4" />
            </button>
          </div>
        ))}
      </div>

      {/* Live Preview Bar */}
      <div className="pt-2">
        <div className="flex items-center space-x-1.5 text-[11px] font-semibold text-slate-500 uppercase tracking-wider mb-1">
          <Eye className="w-3.5 h-3.5 text-blue-500" />
          <span>{t('rules', 'generatedLogic')}</span>
        </div>
        <div className="p-2.5 bg-slate-900 text-emerald-400 font-mono text-xs rounded-xl border border-slate-800 shadow-inner overflow-x-auto select-all">
          ({previewExpression})
        </div>
      </div>

      {/* Field Search Modal */}
      <FieldSearchModal
        isOpen={isSearchOpen}
        onClose={() => setIsSearchOpen(false)}
        onSelectField={handleSelectFieldFromCatalog}
        currentTrigger={triggerEvent}
      />
    </div>
  );
};
