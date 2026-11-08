import React, { useState, useEffect } from 'react';
import { CustomerCategory } from '../../types/crm';
import { crmApi } from '../../services/crmApi';
import { X, Tag, Plus, Sparkles } from 'lucide-react';
import { generateCustomerCategoryCode } from '../../utils/codeGenerator';

interface CustomerCategoryModalProps {
  isOpen: boolean;
  onClose: () => void;
  onUpdated: () => void;
}

export const CustomerCategoryModal: React.FC<CustomerCategoryModalProps> = ({
  isOpen,
  onClose,
  onUpdated,
}) => {
  const [categories, setCategories] = useState<CustomerCategory[]>([]);
  const [loading, setLoading] = useState(false);
  const [code, setCode] = useState('');
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [colorHex, setColorHex] = useState('#3B82F6');
  const [defaultDiscountPct, setDefaultDiscountPct] = useState('0');
  const [error, setError] = useState<string | null>(null);

  const fetchCategories = async () => {
    try {
      setLoading(true);
      const data = await crmApi.listCustomerCategories();
      setCategories(data);
    } catch (err: any) {
      setError(err?.response?.data?.message || 'Erro ao carregar categorias.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (isOpen) {
      fetchCategories();
      setCode(generateCustomerCategoryCode());
      setError(null);
    }
  }, [isOpen]);

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim()) return;

    try {
      setError(null);
      await crmApi.createCustomerCategory({
        code: (code.trim() || generateCustomerCategoryCode()).toUpperCase(),
        name: name.trim(),
        description: description.trim() || undefined,
        colorHex,
        defaultDiscountPct: parseFloat(defaultDiscountPct) || 0,
      });
      setCode(generateCustomerCategoryCode());
      setName('');
      setDescription('');
      setDefaultDiscountPct('0');
      await fetchCategories();
      onUpdated();
    } catch (err: any) {
      setError(err?.response?.data?.message || 'Erro ao salvar categoria.');
    }
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
      <div className="w-full max-w-xl rounded-xl bg-white shadow-xl dark:bg-gray-800">
        <div className="flex items-center justify-between border-b border-gray-200 px-6 py-4 dark:border-gray-700">
          <div className="flex items-center gap-2">
            <Tag className="h-5 w-5 text-indigo-600 dark:text-indigo-400" />
            <h2 className="text-lg font-semibold text-gray-900 dark:text-white">
              Categorias de Clientes
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
            <h3 className="text-sm font-medium text-gray-900 dark:text-white">Nova Categoria</h3>
            <div className="grid grid-cols-2 gap-3">
              <div>
                <div className="flex items-center justify-between mb-1">
                  <label className="text-xs font-medium text-gray-700 dark:text-gray-300">Código *</label>
                  <button
                    type="button"
                    onClick={() => setCode(generateCustomerCategoryCode())}
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
                  placeholder="EX: CAT-123"
                  value={code}
                  onChange={(e) => setCode(e.target.value.toUpperCase())}
                  className="w-full rounded-md border border-gray-300 px-3 py-1.5 text-sm dark:border-gray-600 dark:bg-gray-700 dark:text-white"
                />
                <span className="text-[10px] text-gray-400 mt-0.5 block">Gerado automaticamente (editável).</span>
              </div>
              <div>
                <label className="block text-xs font-medium text-gray-700 dark:text-gray-300">Nome *</label>
                <input
                  type="text"
                  required
                  placeholder="Ex: Clientes Atacado"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  className="mt-1 w-full rounded-md border border-gray-300 px-3 py-1.5 text-sm dark:border-gray-600 dark:bg-gray-700 dark:text-white"
                />
              </div>
            </div>

            <div className="grid grid-cols-2 gap-3">
              <div>
                <label className="block text-xs font-medium text-gray-700 dark:text-gray-300">Cor do Badge</label>
                <div className="mt-1 flex items-center gap-2">
                  <input
                    type="color"
                    value={colorHex}
                    onChange={(e) => setColorHex(e.target.value)}
                    className="h-8 w-12 cursor-pointer rounded border border-gray-300 dark:border-gray-600"
                  />
                  <span className="text-xs text-gray-600 dark:text-gray-400 font-mono">{colorHex}</span>
                </div>
              </div>
              <div>
                <label className="block text-xs font-medium text-gray-700 dark:text-gray-300">Desconto Padrão (%)</label>
                <input
                  type="number"
                  step="0.01"
                  min="0"
                  max="100"
                  value={defaultDiscountPct}
                  onChange={(e) => setDefaultDiscountPct(e.target.value)}
                  className="mt-1 w-full rounded-md border border-gray-300 px-3 py-1.5 text-sm dark:border-gray-600 dark:bg-gray-700 dark:text-white"
                />
              </div>
            </div>

            <div>
              <label className="block text-xs font-medium text-gray-700 dark:text-gray-300">Descrição</label>
              <input
                type="text"
                placeholder="Regras comerciais aplicáveis"
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                className="mt-1 w-full rounded-md border border-gray-300 px-3 py-1.5 text-sm dark:border-gray-600 dark:bg-gray-700 dark:text-white"
              />
            </div>

            <div className="flex justify-end">
              <button
                type="submit"
                className="inline-flex items-center gap-1.5 rounded-lg bg-indigo-600 px-4 py-2 text-xs font-medium text-white hover:bg-indigo-700"
              >
                <Plus className="h-4 w-4" /> Adicionar Categoria
              </button>
            </div>
          </form>

          {/* Existing Categories List */}
          <div className="max-h-60 overflow-y-auto space-y-2">
            <h3 className="text-xs font-semibold uppercase tracking-wider text-gray-500 dark:text-gray-400">
              Categorias Cadastradas ({categories.length})
            </h3>
            {categories.map((c) => (
              <div
                key={c.id}
                className="flex items-center justify-between rounded-lg border border-gray-200 p-3 dark:border-gray-700 dark:bg-gray-750"
              >
                <div className="flex items-center gap-2">
                  <span
                    className="inline-block h-3.5 w-3.5 rounded-full"
                    style={{ backgroundColor: c.colorHex }}
                  />
                  <div>
                    <span className="font-semibold text-sm text-gray-900 dark:text-white">{c.name}</span>
                    <span className="ml-2 text-xs text-gray-500 dark:text-gray-400 font-mono">({c.code})</span>
                    {c.defaultDiscountPct > 0 && (
                      <span className="ml-2 rounded bg-green-50 px-1.5 py-0.5 text-xs text-green-700 dark:bg-green-900/30 dark:text-green-300">
                        -{c.defaultDiscountPct}%
                      </span>
                    )}
                  </div>
                </div>
                <span className={`text-xs px-2 py-0.5 rounded-full ${c.isActive ? 'bg-green-100 text-green-800 dark:bg-green-900/50 dark:text-green-300' : 'bg-gray-100 text-gray-600'}`}>
                  {c.isActive ? 'Ativa' : 'Inativa'}
                </span>
              </div>
            ))}
            {categories.length === 0 && !loading && (
              <p className="text-center py-4 text-xs text-gray-500 dark:text-gray-400">
                Nenhuma categoria cadastrada ainda.
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
