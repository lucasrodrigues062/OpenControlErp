import React, { useState, useEffect } from 'react';
import {
  Plus,
  Shield,
  Lock,
  ArrowUp,
  ArrowDown,
  Edit2,
  CheckCircle2,
  XCircle,
  Database,
  Sliders,
} from 'lucide-react';
import {
  CustomFieldDefinition,
  TargetEntityDescriptor,
  CreateCustomFieldDefinitionRequest,
  UpdateCustomFieldDefinitionRequest,
  FieldOrderDto,
} from '../types/customFields';
import { customFieldsApi } from '../api/customFieldsApi';
import { CustomFieldDefinitionModal } from '../components/customFields/CustomFieldDefinitionModal';
import { useI18n } from '../context/I18nContext';

export const CustomFieldsSettingsPage: React.FC = () => {
  const { t } = useI18n();

  const [entities, setEntities] = useState<TargetEntityDescriptor[]>([]);
  const [selectedEntity, setSelectedEntity] = useState<string>('CUSTOMER');
  const [definitions, setDefinitions] = useState<CustomFieldDefinition[]>([]);
  const [loading, setLoading] = useState(false);
  const [modalOpen, setModalOpen] = useState(false);
  const [editingField, setEditingField] = useState<CustomFieldDefinition | null>(null);
  const [notification, setNotification] = useState<{ type: 'success' | 'error'; message: string } | null>(null);

  useEffect(() => {
    loadEntities();
  }, []);

  useEffect(() => {
    if (selectedEntity) {
      loadDefinitions(selectedEntity);
    }
  }, [selectedEntity]);

  const showNotification = (type: 'success' | 'error', message: string) => {
    setNotification({ type, message });
    setTimeout(() => setNotification(null), 4000);
  };

  const loadEntities = async () => {
    try {
      const data = await customFieldsApi.listEntities();
      setEntities(data);
      if (data.length > 0 && !selectedEntity) {
        setSelectedEntity(data[0].entityCode);
      }
    } catch (err: any) {
      showNotification('error', err.message || 'Failed to load entities');
    }
  };

  const loadDefinitions = async (entity: string) => {
    setLoading(true);
    try {
      const data = await customFieldsApi.listDefinitions(entity, false);
      setDefinitions(data);
    } catch (err: any) {
      showNotification('error', err.message || 'Failed to load custom fields');
    } finally {
      setLoading(false);
    }
  };

  const handleOpenCreate = () => {
    setEditingField(null);
    setModalOpen(true);
  };

  const handleOpenEdit = (field: CustomFieldDefinition) => {
    setEditingField(field);
    setModalOpen(true);
  };

  const handleSaveField = async (
    req: CreateCustomFieldDefinitionRequest | UpdateCustomFieldDefinitionRequest
  ) => {
    const target = (req as CreateCustomFieldDefinitionRequest).targetEntity || selectedEntity;
    if (target !== selectedEntity) {
      setSelectedEntity(target);
    }
    if (editingField) {
      await customFieldsApi.updateDefinition(editingField.id, req as UpdateCustomFieldDefinitionRequest);
      showNotification('success', t('customFields', 'saveSuccess'));
    } else {
      await customFieldsApi.createDefinition(req as CreateCustomFieldDefinitionRequest);
      showNotification('success', t('customFields', 'saveSuccess'));
    }
    await loadDefinitions(selectedEntity);
  };

  const handleToggleStatus = async (field: CustomFieldDefinition) => {
    try {
      await customFieldsApi.toggleStatus(field.id);
      showNotification('success', t('customFields', 'toggleSuccess'));
      await loadDefinitions(selectedEntity);
    } catch (err: any) {
      showNotification('error', err.message || 'Error toggling status');
    }
  };

  const handleMoveOrder = async (index: number, direction: 'UP' | 'DOWN') => {
    if (
      (direction === 'UP' && index === 0) ||
      (direction === 'DOWN' && index === definitions.length - 1)
    ) {
      return;
    }

    const targetIndex = direction === 'UP' ? index - 1 : index + 1;
    const newItems = [...definitions];
    const temp = newItems[index];
    newItems[index] = newItems[targetIndex];
    newItems[targetIndex] = temp;

    const orders: FieldOrderDto[] = newItems.map((item, idx) => ({
      id: item.id,
      displayOrder: (idx + 1) * 10,
    }));

    try {
      await customFieldsApi.reorderDefinitions(orders);
      setDefinitions(newItems);
      showNotification('success', t('customFields', 'reorderSuccess'));
      await loadDefinitions(selectedEntity);
    } catch (err: any) {
      showNotification('error', err.message || 'Error reordering fields');
    }
  };

  const getTypeBadgeColor = (type: string) => {
    switch (type) {
      case 'TEXT':
        return 'bg-blue-50 text-blue-700 border-blue-200 dark:bg-blue-950/40 dark:text-blue-300 dark:border-blue-700/50';
      case 'NUMBER':
      case 'DECIMAL':
        return 'bg-emerald-50 text-emerald-700 border-emerald-200 dark:bg-emerald-950/40 dark:text-emerald-300 dark:border-emerald-700/50';
      case 'DATE':
        return 'bg-amber-50 text-amber-700 border-amber-200 dark:bg-amber-950/40 dark:text-amber-300 dark:border-amber-700/50';
      case 'BOOLEAN':
        return 'bg-purple-50 text-purple-700 border-purple-200 dark:bg-purple-950/40 dark:text-purple-300 dark:border-purple-700/50';
      case 'SELECT':
        return 'bg-indigo-50 text-indigo-700 border-indigo-200 dark:bg-indigo-950/40 dark:text-indigo-300 dark:border-indigo-700/50';
      default:
        return 'bg-slate-100 text-slate-700 border-slate-200 dark:bg-slate-800 dark:text-slate-300 dark:border-slate-700';
    }
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <div className="flex items-center gap-2">
            <Sliders className="text-primary" size={24} />
            <h1 className="text-2xl font-bold tracking-tight text-slate-900 dark:text-white">
              {t('customFields', 'title')}
            </h1>
          </div>
          <p className="text-sm text-slate-500 dark:text-slate-400 mt-1">
            {t('customFields', 'description')}
          </p>
        </div>

        <button
          onClick={handleOpenCreate}
          className="inline-flex items-center gap-2 px-4 py-2 bg-primary hover:bg-primary/90 text-primary-foreground rounded-lg text-sm font-medium transition shadow-sm"
        >
          <Plus size={16} />
          {t('customFields', 'newField')}
        </button>
      </div>

      {/* Notification banner */}
      {notification && (
        <div
          className={`p-3 rounded-lg text-sm flex items-center gap-2 border ${
            notification.type === 'success'
              ? 'bg-emerald-50 dark:bg-emerald-950/60 border-emerald-200 dark:border-emerald-800 text-emerald-800 dark:text-emerald-200'
              : 'bg-red-50 dark:bg-red-950/60 border-red-200 dark:border-red-800 text-red-800 dark:text-red-200'
          }`}
        >
          {notification.type === 'success' ? (
            <CheckCircle2 size={16} className="text-emerald-500" />
          ) : (
            <XCircle size={16} className="text-red-500" />
          )}
          <span>{notification.message}</span>
        </div>
      )}

      {/* Entity Tabs */}
      <div className="border-b border-slate-200 dark:border-slate-800 flex gap-2 overflow-x-auto pb-1">
        {entities.map((entity) => (
          <button
            key={entity.entityCode}
            onClick={() => setSelectedEntity(entity.entityCode)}
            className={`px-4 py-2 text-sm font-medium rounded-t-lg transition flex items-center gap-2 border-b-2 whitespace-nowrap ${
              selectedEntity === entity.entityCode
                ? 'border-primary text-primary bg-primary/5 dark:bg-primary/10 font-semibold'
                : 'border-transparent text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white hover:bg-slate-100 dark:hover:bg-slate-800/40'
            }`}
          >
            <Database size={14} className="text-primary" />
            <span>{entity.displayName}</span>
            <span className="text-[10px] uppercase font-mono px-1.5 py-0.5 rounded bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-400">
              {entity.entityCode}
            </span>
          </button>
        ))}
      </div>

      {/* Fields List Card */}
      <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl overflow-hidden shadow-sm">
        {loading ? (
          <div className="p-12 text-center text-slate-500 dark:text-slate-400">
            {t('common', 'loading')}
          </div>
        ) : definitions.length === 0 ? (
          <div className="p-12 text-center text-slate-500 dark:text-slate-400 space-y-3">
            <Sliders size={36} className="mx-auto text-slate-400 dark:text-slate-600" />
            <p className="text-sm">{t('customFields', 'noFields')}</p>
            <button
              onClick={handleOpenCreate}
              className="inline-flex items-center gap-1.5 px-3.5 py-1.5 bg-primary hover:bg-primary/90 text-primary-foreground rounded-lg text-xs font-medium transition shadow-sm"
            >
              <Plus size={14} />
              {t('customFields', 'newField')}
            </button>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="border-b border-slate-200 dark:border-slate-800 bg-slate-50/75 dark:bg-slate-950/60 text-xs font-semibold text-slate-500 dark:text-slate-400 uppercase tracking-wider">
                  <th className="py-3 px-4 w-12 text-center">#</th>
                  <th className="py-3 px-4">{t('customFields', 'label')}</th>
                  <th className="py-3 px-4">{t('customFields', 'fieldKey')}</th>
                  <th className="py-3 px-4">{t('customFields', 'fieldType')}</th>
                  <th className="py-3 px-4">Flags</th>
                  <th className="py-3 px-4">{t('common', 'status')}</th>
                  <th className="py-3 px-4 text-right">{t('common', 'actions')}</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 dark:divide-slate-800 text-sm">
                {definitions.map((def, idx) => (
                  <tr
                    key={def.id}
                    className={`hover:bg-slate-50/80 dark:hover:bg-slate-800/40 transition ${
                      !def.isActive ? 'opacity-60 bg-slate-50/40 dark:bg-slate-950/20' : ''
                    }`}
                  >
                    <td className="py-3 px-4 text-center text-xs font-mono text-slate-400">
                      {def.displayOrder}
                    </td>

                    <td className="py-3 px-4">
                      <div className="font-medium text-slate-900 dark:text-white">{def.label}</div>
                      {def.description && (
                        <div className="text-xs text-slate-500 dark:text-slate-400 truncate max-w-xs">
                          {def.description}
                        </div>
                      )}
                    </td>

                    <td className="py-3 px-4">
                      <code className="text-xs font-mono text-indigo-700 dark:text-indigo-300 bg-indigo-50 dark:bg-indigo-950/40 px-1.5 py-0.5 rounded border border-indigo-200 dark:border-indigo-800/30">
                        {def.fieldKey}
                      </code>
                    </td>

                    <td className="py-3 px-4">
                      <span
                        className={`inline-block px-2.5 py-0.5 rounded-full text-xs font-medium border ${getTypeBadgeColor(
                          def.fieldType
                        )}`}
                      >
                        {t('customFields.types', def.fieldType) || def.fieldType}
                        {def.fieldType === 'SELECT' && def.options && (
                          <span className="ml-1 text-[10px] opacity-75">
                            ({def.options.length})
                          </span>
                        )}
                      </span>
                    </td>

                    <td className="py-3 px-4">
                      <div className="flex items-center gap-1.5 flex-wrap">
                        {def.required && (
                          <span className="text-[10px] font-semibold px-1.5 py-0.5 bg-red-50 dark:bg-red-950/60 border border-red-200 dark:border-red-800 text-red-700 dark:text-red-300 rounded">
                            REQ
                          </span>
                        )}
                        {def.adminOnly && (
                          <span className="inline-flex items-center gap-1 text-[10px] font-semibold px-1.5 py-0.5 bg-amber-50 dark:bg-amber-950/60 border border-amber-200 dark:border-amber-800 text-amber-700 dark:text-amber-300 rounded">
                            <Shield size={10} />
                            ADMIN
                          </span>
                        )}
                        {def.readOnly && (
                          <span className="inline-flex items-center gap-1 text-[10px] font-semibold px-1.5 py-0.5 bg-sky-50 dark:bg-sky-950/60 border border-sky-200 dark:border-sky-800 text-sky-700 dark:text-sky-300 rounded">
                            <Lock size={10} />
                            RO
                          </span>
                        )}
                      </div>
                    </td>

                    <td className="py-3 px-4">
                      <button
                        onClick={() => handleToggleStatus(def)}
                        title={t('customFields', 'toggleActive')}
                        className={`inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-medium border transition ${
                          def.isActive
                            ? 'bg-emerald-50 dark:bg-emerald-950/50 border-emerald-200 dark:border-emerald-700/60 text-emerald-700 dark:text-emerald-300 hover:bg-emerald-100 dark:hover:bg-emerald-900/60'
                            : 'bg-slate-100 dark:bg-slate-800 border-slate-200 dark:border-slate-700 text-slate-600 dark:text-slate-400 hover:bg-slate-200 dark:hover:bg-slate-700'
                        }`}
                      >
                        <span
                          className={`w-1.5 h-1.5 rounded-full ${
                            def.isActive ? 'bg-emerald-500' : 'bg-slate-400'
                          }`}
                        />
                        {def.isActive ? t('common', 'active') : t('common', 'inactive')}
                      </button>
                    </td>

                    <td className="py-3 px-4 text-right">
                      <div className="inline-flex items-center gap-1">
                        <button
                          onClick={() => handleMoveOrder(idx, 'UP')}
                          disabled={idx === 0}
                          title={t('customFields', 'moveUp')}
                          className="p-1.5 text-slate-400 hover:text-slate-700 dark:hover:text-white disabled:opacity-30 disabled:cursor-not-allowed rounded hover:bg-slate-100 dark:hover:bg-slate-800 transition"
                        >
                          <ArrowUp size={14} />
                        </button>
                        <button
                          onClick={() => handleMoveOrder(idx, 'DOWN')}
                          disabled={idx === definitions.length - 1}
                          title={t('customFields', 'moveDown')}
                          className="p-1.5 text-slate-400 hover:text-slate-700 dark:hover:text-white disabled:opacity-30 disabled:cursor-not-allowed rounded hover:bg-slate-100 dark:hover:bg-slate-800 transition"
                        >
                          <ArrowDown size={14} />
                        </button>
                        <button
                          onClick={() => handleOpenEdit(def)}
                          title={t('common', 'edit')}
                          className="p-1.5 text-slate-400 hover:text-primary rounded hover:bg-slate-100 dark:hover:bg-slate-800 transition ml-1"
                        >
                          <Edit2 size={14} />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Modal */}
      <CustomFieldDefinitionModal
        isOpen={modalOpen}
        onClose={() => setModalOpen(false)}
        targetEntity={selectedEntity}
        entities={entities}
        fieldToEdit={editingField}
        onSave={handleSaveField}
      />
    </div>
  );
};
