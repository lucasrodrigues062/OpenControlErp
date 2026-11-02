import React, { useState, useEffect } from 'react';
import { CustomFieldDefinition } from '../../types/customFields';
import { customFieldsApi } from '../../api/customFieldsApi';
import { useI18n } from '../../context/I18nContext';

interface CustomFieldDisplayCardProps {
  targetEntity: string;
  values: Record<string, any>;
  className?: string;
}

export const CustomFieldDisplayCard: React.FC<CustomFieldDisplayCardProps> = ({
  targetEntity,
  values,
  className = '',
}) => {
  const { t } = useI18n();
  const [definitions, setDefinitions] = useState<CustomFieldDefinition[]>([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    let isMounted = true;
    const fetchDefinitions = async () => {
      setLoading(true);
      try {
        const data = await customFieldsApi.listDefinitions(targetEntity, true);
        if (isMounted) setDefinitions(data);
      } catch (err) {
        console.error('Failed to load definitions for display card', err);
      } finally {
        if (isMounted) setLoading(false);
      }
    };
    fetchDefinitions();
    return () => {
      isMounted = false;
    };
  }, [targetEntity]);

  if (loading || definitions.length === 0) return null;

  const filledFields = definitions.filter((def) => {
    const val = values[def.fieldKey];
    return val !== undefined && val !== null && val !== '';
  });

  if (filledFields.length === 0) return null;

  const renderValue = (def: CustomFieldDefinition, val: any) => {
    if (def.fieldType === 'BOOLEAN') {
      return (
        <span
          className={`inline-block px-2 py-0.5 rounded text-xs font-medium ${
            val
              ? 'bg-emerald-50 dark:bg-emerald-950/50 text-emerald-700 dark:text-emerald-300 border border-emerald-200 dark:border-emerald-800/60'
              : 'bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-400'
          }`}
        >
          {val ? t('common', 'yes') : t('common', 'no')}
        </span>
      );
    }
    return <span className="text-slate-900 dark:text-slate-100 font-medium text-sm">{String(val)}</span>;
  };

  return (
    <div className={`bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl p-4 space-y-3 shadow-sm ${className}`}>
      <h4 className="text-xs font-semibold uppercase tracking-wider text-slate-500 dark:text-slate-400">
        {t('customFields', 'customFieldsSection')}
      </h4>
      <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-3">
        {filledFields.map((def) => (
          <div key={def.id} className="space-y-0.5">
            <span className="text-[11px] text-slate-500 dark:text-slate-400 block truncate">{def.label}</span>
            <div>{renderValue(def, values[def.fieldKey])}</div>
          </div>
        ))}
      </div>
    </div>
  );
};
