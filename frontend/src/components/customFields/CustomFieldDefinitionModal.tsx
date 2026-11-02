import React, { useState, useEffect } from 'react';
import { X, Plus, Trash2, Shield, Lock, AlertCircle } from 'lucide-react';
import {
  CustomFieldDefinition,
  CreateCustomFieldDefinitionRequest,
  UpdateCustomFieldDefinitionRequest,
  FieldType,
} from '../../types/customFields';
import { useI18n } from '../../context/I18nContext';

interface CustomFieldDefinitionModalProps {
  isOpen: boolean;
  onClose: () => void;
  targetEntity: string;
  fieldToEdit?: CustomFieldDefinition | null;
  onSave: (
    req: CreateCustomFieldDefinitionRequest | UpdateCustomFieldDefinitionRequest
  ) => Promise<void>;
}

export const CustomFieldDefinitionModal: React.FC<CustomFieldDefinitionModalProps> = ({
  isOpen,
  onClose,
  targetEntity,
  fieldToEdit,
  onSave,
}) => {
  const { t } = useI18n();

  const [fieldKey, setFieldKey] = useState('');
  const [label, setLabel] = useState('');
  const [description, setDescription] = useState('');
  const [fieldType, setFieldType] = useState<FieldType>('TEXT');
  const [required, setRequired] = useState(false);
  const [adminOnly, setAdminOnly] = useState(false);
  const [readOnly, setReadOnly] = useState(false);
  const [defaultValue, setDefaultValue] = useState('');
  const [options, setOptions] = useState<string[]>([]);
  const [newOption, setNewOption] = useState('');
  const [minVal, setMinVal] = useState<number | ''>('');
  const [maxVal, setMaxVal] = useState<number | ''>('');
  const [regex, setRegex] = useState('');
  const [displayOrder, setDisplayOrder] = useState<number>(0);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (fieldToEdit) {
      setFieldKey(fieldToEdit.fieldKey);
      setLabel(fieldToEdit.label);
      setDescription(fieldToEdit.description || '');
      setFieldType(fieldToEdit.fieldType);
      setRequired(fieldToEdit.required);
      setAdminOnly(fieldToEdit.adminOnly);
      setReadOnly(fieldToEdit.readOnly);
      setDefaultValue(fieldToEdit.defaultValue || '');
      setOptions(fieldToEdit.options || []);
      setMinVal(fieldToEdit.validationRules?.min ?? '');
      setMaxVal(fieldToEdit.validationRules?.max ?? '');
      setRegex(fieldToEdit.validationRules?.regex || '');
      setDisplayOrder(fieldToEdit.displayOrder);
    } else {
      setFieldKey('');
      setLabel('');
      setDescription('');
      setFieldType('TEXT');
      setRequired(false);
      setAdminOnly(false);
      setReadOnly(false);
      setDefaultValue('');
      setOptions([]);
      setMinVal('');
      setMaxVal('');
      setRegex('');
      setDisplayOrder(0);
    }
    setError(null);
  }, [fieldToEdit, isOpen]);

  if (!isOpen) return null;

  const handleAddOption = () => {
    const trimmed = newOption.trim();
    if (!trimmed) return;
    if (options.includes(trimmed)) return;
    setOptions([...options, trimmed]);
    setNewOption('');
  };

  const handleRemoveOption = (optToRemove: string) => {
    setOptions(options.filter((opt) => opt !== optToRemove));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    if (!label.trim()) {
      setError(t('common', 'fillRequired') || 'Label is required');
      return;
    }

    if (!fieldToEdit && !fieldKey.trim()) {
      setError('Field key is required');
      return;
    }

    if (fieldType === 'SELECT' && options.length === 0) {
      setError(t('customFields', 'optionsHelp') || 'Select type requires at least one option');
      return;
    }

    const validationRules =
      minVal !== '' || maxVal !== '' || regex.trim() !== ''
        ? {
            min: minVal !== '' ? Number(minVal) : undefined,
            max: maxVal !== '' ? Number(maxVal) : undefined,
            regex: regex.trim() || undefined,
          }
        : undefined;

    setLoading(true);
    try {
      if (fieldToEdit) {
        const updatePayload: UpdateCustomFieldDefinitionRequest = {
          label: label.trim(),
          description: description.trim() || undefined,
          required,
          adminOnly,
          readOnly,
          defaultValue: defaultValue.trim() || undefined,
          options: fieldType === 'SELECT' ? options : undefined,
          validationRules,
          displayOrder,
        };
        await onSave(updatePayload);
      } else {
        const createPayload: CreateCustomFieldDefinitionRequest = {
          targetEntity,
          fieldKey: fieldKey.trim().toLowerCase(),
          label: label.trim(),
          description: description.trim() || undefined,
          fieldType,
          required,
          adminOnly,
          readOnly,
          defaultValue: defaultValue.trim() || undefined,
          options: fieldType === 'SELECT' ? options : undefined,
          validationRules,
          displayOrder,
        };
        await onSave(createPayload);
      }
      onClose();
    } catch (err: any) {
      setError(err?.response?.data?.message || err?.message || 'Error saving custom field');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm p-4 overflow-y-auto">
      <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl max-w-2xl w-full shadow-2xl overflow-hidden my-8">
        <div className="flex items-center justify-between px-6 py-4 border-b border-slate-100 dark:border-slate-800 bg-slate-50/75 dark:bg-slate-950/50">
          <div>
            <h2 className="text-lg font-semibold text-slate-900 dark:text-white">
              {fieldToEdit ? t('customFields', 'editField') : t('customFields', 'newField')}
            </h2>
            <p className="text-xs text-slate-500 dark:text-slate-400 mt-0.5">
              {t('customFields', 'targetEntity')}: <span className="font-semibold text-primary">{targetEntity}</span>
            </p>
          </div>
          <button
            onClick={onClose}
            className="text-slate-400 hover:text-slate-700 dark:hover:text-slate-200 p-1 rounded-lg hover:bg-slate-100 dark:hover:bg-slate-800 transition"
          >
            <X size={20} />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="p-6 space-y-5">
          {error && (
            <div className="flex items-center gap-2 p-3 bg-red-50 dark:bg-red-950/50 border border-red-200 dark:border-red-800 rounded-lg text-red-700 dark:text-red-200 text-sm">
              <AlertCircle size={16} className="text-red-500 flex-shrink-0" />
              <span>{error}</span>
            </div>
          )}

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-medium text-slate-700 dark:text-slate-300 mb-1">
                {t('customFields', 'fieldKey')} *
              </label>
              <input
                type="text"
                disabled={!!fieldToEdit}
                value={fieldKey}
                onChange={(e) => setFieldKey(e.target.value.toLowerCase().replace(/[^a-z0-9_]/g, ''))}
                placeholder="e.g. spouse_name"
                className="w-full bg-white dark:bg-slate-950 border border-slate-300 dark:border-slate-800 rounded-lg px-3 py-2 text-sm text-slate-900 dark:text-slate-100 placeholder:text-slate-400 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary disabled:opacity-60 disabled:bg-slate-100 dark:disabled:bg-slate-900 disabled:cursor-not-allowed"
                required
              />
              <span className="text-[11px] text-slate-500 dark:text-slate-400 mt-0.5 block">{t('customFields', 'fieldKeyHelp')}</span>
            </div>

            <div>
              <label className="block text-xs font-medium text-slate-700 dark:text-slate-300 mb-1">
                {t('customFields', 'label')} *
              </label>
              <input
                type="text"
                value={label}
                onChange={(e) => setLabel(e.target.value)}
                placeholder={t('customFields', 'labelPlaceholder')}
                className="w-full bg-white dark:bg-slate-950 border border-slate-300 dark:border-slate-800 rounded-lg px-3 py-2 text-sm text-slate-900 dark:text-slate-100 placeholder:text-slate-400 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary"
                required
              />
            </div>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-medium text-slate-700 dark:text-slate-300 mb-1">
                {t('customFields', 'fieldType')}
              </label>
              <select
                disabled={!!fieldToEdit}
                value={fieldType}
                onChange={(e) => setFieldType(e.target.value as FieldType)}
                className="w-full bg-white dark:bg-slate-950 border border-slate-300 dark:border-slate-800 rounded-lg px-3 py-2 text-sm text-slate-900 dark:text-slate-100 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary disabled:opacity-60 disabled:bg-slate-100 dark:disabled:bg-slate-900"
              >
                <option value="TEXT">{t('customFields.types', 'TEXT') || 'Text'}</option>
                <option value="NUMBER">{t('customFields.types', 'NUMBER') || 'Integer Number'}</option>
                <option value="DECIMAL">{t('customFields.types', 'DECIMAL') || 'Decimal'}</option>
                <option value="DATE">{t('customFields.types', 'DATE') || 'Date'}</option>
                <option value="BOOLEAN">{t('customFields.types', 'BOOLEAN') || 'Boolean'}</option>
                <option value="SELECT">{t('customFields.types', 'SELECT') || 'Dropdown'}</option>
              </select>
            </div>

            <div>
              <label className="block text-xs font-medium text-slate-700 dark:text-slate-300 mb-1">
                {t('customFields', 'displayOrder')}
              </label>
              <input
                type="number"
                value={displayOrder}
                onChange={(e) => setDisplayOrder(parseInt(e.target.value, 10) || 0)}
                className="w-full bg-white dark:bg-slate-950 border border-slate-300 dark:border-slate-800 rounded-lg px-3 py-2 text-sm text-slate-900 dark:text-slate-100 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary"
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-medium text-slate-700 dark:text-slate-300 mb-1">
              {t('customFields', 'fieldDescription')}
            </label>
            <input
              type="text"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder={t('customFields', 'fieldDescriptionPlaceholder')}
              className="w-full bg-white dark:bg-slate-950 border border-slate-300 dark:border-slate-800 rounded-lg px-3 py-2 text-sm text-slate-900 dark:text-slate-100 placeholder:text-slate-400 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary"
            />
          </div>

          {fieldType === 'SELECT' && (
            <div className="bg-slate-50 dark:bg-slate-950/60 border border-slate-200 dark:border-slate-800 rounded-lg p-3.5 space-y-2">
              <label className="block text-xs font-medium text-primary">
                {t('customFields', 'options')} *
              </label>
              <div className="flex gap-2">
                <input
                  type="text"
                  value={newOption}
                  onChange={(e) => setNewOption(e.target.value)}
                  onKeyDown={(e) => {
                    if (e.key === 'Enter') {
                      e.preventDefault();
                      handleAddOption();
                    }
                  }}
                  placeholder="e.g. PLATINUM"
                  className="flex-1 bg-white dark:bg-slate-900 border border-slate-300 dark:border-slate-700 rounded-lg px-3 py-1.5 text-xs text-slate-900 dark:text-slate-100 placeholder:text-slate-400 focus:outline-none focus:border-primary"
                />
                <button
                  type="button"
                  onClick={handleAddOption}
                  className="px-3 py-1.5 bg-primary hover:bg-primary/90 text-primary-foreground rounded-lg text-xs font-medium flex items-center gap-1 transition shadow-sm"
                >
                  <Plus size={14} />
                  {t('customFields', 'addOption')}
                </button>
              </div>

              <div className="flex flex-wrap gap-1.5 mt-2 max-h-32 overflow-y-auto">
                {options.map((opt) => (
                  <span
                    key={opt}
                    className="inline-flex items-center gap-1 px-2.5 py-1 bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 text-slate-800 dark:text-slate-200 rounded-full text-xs shadow-sm"
                  >
                    {opt}
                    <button
                      type="button"
                      onClick={() => handleRemoveOption(opt)}
                      className="text-slate-400 hover:text-red-500 ml-1"
                    >
                      <Trash2 size={12} />
                    </button>
                  </span>
                ))}
              </div>
            </div>
          )}

          <div className="grid grid-cols-1 md:grid-cols-3 gap-3 pt-2">
            <label className="flex items-center gap-2 p-2.5 bg-slate-50 dark:bg-slate-950/40 border border-slate-200 dark:border-slate-800/80 rounded-lg cursor-pointer hover:border-slate-300 dark:hover:border-slate-700 transition">
              <input
                type="checkbox"
                checked={required}
                onChange={(e) => setRequired(e.target.checked)}
                className="rounded border-slate-300 dark:border-slate-700 text-primary focus:ring-0"
              />
              <span className="text-xs text-slate-800 dark:text-slate-200 font-medium">{t('customFields', 'required')}</span>
            </label>

            <label className="flex items-center gap-2 p-2.5 bg-slate-50 dark:bg-slate-950/40 border border-slate-200 dark:border-slate-800/80 rounded-lg cursor-pointer hover:border-slate-300 dark:hover:border-slate-700 transition">
              <input
                type="checkbox"
                checked={adminOnly}
                onChange={(e) => setAdminOnly(e.target.checked)}
                className="rounded border-slate-300 dark:border-slate-700 text-amber-500 focus:ring-0"
              />
              <Shield size={14} className="text-amber-500" />
              <span className="text-xs text-slate-800 dark:text-slate-200 font-medium">{t('customFields', 'adminOnly')}</span>
            </label>

            <label className="flex items-center gap-2 p-2.5 bg-slate-50 dark:bg-slate-950/40 border border-slate-200 dark:border-slate-800/80 rounded-lg cursor-pointer hover:border-slate-300 dark:hover:border-slate-700 transition">
              <input
                type="checkbox"
                checked={readOnly}
                onChange={(e) => setReadOnly(e.target.checked)}
                className="rounded border-slate-300 dark:border-slate-700 text-sky-500 focus:ring-0"
              />
              <Lock size={14} className="text-sky-500" />
              <span className="text-xs text-slate-800 dark:text-slate-200 font-medium">{t('customFields', 'readOnly')}</span>
            </label>
          </div>

          <div className="flex items-center justify-end gap-3 pt-4 border-t border-slate-100 dark:border-slate-800">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 bg-slate-100 dark:bg-slate-800 hover:bg-slate-200 dark:hover:bg-slate-700 text-slate-700 dark:text-slate-300 rounded-lg text-sm font-medium transition"
            >
              {t('common', 'cancel')}
            </button>
            <button
              type="submit"
              disabled={loading}
              className="px-4 py-2 bg-primary hover:bg-primary/90 disabled:opacity-50 text-primary-foreground rounded-lg text-sm font-medium transition shadow-sm"
            >
              {loading ? t('common', 'loading') : t('common', 'save')}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
