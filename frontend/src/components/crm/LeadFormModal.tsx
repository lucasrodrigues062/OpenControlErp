import React, { useState, useEffect } from 'react';
import { Lead, CreateLeadRequest, LeadSource } from '../../types/crm';
import { crmApi } from '../../services/crmApi';
import { X, Target } from 'lucide-react';

interface LeadFormModalProps {
  isOpen: boolean;
  onClose: () => void;
  leadToEdit?: Lead | null;
  onSaved: () => void;
}

export const LeadFormModal: React.FC<LeadFormModalProps> = ({
  isOpen,
  onClose,
  leadToEdit,
  onSaved,
}) => {
  const [name, setName] = useState('');
  const [companyName, setCompanyName] = useState('');
  const [email, setEmail] = useState('');
  const [phone, setPhone] = useState('');
  const [source, setSource] = useState<LeadSource>('WEBSITE');
  const [estimatedBudget, setEstimatedBudget] = useState('0');
  const [interestSummary, setInterestSummary] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (isOpen) {
      if (leadToEdit) {
        setName(leadToEdit.name);
        setCompanyName(leadToEdit.companyName || '');
        setEmail(leadToEdit.email || '');
        setPhone(leadToEdit.phone || '');
        setSource(leadToEdit.source);
        setEstimatedBudget(leadToEdit.estimatedBudget?.toString() || '0');
        setInterestSummary(leadToEdit.interestSummary || '');
      } else {
        setName('');
        setCompanyName('');
        setEmail('');
        setPhone('');
        setSource('WEBSITE');
        setEstimatedBudget('0');
        setInterestSummary('');
      }
      setError(null);
    }
  }, [isOpen, leadToEdit]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim()) {
      setError('Nome do contato é obrigatório.');
      return;
    }

    try {
      setLoading(true);
      setError(null);
      const payload: CreateLeadRequest = {
        name: name.trim(),
        companyName: companyName.trim() || undefined,
        email: email.trim() || undefined,
        phone: phone.trim() || undefined,
        source,
        estimatedBudget: parseFloat(estimatedBudget) || 0,
        interestSummary: interestSummary.trim() || undefined,
      };

      if (leadToEdit) {
        await crmApi.updateLead(leadToEdit.id, payload);
      } else {
        await crmApi.createLead(payload);
      }

      onSaved();
      onClose();
    } catch (err: any) {
      setError(err?.response?.data?.message || 'Erro ao salvar lead.');
    } finally {
      setLoading(false);
    }
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
      <div className="w-full max-w-lg rounded-xl bg-white shadow-xl dark:bg-gray-800">
        <div className="flex items-center justify-between border-b border-gray-200 px-6 py-4 dark:border-gray-700">
          <div className="flex items-center gap-2">
            <Target className="h-5 w-5 text-indigo-600 dark:text-indigo-400" />
            <h2 className="text-lg font-semibold text-gray-900 dark:text-white">
              {leadToEdit ? 'Editar Lead' : 'Novo Lead / Prospect'}
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

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-medium text-gray-700 dark:text-gray-300">Nome do Contato *</label>
              <input
                type="text"
                required
                placeholder="Ex: Carlos Silva"
                value={name}
                onChange={(e) => setName(e.target.value)}
                className="mt-1 w-full rounded-md border border-gray-300 px-3 py-1.5 text-sm dark:border-gray-600 dark:bg-gray-700 dark:text-white"
              />
            </div>
            <div>
              <label className="block text-xs font-medium text-gray-700 dark:text-gray-300">Nome da Empresa</label>
              <input
                type="text"
                placeholder="Ex: Silva Distribuidora"
                value={companyName}
                onChange={(e) => setCompanyName(e.target.value)}
                className="mt-1 w-full rounded-md border border-gray-300 px-3 py-1.5 text-sm dark:border-gray-600 dark:bg-gray-700 dark:text-white"
              />
            </div>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-medium text-gray-700 dark:text-gray-300">E-mail</label>
              <input
                type="email"
                placeholder="carlos@empresa.com"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                className="mt-1 w-full rounded-md border border-gray-300 px-3 py-1.5 text-sm dark:border-gray-600 dark:bg-gray-700 dark:text-white"
              />
            </div>
            <div>
              <label className="block text-xs font-medium text-gray-700 dark:text-gray-300">Telefone / WhatsApp</label>
              <input
                type="text"
                placeholder="(11) 99999-9999"
                value={phone}
                onChange={(e) => setPhone(e.target.value)}
                className="mt-1 w-full rounded-md border border-gray-300 px-3 py-1.5 text-sm dark:border-gray-600 dark:bg-gray-700 dark:text-white"
              />
            </div>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-medium text-gray-700 dark:text-gray-300">Origem / Canal</label>
              <select
                value={source}
                onChange={(e) => setSource(e.target.value as LeadSource)}
                className="mt-1 w-full rounded-md border border-gray-300 px-3 py-1.5 text-sm dark:border-gray-600 dark:bg-gray-700 dark:text-white"
              >
                <option value="WEBSITE">Site / Formulário</option>
                <option value="INBOUND">Inbound / Contato Direto</option>
                <option value="OUTBOUND">Outbound / Prospecção Ativa</option>
                <option value="REFERRAL">Indicação</option>
                <option value="EVENT">Feira / Evento</option>
                <option value="SOCIAL_MEDIA">Redes Sociais</option>
                <option value="OTHER">Outro</option>
              </select>
            </div>
            <div>
              <label className="block text-xs font-medium text-gray-700 dark:text-gray-300">Orçamento Estimado (R$)</label>
              <input
                type="number"
                step="0.01"
                min="0"
                value={estimatedBudget}
                onChange={(e) => setEstimatedBudget(e.target.value)}
                className="mt-1 w-full rounded-md border border-gray-300 px-3 py-1.5 text-sm dark:border-gray-600 dark:bg-gray-700 dark:text-white"
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-medium text-gray-700 dark:text-gray-300">Resumo do Interesse</label>
            <textarea
              rows={3}
              placeholder="Descreva o que o lead busca ou pontos levantados na conversa..."
              value={interestSummary}
              onChange={(e) => setInterestSummary(e.target.value)}
              className="mt-1 w-full rounded-md border border-gray-300 px-3 py-2 text-sm dark:border-gray-600 dark:bg-gray-700 dark:text-white"
            />
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
              {loading ? 'Salvando...' : leadToEdit ? 'Atualizar Lead' : 'Cadastrar Lead'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
