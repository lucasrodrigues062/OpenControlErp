import React, { useState, useEffect } from 'react';
import { EconomicGroup } from '../../types/crm';
import { crmApi } from '../../services/crmApi';
import { X, Network, Plus, Sparkles } from 'lucide-react';
import { generateEconomicGroupCode } from '../../utils/codeGenerator';

interface EconomicGroupModalProps {
  isOpen: boolean;
  onClose: () => void;
  onUpdated: () => void;
}

export const EconomicGroupModal: React.FC<EconomicGroupModalProps> = ({
  isOpen,
  onClose,
  onUpdated,
}) => {
  const [groups, setGroups] = useState<EconomicGroup[]>([]);
  const [loading, setLoading] = useState(false);
  const [code, setCode] = useState('');
  const [name, setName] = useState('');
  const [holdingCnpj, setHoldingCnpj] = useState('');
  const [consolidatedCreditLimit, setConsolidatedCreditLimit] = useState('0');
  const [error, setError] = useState<string | null>(null);

  const fetchGroups = async () => {
    try {
      setLoading(true);
      const data = await crmApi.listEconomicGroups();
      setGroups(data);
    } catch (err: any) {
      setError(err?.response?.data?.message || 'Erro ao carregar grupos econômicos.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (isOpen) {
      fetchGroups();
      setCode(generateEconomicGroupCode());
      setError(null);
    }
  }, [isOpen]);

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim()) return;

    try {
      setError(null);
      await crmApi.createEconomicGroup({
        code: (code.trim() || generateEconomicGroupCode()).toUpperCase(),
        name: name.trim(),
        holdingCnpj: holdingCnpj.replace(/\D/g, '') || undefined,
        consolidatedCreditLimit: parseFloat(consolidatedCreditLimit) || 0,
      });
      setCode(generateEconomicGroupCode());
      setName('');
      setHoldingCnpj('');
      setConsolidatedCreditLimit('0');
      await fetchGroups();
      onUpdated();
    } catch (err: any) {
      setError(err?.response?.data?.message || 'Erro ao salvar grupo econômico.');
    }
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
      <div className="w-full max-w-xl rounded-xl bg-white shadow-xl dark:bg-gray-800">
        <div className="flex items-center justify-between border-b border-gray-200 px-6 py-4 dark:border-gray-700">
          <div className="flex items-center gap-2">
            <Network className="h-5 w-5 text-indigo-600 dark:text-indigo-400" />
            <h2 className="text-lg font-semibold text-gray-900 dark:text-white">
              Grupos Econômicos (Holdings)
            </h2>
          </div>
          <button
            onClick={onClose}
            className="rounded-lg p-1 text-gray-400 hover:bg-gray-100 hover:text-gray-700 dark:hover:bg-gray-700"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        <div className="p-6">
          {error && (
            <div className="mb-4 rounded-lg bg-red-50 p-3 text-sm text-red-700 dark:bg-red-900/30 dark:text-red-300">
              {error}
            </div>
          )}

          {/* Quick Create Form */}
          <form onSubmit={handleCreate} className="mb-6 space-y-4 rounded-lg border border-gray-200 p-4 dark:border-gray-700">
            <h3 className="text-sm font-medium text-gray-900 dark:text-white">Novo Grupo Econômico</h3>
            <div className="grid grid-cols-2 gap-3">
              <div>
                <div className="flex items-center justify-between mb-1">
                  <label className="text-xs font-medium text-gray-700 dark:text-gray-300">Código *</label>
                  <button
                    type="button"
                    onClick={() => setCode(generateEconomicGroupCode())}
                    className="text-xs text-blue-600 hover:text-blue-700 font-medium flex items-center gap-1 cursor-pointer"
                    title="Gerar novo código"
                  >
                    <Sparkles className="h-3 w-3" />
                    Gerar outro
                  </button>
                </div>
                <input
                  type="text"
                  required
                  placeholder="EX: GRP-01"
                  value={code}
                  onChange={(e) => setCode(e.target.value.toUpperCase())}
                  className="w-full rounded-md border border-gray-300 px-3 py-1.5 text-sm dark:border-gray-600 dark:bg-gray-700 dark:text-white"
                />
                <span className="text-[10px] text-gray-400 mt-0.5 block">Gerado automaticamente (editável).</span>
              </div>
              <div>
                <label className="block text-xs font-medium text-gray-700 dark:text-gray-300">Razão Social / Nome *</label>
                <input
                  type="text"
                  required
                  placeholder="Ex: Grupo Sim Tecnologia"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  className="mt-1 w-full rounded-md border border-gray-300 px-3 py-1.5 text-sm dark:border-gray-600 dark:bg-gray-700 dark:text-white"
                />
              </div>
            </div>

            <div className="grid grid-cols-2 gap-3">
              <div>
                <label className="block text-xs font-medium text-gray-700 dark:text-gray-300">CNPJ da Holding (opcional)</label>
                <input
                  type="text"
                  placeholder="00.000.000/0000-00"
                  value={holdingCnpj}
                  onChange={(e) => setHoldingCnpj(e.target.value)}
                  className="mt-1 w-full rounded-md border border-gray-300 px-3 py-1.5 text-sm dark:border-gray-600 dark:bg-gray-700 dark:text-white"
                />
              </div>
              <div>
                <label className="block text-xs font-medium text-gray-700 dark:text-gray-300">Limite de Crédito Consolidado (R$)</label>
                <input
                  type="number"
                  step="0.01"
                  min="0"
                  value={consolidatedCreditLimit}
                  onChange={(e) => setConsolidatedCreditLimit(e.target.value)}
                  className="mt-1 w-full rounded-md border border-gray-300 px-3 py-1.5 text-sm dark:border-gray-600 dark:bg-gray-700 dark:text-white"
                />
              </div>
            </div>

            <div className="flex justify-end">
              <button
                type="submit"
                className="inline-flex items-center gap-1.5 rounded-lg bg-indigo-600 px-4 py-2 text-xs font-medium text-white hover:bg-indigo-700"
              >
                <Plus className="h-4 w-4" /> Cadastrar Grupo
              </button>
            </div>
          </form>

          {/* Existing Groups */}
          <div className="max-h-60 overflow-y-auto space-y-2">
            <h3 className="text-xs font-semibold uppercase tracking-wider text-gray-500 dark:text-gray-400">
              Grupos Ativos ({groups.length})
            </h3>
            {groups.map((g) => (
              <div
                key={g.id}
                className="flex items-center justify-between rounded-lg border border-gray-200 p-3 dark:border-gray-700"
              >
                <div>
                  <span className="font-semibold text-sm text-gray-900 dark:text-white">{g.name}</span>
                  <span className="ml-2 text-xs text-gray-500 dark:text-gray-400 font-mono">({g.code})</span>
                  {g.consolidatedCreditLimit > 0 && (
                    <div className="text-xs text-emerald-600 dark:text-emerald-400 font-medium mt-0.5">
                      Crédito Total: R$ {g.consolidatedCreditLimit.toLocaleString('pt-BR', { minimumFractionDigits: 2 })}
                    </div>
                  )}
                </div>
                <span className={`text-xs px-2 py-0.5 rounded-full ${g.isActive ? 'bg-green-100 text-green-800 dark:bg-green-900/50 dark:text-green-300' : 'bg-gray-100 text-gray-600'}`}>
                  {g.isActive ? 'Ativo' : 'Inativo'}
                </span>
              </div>
            ))}
            {groups.length === 0 && !loading && (
              <p className="text-center py-4 text-xs text-gray-500 dark:text-gray-400">
                Nenhum grupo econômico cadastrado.
              </p>
            )}
          </div>
        </div>

        <div className="flex justify-end border-t border-gray-200 px-6 py-4 dark:border-gray-700">
          <button
            onClick={onClose}
            className="rounded-lg bg-gray-100 px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-200 dark:bg-gray-700 dark:text-gray-300"
          >
            Fechar
          </button>
        </div>
      </div>
    </div>
  );
};
