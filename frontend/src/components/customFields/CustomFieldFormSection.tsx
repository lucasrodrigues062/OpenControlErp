import React, { useState, useEffect } from 'react';
import { Shield, Lock } from 'lucide-react';
import { CustomFieldDefinition } from '../../types/customFields';
import { customFieldsApi } from '../../api/customFieldsApi';
import { useI18n } from '../../context/I18nContext';

interface CustomFieldFormSectionProps {
  targetEntity: string;
  values: Record<string, any>;
  onChange: (key: string, value: any) => void;
  errors?: Record<string, string>;
  disabled?: boolean;
  isNewRecord?: boolean;
}

export const CustomFieldFormSection: React.FC<CustomFieldFormSectionProps> = ({
  targetEntity,
  values,
  onChange,
  errors = {},
  disabled = false,
  isNewRecord = true,
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
        if (isMounted) {
          setDefinitions(data);
        }
      } catch (err) {
        console.error('Failed to load custom fields for', targetEntity, err);
      } finally {
        if (isMounted) setLoading(false);
      }
    };

    fetchDefinitions();
    return () => {
      isMounted = false;
    };
  }, [targetEntity]);

  if (loading) {
    return <div className="p-4 text-xs text-slate-500">{t('common', 'loading')}</div>;
  }

  if (definitions.length === 0) {
    return null;
  }

  return (
    <div className="space-y-4 pt-4 border-t border-slate-200 dark:border-slate-800">
      <div className="flex items-center justify-between">
        <h3 className="text-sm font-semibold text-slate-900 dark:text-slate-100">
          {t('customFields', 'customFieldsSection')}
        </h3>
        <span className="text-[11px] text-slate-500 dark:text-slate-400">
          {definitions.length} {definitions.length === 1 ? 'campo' : 'campos'}
        </span>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {definitions.map((def) => {
          const value = values[def.fieldKey] ?? def.defaultValue ?? '';
          const isFieldDisabled = disabled || (!isNewRecord && def.readOnly);
          const fieldError = errors[def.fieldKey];

          return (
            <div key={def.id} className="space-y-1">
              <div className="flex items-center justify-between">
                <label className="block text-xs font-medium text-slate-700 dark:text-slate-300">
                  {def.label} {def.required && <span className="text-red-500">*</span>}
                </label>
                <div className="flex items-center gap-1">
                  {def.adminOnly && (
                    <span
                      title={t('customFields', 'adminOnlyHelp')}
                      className="inline-flex items-center gap-0.5 text-[10px] text-amber-700 dark:text-amber-400 bg-amber-50 dark:bg-amber-950/40 px-1 py-0.5 rounded border border-amber-200 dark:border-amber-800/50"
                    >
                      <Shield size={10} />
                      Admin
                    </span>
                  )}
                  {def.readOnly && !isNewRecord && (
                    <span
                      title={t('customFields', 'readOnlyHelp')}
                      className="inline-flex items-center gap-0.5 text-[10px] text-sky-700 dark:text-sky-400 bg-sky-50 dark:bg-sky-950/40 px-1 py-0.5 rounded border border-sky-200 dark:border-sky-800/50"
                    >
                      <Lock size={10} />
                      Read-Only
                    </span>
                  )}
                </div>
              </div>

              {def.description && (
                <p className="text-[11px] text-slate-500 dark:text-slate-400">{def.description}</p>
              )}

              {/* Input Render based on Type */}
              {def.fieldType === 'TEXT' && (
                <input
                  type="text"
                  disabled={isFieldDisabled}
                  value={value}
                  onChange={(e) => onChange(def.fieldKey, e.target.value)}
                  className="w-full bg-white dark:bg-slate-950 border border-slate-300 dark:border-slate-800 rounded-lg px-3 py-2 text-sm text-slate-900 dark:text-slate-100 placeholder:text-slate-400 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary disabled:opacity-50 disabled:bg-slate-100 dark:disabled:bg-slate-900 disabled:cursor-not-allowed"
                />
              )}

              {def.fieldType === 'NUMBER' && (
                <input
                  type="number"
                  step="1"
                  disabled={isFieldDisabled}
                  value={value}
                  onChange={(e) => onChange(def.fieldKey, e.target.value === '' ? '' : parseInt(e.target.value, 10))}
                  className="w-full bg-white dark:bg-slate-950 border border-slate-300 dark:border-slate-800 rounded-lg px-3 py-2 text-sm text-slate-900 dark:text-slate-100 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary disabled:opacity-50 disabled:bg-slate-100 dark:disabled:bg-slate-900 disabled:cursor-not-allowed"
                />
              )}

              {def.fieldType === 'DECIMAL' && (
                <input
                  type="number"
                  step="0.01"
                  disabled={isFieldDisabled}
                  value={value}
                  onChange={(e) => onChange(def.fieldKey, e.target.value === '' ? '' : parseFloat(e.target.value))}
                  className="w-full bg-white dark:bg-slate-950 border border-slate-300 dark:border-slate-800 rounded-lg px-3 py-2 text-sm text-slate-900 dark:text-slate-100 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary disabled:opacity-50 disabled:bg-slate-100 dark:disabled:bg-slate-900 disabled:cursor-not-allowed"
                />
              )}

              {def.fieldType === 'DATE' && (
                <input
                  type="date"
                  disabled={isFieldDisabled}
                  value={value}
                  onChange={(e) => onChange(def.fieldKey, e.target.value)}
                  className="w-full bg-white dark:bg-slate-950 border border-slate-300 dark:border-slate-800 rounded-lg px-3 py-2 text-sm text-slate-900 dark:text-slate-100 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary disabled:opacity-50 disabled:bg-slate-100 dark:disabled:bg-slate-900 disabled:cursor-not-allowed"
                />
              )}

              {def.fieldType === 'BOOLEAN' && (
                <label className="flex items-center gap-2 pt-1 cursor-pointer">
                  <input
                    type="checkbox"
                    disabled={isFieldDisabled}
                    checked={Boolean(value)}
                    onChange={(e) => onChange(def.fieldKey, e.target.checked)}
                    className="rounded border-slate-300 dark:border-slate-700 text-primary focus:ring-0 disabled:opacity-50"
                  />
                  <span className="text-xs text-slate-700 dark:text-slate-300">
                    {Boolean(value) ? t('common', 'yes') : t('common', 'no')}
                  </span>
                </label>
              )}

              {def.fieldType === 'SELECT' && (
                <select
                  disabled={isFieldDisabled}
                  value={value}
                  onChange={(e) => onChange(def.fieldKey, e.target.value)}
                  className="w-full bg-white dark:bg-slate-950 border border-slate-300 dark:border-slate-800 rounded-lg px-3 py-2 text-sm text-slate-900 dark:text-slate-100 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary disabled:opacity-50 disabled:bg-slate-100 dark:disabled:bg-slate-900 disabled:cursor-not-allowed"
                >
                  <option value="">-- {t('common', 'filter')} --</option>
                  {(def.options || []).map((opt) => (
                    <option key={opt} value={opt}>
                      {opt}
                    </option>
                  ))}
                </select>
              )}

              {fieldError && (
                <p className="text-[11px] text-red-500 dark:text-red-400 mt-0.5">{fieldError}</p>
              )}
            </div>
          );
        })}
      </div>
    </div>
  );
};
