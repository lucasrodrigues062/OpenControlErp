import React, { useState, useMemo } from 'react';
import { X, Search, Hash, Type, HelpCircle, ListFilter, Calendar, Check, Sparkles } from 'lucide-react';
import { ENTITY_FIELDS_CATALOG, EntityField, FieldDataType } from '../../types/entityFields';
import { useI18n } from '../../context/I18nContext';

interface FieldSearchModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSelectField: (field: EntityField) => void;
  currentTrigger?: string;
}

export const FieldSearchModal: React.FC<FieldSearchModalProps> = ({
  isOpen,
  onClose,
  onSelectField,
  currentTrigger,
}) => {
  const { t } = useI18n();
  const [query, setQuery] = useState('');
  const [selectedEntity, setSelectedEntity] = useState<string>('ALL');

  const entities = useMemo(() => {
    const set = new Set<string>();
    ENTITY_FIELDS_CATALOG.forEach((f) => set.add(f.entity));
    return ['ALL', ...Array.from(set)];
  }, []);

  const filteredFields = useMemo(() => {
    const q = query.toLowerCase().trim();
    return ENTITY_FIELDS_CATALOG.filter((f) => {
      const matchesEntity = selectedEntity === 'ALL' || f.entity === selectedEntity;
      const matchesQuery =
        !q ||
        f.path.toLowerCase().includes(q) ||
        f.label.toLowerCase().includes(q) ||
        f.description.toLowerCase().includes(q) ||
        f.entity.toLowerCase().includes(q);
      return matchesEntity && matchesQuery;
    }).sort((a, b) => {
      // Prioritize fields matching current trigger domain
      const prefix = currentTrigger?.split('.')[0] || '';
      const aMatches = prefix && a.path.startsWith(prefix) ? 1 : 0;
      const bMatches = prefix && b.path.startsWith(prefix) ? 1 : 0;
      return bMatches - aMatches;
    });
  }, [query, selectedEntity, currentTrigger]);

  if (!isOpen) return null;

  const renderTypeIcon = (type: FieldDataType) => {
    switch (type) {
      case 'number':
        return <span className="inline-flex items-center gap-1 text-[11px] font-mono font-bold text-blue-600 dark:text-blue-400 bg-blue-50 dark:bg-blue-950/40 px-2 py-0.5 rounded border border-blue-200 dark:border-blue-900"><Hash className="w-3 h-3" /> Number</span>;
      case 'string':
        return <span className="inline-flex items-center gap-1 text-[11px] font-mono font-bold text-emerald-600 dark:text-emerald-400 bg-emerald-50 dark:bg-emerald-950/40 px-2 py-0.5 rounded border border-emerald-200 dark:border-emerald-900"><Type className="w-3 h-3" /> Text</span>;
      case 'boolean':
        return <span className="inline-flex items-center gap-1 text-[11px] font-mono font-bold text-purple-600 dark:text-purple-400 bg-purple-50 dark:bg-purple-950/40 px-2 py-0.5 rounded border border-purple-200 dark:border-purple-900"><HelpCircle className="w-3 h-3" /> Boolean</span>;
      case 'enum':
        return <span className="inline-flex items-center gap-1 text-[11px] font-mono font-bold text-amber-600 dark:text-amber-400 bg-amber-50 dark:bg-amber-950/40 px-2 py-0.5 rounded border border-amber-200 dark:border-amber-900"><ListFilter className="w-3 h-3" /> Enum List</span>;
      case 'date':
        return <span className="inline-flex items-center gap-1 text-[11px] font-mono font-bold text-indigo-600 dark:text-indigo-400 bg-indigo-50 dark:bg-indigo-950/40 px-2 py-0.5 rounded border border-indigo-200 dark:border-indigo-900"><Calendar className="w-3 h-3" /> Date</span>;
    }
  };

  return (
    <div className="fixed inset-0 z-60 flex items-center justify-center p-3 sm:p-6 bg-slate-950/70 backdrop-blur-md animate-fadeIn">
      <div className="w-full max-w-3xl bg-white dark:bg-slate-900 rounded-2xl shadow-2xl border border-slate-200 dark:border-slate-800 flex flex-col max-h-[85vh] overflow-hidden">
        {/* Header */}
        <div className="p-5 border-b border-slate-200 dark:border-slate-800 flex items-center justify-between">
          <div className="flex items-center space-x-3">
            <div className="p-2 bg-blue-50 dark:bg-blue-950/40 border border-blue-200 dark:border-blue-900 rounded-xl text-blue-600 dark:text-blue-400">
              <Sparkles className="w-5 h-5" />
            </div>
            <div>
              <h3 className="text-base font-bold text-slate-900 dark:text-white">
                {t('rules', 'fieldCatalog')}
              </h3>
              <p className="text-xs text-slate-500">
                {t('rules', 'fieldCatalogDesc')}
              </p>
            </div>
          </div>

          <button
            onClick={onClose}
            className="p-1.5 rounded-lg text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Search bar & Category filter tabs */}
        <div className="p-4 border-b border-slate-100 dark:border-slate-800/80 bg-slate-50/50 dark:bg-slate-900/50 space-y-3">
          <div className="relative">
            <Search className="w-4 h-4 absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400" />
            <input
              type="text"
              autoFocus
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder={t('rules', 'searchField')}
              className="w-full pl-10 pr-4 py-2 text-sm bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-xl text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500 shadow-sm"
            />
          </div>

          {/* Entity category pills */}
          <div className="flex items-center space-x-1.5 overflow-x-auto pb-1 text-xs no-scrollbar">
            {entities.map((ent) => (
              <button
                key={ent}
                type="button"
                onClick={() => setSelectedEntity(ent)}
                className={`px-3 py-1 rounded-lg font-medium whitespace-nowrap transition-colors ${
                  selectedEntity === ent
                    ? 'bg-blue-600 text-white shadow-xs'
                    : 'bg-white dark:bg-slate-800 text-slate-600 dark:text-slate-400 border border-slate-200 dark:border-slate-700 hover:bg-slate-100'
                }`}
              >
                {ent === 'ALL' ? t('rules', 'allEntities') : ent}
              </button>
            ))}
          </div>
        </div>

        {/* Fields list */}
        <div className="p-4 overflow-y-auto flex-1 divide-y divide-slate-100 dark:divide-slate-800/60">
          {filteredFields.length === 0 ? (
            <div className="text-center py-12 text-slate-400 text-sm">
              Nenhum campo encontrado para a busca "{query}".
            </div>
          ) : (
            filteredFields.map((f) => (
              <div
                key={f.path}
                onClick={() => {
                  onSelectField(f);
                  onClose();
                }}
                className="py-3 px-3 hover:bg-blue-50/60 dark:hover:bg-slate-800/60 rounded-xl cursor-pointer transition-colors group flex items-start justify-between gap-4"
              >
                <div className="space-y-1 flex-1">
                  <div className="flex items-center space-x-2">
                    <span className="font-mono text-xs font-bold text-blue-600 dark:text-blue-400 group-hover:underline">
                      #{f.path}
                    </span>
                    <span className="text-xs font-semibold text-slate-800 dark:text-slate-200">
                      • {f.label}
                    </span>
                    <span className="text-[10px] uppercase font-bold tracking-wider text-slate-400 bg-slate-100 dark:bg-slate-800 px-1.5 py-0.5 rounded">
                      {f.entity}
                    </span>
                  </div>

                  <p className="text-xs text-slate-500 line-clamp-2">
                    {f.description}
                  </p>

                  <div className="flex items-center space-x-3 text-[11px] text-slate-400 pt-0.5">
                    <span>
                      {t('rules', 'example')}: <code className="font-mono text-slate-600 dark:text-slate-300">{String(f.example)}</code>
                    </span>
                    {f.allowedValues && (
                      <span className="truncate max-w-xs">
                        Valores: <code className="font-mono text-slate-600 dark:text-slate-300">{f.allowedValues.join(', ')}</code>
                      </span>
                    )}
                  </div>
                </div>

                <div className="flex flex-col items-end space-y-2 shrink-0">
                  {renderTypeIcon(f.type)}
                  <span className="text-xs font-semibold text-blue-600 opacity-0 group-hover:opacity-100 transition-opacity inline-flex items-center gap-1">
                    <Check className="w-3.5 h-3.5" /> Selecionar
                  </span>
                </div>
              </div>
            ))
          )}
        </div>
      </div>
    </div>
  );
};
