import React, { useState } from 'react';
import { Code2, Info, Search } from 'lucide-react';
import { FieldSearchModal } from './FieldSearchModal';
import { EntityField } from '../../types/entityFields';
import { useI18n } from '../../context/I18nContext';

interface DslCodeEditorProps {
  expression: string;
  onChange: (val: string) => void;
  triggerEvent?: string;
}

const CHIPS = [
  '#sale',
  '#item',
  '#customer',
  '#totalAmount',
  '#discountPercentage',
  '#status',
  'and',
  'or',
  '==',
  '!=',
  '>',
  '<',
];

export const DslCodeEditor: React.FC<DslCodeEditorProps> = ({
  expression,
  onChange,
  triggerEvent = 'sale.item_added',
}) => {
  const { t } = useI18n();
  const [isCatalogOpen, setIsCatalogOpen] = useState(false);

  const insertChip = (chip: string) => {
    const space = expression.length > 0 && !expression.endsWith(' ') ? ' ' : '';
    onChange(`${expression}${space}${chip} `);
  };

  const handleSelectField = (field: EntityField) => {
    insertChip(`#${field.path}`);
  };

  return (
    <div className="space-y-3">
      <div className="flex items-center justify-between text-xs text-slate-500">
        <div className="flex items-center space-x-1.5">
          <Code2 className="w-4 h-4 text-blue-500" />
          <span>Spring Expression Language (SpEL)</span>
        </div>
        <div className="flex items-center space-x-2">
          <button
            type="button"
            onClick={() => setIsCatalogOpen(true)}
            className="inline-flex items-center space-x-1 px-2.5 py-1 text-xs font-semibold text-blue-600 dark:text-blue-400 bg-blue-50 dark:bg-blue-950/40 border border-blue-200 dark:border-blue-900 rounded-lg hover:bg-blue-100 transition-colors shadow-2xs"
          >
            <Search className="w-3 h-3" />
            <span>{t('rules', 'browseFields')}</span>
          </button>
        </div>
      </div>

      <textarea
        rows={6}
        value={expression}
        onChange={(e) => onChange(e.target.value)}
        placeholder="#sale.totalAmount > 1000.0 and #sale.customer.tier == 'VIP'"
        className="w-full px-3.5 py-2.5 font-mono text-sm bg-slate-900 text-emerald-400 border border-slate-700 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500 shadow-inner"
      />

      <div className="flex items-center flex-wrap gap-1.5">
        <span className="text-xs text-slate-400 mr-1 flex items-center gap-1">
          <Info className="w-3 h-3" /> Insert:
        </span>
        {CHIPS.map((chip) => (
          <button
            key={chip}
            type="button"
            onClick={() => insertChip(chip)}
            className="px-2 py-0.5 text-xs font-mono bg-slate-100 hover:bg-slate-200 dark:bg-slate-800 dark:hover:bg-slate-700 text-slate-700 dark:text-slate-300 rounded border border-slate-200 dark:border-slate-700 transition-colors"
          >
            {chip}
          </button>
        ))}
      </div>

      <FieldSearchModal
        isOpen={isCatalogOpen}
        onClose={() => setIsCatalogOpen(false)}
        onSelectField={handleSelectField}
        currentTrigger={triggerEvent}
      />
    </div>
  );
};
