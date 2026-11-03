import React, { useState, useEffect } from 'react';
import { Deal, CreateDealRequest, SalesPipeline, Customer } from '../../types/crm';
import { crmApi } from '../../services/crmApi';
import { X, Briefcase } from 'lucide-react';

interface DealFormModalProps {
  isOpen: boolean;
  onClose: () => void;
  dealToEdit?: Deal | null;
  defaultPipelineId?: string;
  defaultStageId?: string;
  pipelines: SalesPipeline[];
  onSaved: () => void;
}

export const DealFormModal: React.FC<DealFormModalProps> = ({
  isOpen,
  onClose,
  dealToEdit,
  defaultPipelineId,
  defaultStageId,
  pipelines,
  onSaved,
}) => {
  const [title, setTitle] = useState('');
  const [customerId, setCustomerId] = useState('');
  const [pipelineId, setPipelineId] = useState('');
  const [stageId, setStageId] = useState('');
  const [estimatedValue, setEstimatedValue] = useState('0');
  const [expectedCloseDate, setExpectedCloseDate] = useState('');
  const [customers, setCustomers] = useState<Customer[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (isOpen) {
      loadCustomers();
      if (dealToEdit) {
        setTitle(dealToEdit.title);
        setCustomerId(dealToEdit.customerId);
        setPipelineId(dealToEdit.pipelineId);
        setStageId(dealToEdit.stageId);
        setEstimatedValue(dealToEdit.estimatedValue?.toString() || '0');
        setExpectedCloseDate(dealToEdit.expectedCloseDate || '');
      } else {
        setTitle('');
        setCustomerId('');
        const pipeId = defaultPipelineId || (pipelines.length > 0 ? pipelines[0].id : '');
        setPipelineId(pipeId);
        const curPipe = pipelines.find((p) => p.id === pipeId);
        setStageId(defaultStageId || (curPipe && curPipe.stages.length > 0 ? curPipe.stages[0].id : ''));
        setEstimatedValue('0');
        setExpectedCloseDate('');
      }
      setError(null);
    }
  }, [isOpen, dealToEdit, defaultPipelineId, defaultStageId, pipelines]);

  const loadCustomers = async () => {
    try {
      const res = await crmApi.listCustomers({ activeOnly: true, size: 100 });
      setCustomers(res.content);
    } catch (e) {
      console.error('Error loading customers:', e);
    }
  };

  const handlePipelineChange = (newPipeId: string) => {
    setPipelineId(newPipeId);
    const pipe = pipelines.find((p) => p.id === newPipeId);
    if (pipe && pipe.stages.length > 0) {
      setStageId(pipe.stages[0].id);
    } else {
      setStageId('');
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!title.trim() || !customerId || !pipelineId || !stageId) {
      setError('Preencha todos os campos obrigatórios.');
      return;
    }

    try {
      setLoading(true);
      setError(null);
      const payload: CreateDealRequest = {
        title: title.trim(),
        customerId,
        pipelineId,
        stageId,
        estimatedValue: parseFloat(estimatedValue) || 0,
        currency: 'BRL',
        expectedCloseDate: expectedCloseDate || undefined,
      };

      if (dealToEdit) {
        await crmApi.updateDeal(dealToEdit.id, payload);
      } else {
        await crmApi.createDeal(payload);
      }

      onSaved();
      onClose();
    } catch (err: any) {
      setError(err?.response?.data?.message || 'Erro ao salvar negócio.');
    } finally {
      setLoading(false);
    }
  };

  if (!isOpen) return null;

  const currentPipeline = pipelines.find((p) => p.id === pipelineId);

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
      <div className="w-full max-w-lg rounded-xl bg-white shadow-2xl dark:bg-gray-800">
        <div className="flex items-center justify-between border-b border-gray-200 px-6 py-4 dark:border-gray-700">
          <div className="flex items-center gap-2">
            <Briefcase className="h-5 w-5 text-indigo-600 dark:text-indigo-400" />
            <h2 className="text-lg font-bold text-gray-900 dark:text-white">
              {dealToEdit ? 'Editar Negócio' : 'Nova Oportunidade'}
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
            <label className="block text-xs font-medium text-gray-700 dark:text-gray-300">
              Título da Oportunidade *
            </label>
            <input
              type="text"
              required
              placeholder="Ex: Licenciamento Anual ERP"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              className="mt-1 w-full rounded-md border border-gray-300 px-3 py-1.5 text-sm dark:border-gray-600 dark:bg-gray-700 dark:text-white"
            />
          </div>

          <div>
            <label className="block text-xs font-medium text-gray-700 dark:text-gray-300">Cliente *</label>
            <select
              required
              value={customerId}
              onChange={(e) => setCustomerId(e.target.value)}
              className="mt-1 w-full rounded-md border border-gray-300 px-3 py-1.5 text-sm dark:border-gray-600 dark:bg-gray-700 dark:text-white"
            >
              <option value="">Selecione o Cliente...</option>
              {customers.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.legalName} ({c.formattedDocument || c.document})
                </option>
              ))}
            </select>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-medium text-gray-700 dark:text-gray-300">Funil de Vendas *</label>
              <select
                value={pipelineId}
                onChange={(e) => handlePipelineChange(e.target.value)}
                className="mt-1 w-full rounded-md border border-gray-300 px-3 py-1.5 text-sm dark:border-gray-600 dark:bg-gray-700 dark:text-white"
              >
                {pipelines.map((p) => (
                  <option key={p.id} value={p.id}>
                    {p.name}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="block text-xs font-medium text-gray-700 dark:text-gray-300">Estágio Inicial *</label>
              <select
                value={stageId}
                onChange={(e) => setStageId(e.target.value)}
                className="mt-1 w-full rounded-md border border-gray-300 px-3 py-1.5 text-sm dark:border-gray-600 dark:bg-gray-700 dark:text-white"
              >
                {currentPipeline?.stages.map((st) => (
                  <option key={st.id} value={st.id}>
                    {st.name} ({st.winProbabilityPct}%)
                  </option>
                ))}
              </select>
            </div>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-medium text-gray-700 dark:text-gray-300">
                Valor Estimado (R$)
              </label>
              <input
                type="number"
                step="0.01"
                min="0"
                value={estimatedValue}
                onChange={(e) => setEstimatedValue(e.target.value)}
                className="mt-1 w-full rounded-md border border-gray-300 px-3 py-1.5 text-sm dark:border-gray-600 dark:bg-gray-700 dark:text-white font-mono"
              />
            </div>

            <div>
              <label className="block text-xs font-medium text-gray-700 dark:text-gray-300">
                Previsão de Fechamento
              </label>
              <input
                type="date"
                value={expectedCloseDate}
                onChange={(e) => setExpectedCloseDate(e.target.value)}
                className="mt-1 w-full rounded-md border border-gray-300 px-3 py-1.5 text-sm dark:border-gray-600 dark:bg-gray-700 dark:text-white"
              />
            </div>
          </div>

          <div className="flex justify-end gap-2 border-t border-gray-200 pt-4 dark:border-gray-700">
            <button
              type="button"
              onClick={onClose}
              className="rounded-lg bg-gray-100 px-4 py-2 text-xs font-medium text-gray-700 hover:bg-gray-200 dark:bg-gray-700 dark:text-gray-300"
            >
              Cancelar
            </button>
            <button
              type="submit"
              disabled={loading}
              className="rounded-lg bg-indigo-600 px-4 py-2 text-xs font-medium text-white hover:bg-indigo-700 disabled:opacity-50"
            >
              {loading ? 'Salvando...' : dealToEdit ? 'Atualizar Negócio' : 'Criar Negócio'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
