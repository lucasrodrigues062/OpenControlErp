import React, { useState, useRef, useEffect } from 'react';
import { useBranch } from '../../context/BranchContext';
import { Building2, Check, ChevronDown, Store } from 'lucide-react';
import { useI18n } from '../../context/I18nContext';

export const BranchSwitcher: React.FC = () => {
  const { t } = useI18n();
  const { activeBranch, branches, setActiveBranchId } = useBranch();
  const [isOpen, setIsOpen] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(e.target as Node)) {
        setIsOpen(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  if (branches.length === 0) {
    return null;
  }

  return (
    <div className="relative" ref={dropdownRef}>
      <button
        onClick={() => setIsOpen(!isOpen)}
        className="flex items-center gap-2 px-3 py-1.5 rounded-lg border border-slate-200 dark:border-slate-800 bg-slate-50 dark:bg-slate-900/60 hover:bg-slate-100 dark:hover:bg-slate-800 transition text-left text-xs max-w-xs"
        title={t('companies.switcher.switchBranch')}
      >
        <div className="w-6 h-6 rounded-md bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 flex items-center justify-center overflow-hidden shrink-0">
          {activeBranch?.logoUrl ? (
            <img src={activeBranch.logoUrl} alt={activeBranch.tradeName} className="w-full h-full object-contain p-0.5" />
          ) : activeBranch?.type === 'HEADQUARTERS' ? (
            <Building2 size={13} className="text-primary" />
          ) : (
            <Store size={13} className="text-purple-500" />
          )}
        </div>

        <div className="min-w-0 flex-1">
          <div className="font-semibold text-slate-800 dark:text-slate-100 truncate flex items-center gap-1.5">
            <span className="truncate">{activeBranch?.tradeName || t('companies.switcher.selectBranch')}</span>
            {activeBranch && (
              <span className="text-[10px] uppercase font-mono px-1 py-0.2 rounded bg-slate-200 dark:bg-slate-800 text-slate-600 dark:text-slate-400 shrink-0">
                #{activeBranch.code}
              </span>
            )}
          </div>
          {activeBranch && (
            <div className="text-[10px] text-slate-400 truncate">
              {activeBranch.cnpj} • {activeBranch.cityName}/{activeBranch.stateCode}
            </div>
          )}
        </div>

        <ChevronDown size={14} className="text-slate-400 shrink-0 ml-1" />
      </button>

      {isOpen && (
        <div className="absolute right-0 mt-1.5 w-72 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl shadow-xl z-50 overflow-hidden py-1">
          <div className="px-3 py-2 border-b border-slate-100 dark:border-slate-800 text-[11px] font-semibold text-slate-400 uppercase tracking-wider">
            {t('companies.switcher.activeBranch')}
          </div>

          <div className="max-h-64 overflow-y-auto divide-y divide-slate-100 dark:divide-slate-800/60">
            {branches.map((b) => {
              const isSelected = b.id === activeBranch?.id;
              return (
                <button
                  key={b.id}
                  onClick={() => {
                    setActiveBranchId(b.id);
                    setIsOpen(false);
                  }}
                  className={`w-full flex items-center justify-between px-3 py-2 text-left text-xs transition hover:bg-slate-50 dark:hover:bg-slate-800/50 ${
                    isSelected ? 'bg-primary/5 dark:bg-primary/10 text-primary font-medium' : 'text-slate-700 dark:text-slate-300'
                  }`}
                >
                  <div className="flex items-start gap-2 min-w-0 pr-2">
                    <div className="mt-0.5 text-slate-400">
                      {b.type === 'HEADQUARTERS' ? <Building2 size={13} /> : <Store size={13} />}
                    </div>
                    <div className="min-w-0">
                      <div className="truncate font-medium flex items-center gap-1.5">
                        <span className="truncate">{b.tradeName}</span>
                        <span className="text-[10px] uppercase font-mono px-1 py-0.2 rounded bg-slate-100 dark:bg-slate-800 text-slate-500">
                          #{b.code}
                        </span>
                      </div>
                      <div className="text-[10px] text-slate-400 truncate font-mono">
                        {b.cnpj} • {b.cityName}/{b.stateCode}
                      </div>
                    </div>
                  </div>

                  {isSelected && <Check size={14} className="text-primary shrink-0" />}
                </button>
              );
            })}
          </div>
        </div>
      )}
    </div>
  );
};
