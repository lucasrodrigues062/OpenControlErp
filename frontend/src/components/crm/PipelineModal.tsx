import React, { useState, useEffect } from 'react';
import { crmApi } from '../../services/crmApi';
import { X, GitBranch, Sparkles } from 'lucide-react';
import { generatePipelineCode } from '../../utils/codeGenerator';

interface PipelineModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSaved: (pipelineId: string) => void;
}

export const PipelineModal: React.FC<PipelineModalProps> = ({
  isOpen,
  onClose,
  onSaved,
}) => {
  const [code, setCode] = useState('');
  const [name, setName] = useState('');
  const [isDefault, setIsDefault] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (isOpen) {
      setCode(generatePipelineCode());
      setName('');
      setIsDefault(false);
      setError(null);
    }
  }, [isOpen]);

  if (!isOpen) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    const finalCode = (code.trim() || generatePipelineCode()).toUpperCase();
    if (!name.trim()) {
      setError('Nome do funil é obrigatório.');
      return;
    }

    try {
      setLoading(true);
      setError(null);
      const created = await crmApi.createPipeline({
        code: finalCode,
        name: name.trim(),
        isDefault,
      });
      setCode('');
      setName('');
      setIsDefault(false);
      onSaved(created.id);
      onClose();
    } catch (err: any) {
      setError(err?.response?.data?.detail || 'Erro ao criar funil de vendas.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
      <div className="w-full max-w-md rounded-xl bg-white shadow-xl dark:bg-gray-800 border border-gray-200 dark:border-gray-700">
        <div className="flex items-center justify-between border-b border-gray-200 p-4 dark:border-gray-700">
          <div className="flex items-center gap-2">
            <GitBranch className="h-5 w-5 text-indigo-600 dark:text-indigo-400" />
            <h2 className="text-lg font-bold text-gray-900 dark:text-white">
              Novo Funil de Vendas
            </h2>
          </div>
          <button
            onClick={onClose}
            className="rounded-lg p-1 text-gray-400 hover:bg-gray-100 hover:text-gray-700 dark:hover:bg-gray-700"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="p-6 space-y-4">
          {error && (
            <div className="rounded-lg bg-red-50 p-3 text-xs text-red-700 dark:bg-red-900/30 dark:text-red-300">
              {error}
            </div>
          )}

          <div>
            <div className="flex items-center justify-between">
              <label className="block text-xs font-medium text-gray-700 dark:text-gray-300">
                Código Único * <span className="font-normal text-gray-400">(Auto-gerado, editável)</span>
              </label>
              <button
                type="button"
                onClick={() => setCode(generatePipelineCode())}
                className="flex items-center gap-1 text-[11px] font-medium text-indigo-600 hover:text-indigo-700 dark:text-indigo-400"
              >
                <Sparkles className="h-3 w-3" /> Gerar outro
              </button>
            </div>
            <input
              type="text"
              required
              placeholder="Ex: PIPE-123"
              value={code}
              onChange={(e) => setCode(e.target.value.toUpperCase())}
              className="mt-1 w-full rounded-md border border-gray-300 px-3 py-1.5 text-sm dark:border-gray-600 dark:bg-gray-700 dark:text-white font-mono"
            />
          </div>

          <div>
            <label className="block text-xs font-medium text-gray-700 dark:text-gray-300">
              Nome do Funil *
            </label>
            <input
              type="text"
              required
              placeholder="Ex: Vendas Corporativas B2B"
              value={name}
              onChange={(e) => setName(e.target.value)}
              className="mt-1 w-full rounded-md border border-gray-300 px-3 py-1.5 text-sm dark:border-gray-600 dark:bg-gray-700 dark:text-white"
            />
          </div>

          <div className="flex items-center gap-2 pt-1">
            <input
              type="checkbox"
              id="isDefaultPipeline"
              checked={isDefault}
              onChange={(e) => setIsDefault(e.target.checked)}
              className="h-4 w-4 rounded border-gray-300 text-indigo-600 focus:ring-indigo-500"
            />
            <label htmlFor="isDefaultPipeline" className="text-xs text-gray-700 dark:text-gray-300">
              Definir como Funil Padrão do CRM
            </label>
          </div>

          <div className="flex justify-end gap-3 pt-4 border-t border-gray-100 dark:border-gray-700">
            <button
              type="button"
              onClick={onClose}
              disabled={loading}
              className="rounded-lg border border-gray-300 px-4 py-2 text-xs font-medium text-gray-700 hover:bg-gray-50 dark:border-gray-600 dark:text-gray-300 dark:hover:bg-gray-700"
            >
              Cancelar
            </button>
            <button
              type="submit"
              disabled={loading}
              className="rounded-lg bg-indigo-600 px-4 py-2 text-xs font-semibold text-white hover:bg-indigo-700 disabled:opacity-50"
            >
              {loading ? 'Criando...' : 'Criar Funil'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
