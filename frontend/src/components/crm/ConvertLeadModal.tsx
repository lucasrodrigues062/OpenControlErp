import React, { useState, useEffect } from 'react';
import { Lead, CustomerType, SalesPipeline, ConvertLeadRequest } from '../../types/crm';
import { crmApi } from '../../services/crmApi';
import { X, ArrowRight, UserCheck } from 'lucide-react';

interface ConvertLeadModalProps {
  isOpen: boolean;
  onClose: () => void;
  lead: Lead | null;
  onConverted: () => void;
}

export const ConvertLeadModal: React.FC<ConvertLeadModalProps> = ({
  isOpen,
  onClose,
  lead,
  onConverted,
}) => {
  const [customerType, setCustomerType] = useState<CustomerType>('LEGAL_ENTITY');
  const [customerDocument, setCustomerDocument] = useState('');
  const [customerLegalName, setCustomerLegalName] = useState('');
  const [dealTitle, setDealTitle] = useState('');
  const [dealEstimatedValue, setDealEstimatedValue] = useState('0');
  const [pipelines, setPipelines] = useState<SalesPipeline[]>([]);
  const [selectedPipelineId, setSelectedPipelineId] = useState('');
  const [selectedStageId, setSelectedStageId] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (isOpen && lead) {
      setCustomerType(lead.companyName ? 'LEGAL_ENTITY' : 'INDIVIDUAL');
      setCustomerDocument('');
      setCustomerLegalName(lead.companyName || lead.name);
      setDealTitle(`Oportunidade - ${lead.companyName || lead.name}`);
      setDealEstimatedValue(lead.estimatedBudget?.toString() || '0');
      setError(null);
      loadPipelines();
    }
  }, [isOpen, lead]);

  const loadPipelines = async () => {
    try {
      const data = await crmApi.listPipelines();
      setPipelines(data);
      if (data.length > 0) {
        const defaultPipe = data.find((p) => p.isDefault) || data[0];
        setSelectedPipelineId(defaultPipe.id);
        if (defaultPipe.stages.length > 0) {
          setSelectedStageId(defaultPipe.stages[0].id);
        }
      }
    } catch (err) {
      console.error('Error loading pipelines:', err);
    }
  };

  const handlePipelineChange = (pipeId: string) => {
    setSelectedPipelineId(pipeId);
    const pipe = pipelines.find((p) => p.id === pipeId);
    if (pipe && pipe.stages.length > 0) {
      setSelectedStageId(pipe.stages[0].id);
    } else {
      setSelectedStageId('');
    }
  };

  const handleConvert = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!lead) return;
    if (!customerDocument.trim() || !dealTitle.trim() || !selectedPipelineId || !selectedStageId) {
      setError('Por favor, preencha todos os campos obrigatórios.');
      return;
    }

    try {
      setLoading(true);
      setError(null);
      const payload: ConvertLeadRequest = {
        customerType,
        customerDocument: customerDocument.replace(/\D/g, ''),
        customerLegalName: customerLegalName.trim() || undefined,
        dealTitle: dealTitle.trim(),
        dealEstimatedValue: parseFloat(dealEstimatedValue) || 0,
        pipelineId: selectedPipelineId,
        stageId: selectedStageId,
      };

      await crmApi.convertLead(lead.id, payload);
      onConverted();
      onClose();
    } catch (err: any) {
      setError(err?.response?.data?.message || 'Falha ao converter lead. Verifique o documento informado.');
    } finally {
      setLoading(false);
    }
  };

  if (!isOpen || !lead) return null;

  const currentPipeline = pipelines.find((p) => p.id === selectedPipelineId);

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
      <div className="w-full max-w-lg rounded-xl bg-white shadow-2xl dark:bg-gray-800">
        <div className="flex items-center justify-between border-b border-gray-200 px-6 py-4 dark:border-gray-700">
          <div className="flex items-center gap-2">
            <UserCheck className="h-5 w-5 text-emerald-600 dark:text-emerald-400" />
            <h2 className="text-lg font-bold text-gray-900 dark:text-white">
              Converter Lead em Cliente & Oportunidade
            </h2>
          </div>
          <button
            onClick={onClose}
            className="rounded-lg p-1 text-gray-400 hover:bg-gray-100 hover:text-gray-700 dark:hover:bg-gray-700"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        <form onSubmit={handleConvert} className="p-6 space-y-4">
          {error && (
            <div className="rounded-lg bg-red-50 p-3 text-xs text-red-700 dark:bg-red-900/30 dark:text-red-300">
              {error}
            </div>
          )}

          {/* Lead Summary Callout */}
          <div className="rounded-lg bg-gray-50 p-3 text-xs dark:bg-gray-750 border border-gray-200 dark:border-gray-700">
            <span className="font-semibold text-gray-900 dark:text-white">Lead:</span> {lead.name}
            {lead.companyName && <span> ({lead.companyName})</span>}
            <div className="mt-1 text-gray-500">
              Canal: <span className="font-medium text-gray-700 dark:text-gray-300">{lead.source}</span>
              {lead.estimatedBudget > 0 && (
                <span className="ml-3 font-medium text-emerald-600 dark:text-emerald-400">
                  Orçamento: R$ {lead.estimatedBudget.toLocaleString('pt-BR', { minimumFractionDigits: 2 })}
                </span>
              )}
            </div>
          </div>

          {/* Customer Creation Section */}
          <div className="space-y-3 border-t border-gray-200 pt-3 dark:border-gray-700">
            <h3 className="text-xs font-bold uppercase tracking-wider text-indigo-600 dark:text-indigo-400">
              1. Novo Cadastro de Cliente
            </h3>

            <div className="flex gap-2">
              <button
                type="button"
                onClick={() => setCustomerType('LEGAL_ENTITY')}
                className={`flex-1 rounded-md py-1.5 text-xs font-semibold ${
                  customerType === 'LEGAL_ENTITY'
                    ? 'bg-indigo-600 text-white'
                    : 'bg-gray-100 text-gray-700 dark:bg-gray-700 dark:text-gray-300'
                }`}
              >
                Pessoa Jurídica (CNPJ)
              </button>
              <button
                type="button"
                onClick={() => setCustomerType('INDIVIDUAL')}
                className={`flex-1 rounded-md py-1.5 text-xs font-semibold ${
                  customerType === 'INDIVIDUAL'
                    ? 'bg-indigo-600 text-white'
                    : 'bg-gray-100 text-gray-700 dark:bg-gray-700 dark:text-gray-300'
                }`}
              >
                Pessoa Física (CPF)
              </button>
            </div>

            <div className="grid grid-cols-2 gap-3">
              <div>
                <label className="block text-xs font-medium text-gray-700 dark:text-gray-300">
                  {customerType === 'LEGAL_ENTITY' ? 'CNPJ *' : 'CPF *'}
                </label>
                <input
                  type="text"
                  required
                  placeholder={customerType === 'LEGAL_ENTITY' ? '00.000.000/0000-00' : '000.000.000-00'}
                  value={customerDocument}
                  onChange={(e) => setCustomerDocument(e.target.value)}
                  className="mt-1 w-full rounded-md border border-gray-300 px-3 py-1.5 text-sm dark:border-gray-600 dark:bg-gray-700 dark:text-white font-mono"
                />
              </div>
              <div>
                <label className="block text-xs font-medium text-gray-700 dark:text-gray-300">
                  {customerType === 'LEGAL_ENTITY' ? 'Razão Social *' : 'Nome Completo *'}
                </label>
                <input
                  type="text"
                  required
                  value={customerLegalName}
                  onChange={(e) => setCustomerLegalName(e.target.value)}
                  className="mt-1 w-full rounded-md border border-gray-300 px-3 py-1.5 text-sm dark:border-gray-600 dark:bg-gray-700 dark:text-white"
                />
              </div>
            </div>
          </div>

          {/* Deal Creation Section */}
          <div className="space-y-3 border-t border-gray-200 pt-3 dark:border-gray-700">
            <h3 className="text-xs font-bold uppercase tracking-wider text-emerald-600 dark:text-emerald-400">
              2. Nova Oportunidade no Funil
            </h3>

            <div>
              <label className="block text-xs font-medium text-gray-700 dark:text-gray-300">Título do Negócio *</label>
              <input
                type="text"
                required
                value={dealTitle}
                onChange={(e) => setDealTitle(e.target.value)}
                className="mt-1 w-full rounded-md border border-gray-300 px-3 py-1.5 text-sm dark:border-gray-600 dark:bg-gray-700 dark:text-white"
              />
            </div>

            <div className="grid grid-cols-2 gap-3">
              <div>
                <label className="block text-xs font-medium text-gray-700 dark:text-gray-300">Funil de Vendas *</label>
                <select
                  value={selectedPipelineId}
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
                  value={selectedStageId}
                  onChange={(e) => setSelectedStageId(e.target.value)}
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

            <div>
              <label className="block text-xs font-medium text-gray-700 dark:text-gray-300">Valor Estimado (R$)</label>
              <input
                type="number"
                step="0.01"
                min="0"
                value={dealEstimatedValue}
                onChange={(e) => setDealEstimatedValue(e.target.value)}
                className="mt-1 w-full rounded-md border border-gray-300 px-3 py-1.5 text-sm dark:border-gray-600 dark:bg-gray-700 dark:text-white font-mono"
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
              className="inline-flex items-center gap-1.5 rounded-lg bg-emerald-600 px-4 py-2 text-xs font-semibold text-white hover:bg-emerald-700 disabled:opacity-50"
            >
              <ArrowRight className="h-4 w-4" />
              {loading ? 'Convertendo...' : 'Confirmar Conversão em 1 Clique'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
