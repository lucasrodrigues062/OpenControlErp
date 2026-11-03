import React, { useState } from 'react';
import { Deal } from '../../types/crm';
import { X, AlertCircle } from 'lucide-react';

interface DealLossReasonModalProps {
  isOpen: boolean;
  onClose: () => void;
  deal: Deal | null;
  onConfirm: (reason: string) => void;
}

export const DealLossReasonModal: React.FC<DealLossReasonModalProps> = ({
  isOpen,
  onClose,
  deal,
  onConfirm,
}) => {
  const [reason, setReason] = useState('');
  const [predefinedReason, setPredefinedReason] = useState('');

  const commonReasons = [
    'Preço acima do orçamento',
    'Optou pelo concorrente',
    'Projeto adiado / cancelado',
    'Escopo ou funcionalidades não atendem',
    'Falta de retorno do cliente',
    'Outro motivo',
  ];

  if (!isOpen || !deal) return null;

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const finalReason = predefinedReason === 'Outro motivo' || !predefinedReason ? reason.trim() : predefinedReason;
    if (!finalReason) return;
    onConfirm(finalReason);
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
      <div className="w-full max-w-md rounded-xl bg-white p-6 shadow-2xl dark:bg-gray-800">
        <div className="flex items-center justify-between border-b border-gray-200 pb-3 dark:border-gray-700">
          <div className="flex items-center gap-2 text-rose-600 dark:text-rose-400">
            <AlertCircle className="h-5 w-5" />
            <h3 className="text-base font-bold text-gray-900 dark:text-white">
              Marcar Oportunidade como Perdida
            </h3>
          </div>
          <button
            onClick={onClose}
            className="rounded p-1 text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-700"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="mt-4 space-y-4">
          <p className="text-xs text-gray-500 dark:text-gray-400">
            Negócio: <span className="font-semibold text-gray-800 dark:text-gray-200">{deal.title}</span>
          </p>

          <div>
            <label className="block text-xs font-medium text-gray-700 dark:text-gray-300">
              Motivo da Perda (Obrigatório) *
            </label>
            <select
              value={predefinedReason}
              onChange={(e) => {
                setPredefinedReason(e.target.value);
                if (e.target.value !== 'Outro motivo') setReason(e.target.value);
                else setReason('');
              }}
              className="mt-1 w-full rounded-md border border-gray-300 px-3 py-1.5 text-xs dark:border-gray-600 dark:bg-gray-700 dark:text-white"
            >
              <option value="">Selecione um motivo...</option>
              {commonReasons.map((r) => (
                <option key={r} value={r}>
                  {r}
                </option>
              ))}
            </select>
          </div>

          {(predefinedReason === 'Outro motivo' || !predefinedReason) && (
            <div>
              <label className="block text-xs font-medium text-gray-700 dark:text-gray-300">
                Detalhes do Motivo *
              </label>
              <textarea
                rows={3}
                required
                placeholder="Descreva o motivo da perda para alimentar os relatórios comerciais..."
                value={reason}
                onChange={(e) => setReason(e.target.value)}
                className="mt-1 w-full rounded-md border border-gray-300 p-2 text-xs dark:border-gray-600 dark:bg-gray-700 dark:text-white"
              />
            </div>
          )}

          <div className="flex justify-end gap-2 border-t border-gray-200 pt-3 dark:border-gray-700">
            <button
              type="button"
              onClick={onClose}
              className="rounded-lg bg-gray-100 px-3 py-1.5 text-xs font-medium text-gray-700 hover:bg-gray-200 dark:bg-gray-700 dark:text-gray-300"
            >
              Cancelar
            </button>
            <button
              type="submit"
              disabled={!reason.trim() && !predefinedReason}
              className="rounded-lg bg-rose-600 px-4 py-1.5 text-xs font-semibold text-white hover:bg-rose-700 disabled:opacity-50"
            >
              Confirmar Perda
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
