import React, { useState, useEffect } from 'react';
import { Lead, LeadStatus } from '../../types/crm';
import { crmApi } from '../../services/crmApi';
import { LeadFormModal } from '../../components/crm/LeadFormModal';
import { ConvertLeadModal } from '../../components/crm/ConvertLeadModal';
import {
  Target,
  Plus,
  Search,
  XCircle,
  ArrowRight,
  Edit,
  Mail,
  Phone,
  Building,
} from 'lucide-react';

export const LeadsPage: React.FC = () => {
  const [leads, setLeads] = useState<Lead[]>([]);
  const [loading, setLoading] = useState(false);
  const [activeTab, setActiveTab] = useState<string>('ALL');
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedSource, setSelectedSource] = useState<string>('');

  // Modals
  const [isLeadModalOpen, setIsLeadModalOpen] = useState(false);
  const [leadToEdit, setLeadToEdit] = useState<Lead | null>(null);

  const [isConvertModalOpen, setIsConvertModalOpen] = useState(false);
  const [leadToConvert, setLeadToConvert] = useState<Lead | null>(null);

  // Disqualify Modal
  const [isDisqualifyModalOpen, setIsDisqualifyModalOpen] = useState(false);
  const [leadToDisqualify, setLeadToDisqualify] = useState<Lead | null>(null);
  const [disqualificationReason, setDisqualificationReason] = useState('');

  const fetchLeads = async () => {
    try {
      setLoading(true);
      const data = await crmApi.listLeads({
        status: activeTab !== 'ALL' ? activeTab : undefined,
        source: selectedSource || undefined,
      });
      setLeads(data);
    } catch (err) {
      console.error('Error fetching leads:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchLeads();
  }, [activeTab, selectedSource]);

  const handleStatusChange = async (lead: Lead, newStatus: string) => {
    try {
      await crmApi.updateLeadStatus(lead.id, newStatus);
      await fetchLeads();
    } catch (err) {
      console.error('Error updating lead status:', err);
    }
  };

  const handleOpenDisqualify = (lead: Lead) => {
    setLeadToDisqualify(lead);
    setDisqualificationReason('');
    setIsDisqualifyModalOpen(true);
  };

  const handleConfirmDisqualify = async () => {
    if (!leadToDisqualify || !disqualificationReason.trim()) return;
    try {
      await crmApi.disqualifyLead(leadToDisqualify.id, disqualificationReason.trim());
      setIsDisqualifyModalOpen(false);
      setLeadToDisqualify(null);
      await fetchLeads();
    } catch (err) {
      console.error('Error disqualifying lead:', err);
    }
  };

  const handleOpenConvert = (lead: Lead) => {
    setLeadToConvert(lead);
    setIsConvertModalOpen(true);
  };

  const filteredLeads = leads.filter((l) => {
    if (!searchQuery.trim()) return true;
    const q = searchQuery.toLowerCase();
    return (
      l.name.toLowerCase().includes(q) ||
      (l.companyName && l.companyName.toLowerCase().includes(q)) ||
      (l.email && l.email.toLowerCase().includes(q))
    );
  });

  const getStatusBadge = (status: LeadStatus) => {
    switch (status) {
      case 'NEW':
        return <span className="rounded-full bg-blue-100 px-2 py-0.5 text-[10px] font-semibold text-blue-800 dark:bg-blue-900/40 dark:text-blue-300">Novo</span>;
      case 'IN_QUALIFICATION':
        return <span className="rounded-full bg-amber-100 px-2 py-0.5 text-[10px] font-semibold text-amber-800 dark:bg-amber-900/40 dark:text-amber-300">Em Qualificação</span>;
      case 'QUALIFIED':
        return <span className="rounded-full bg-emerald-100 px-2 py-0.5 text-[10px] font-semibold text-emerald-800 dark:bg-emerald-900/40 dark:text-emerald-300">Qualificado</span>;
      case 'CONVERTED':
        return <span className="rounded-full bg-purple-100 px-2 py-0.5 text-[10px] font-semibold text-purple-800 dark:bg-purple-900/40 dark:text-purple-300">Convertido</span>;
      case 'DISQUALIFIED':
        return <span className="rounded-full bg-rose-100 px-2 py-0.5 text-[10px] font-semibold text-rose-800 dark:bg-rose-900/40 dark:text-rose-300">Desqualificado</span>;
    }
  };

  return (
    <div className="p-6 space-y-6">
      {/* Top Header */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <div className="flex items-center gap-2">
            <Target className="h-7 w-7 text-indigo-600 dark:text-indigo-400" />
            <h1 className="text-2xl font-bold text-gray-900 dark:text-white">
              Gestão de Leads & Prospecção
            </h1>
          </div>
          <p className="text-sm text-gray-500 dark:text-gray-400">
            Captação de oportunidades topo de funil, régua de qualificação e conversão ágil
          </p>
        </div>

        <button
          onClick={() => {
            setLeadToEdit(null);
            setIsLeadModalOpen(true);
          }}
          className="inline-flex items-center gap-1.5 rounded-lg bg-indigo-600 px-4 py-2 text-xs font-semibold text-white shadow-sm hover:bg-indigo-700"
        >
          <Plus className="h-4 w-4" /> Novo Lead
        </button>
      </div>

      {/* Status Funnel Tabs */}
      <div className="flex border-b border-gray-200 dark:border-gray-700 overflow-x-auto">
        {[
          { key: 'ALL', label: 'Todos os Leads' },
          { key: 'NEW', label: 'Novos' },
          { key: 'IN_QUALIFICATION', label: 'Em Qualificação' },
          { key: 'QUALIFIED', label: 'Qualificados' },
          { key: 'CONVERTED', label: 'Convertidos' },
          { key: 'DISQUALIFIED', label: 'Desqualificados' },
        ].map((tab) => (
          <button
            key={tab.key}
            onClick={() => setActiveTab(tab.key)}
            className={`border-b-2 py-3 px-4 text-xs font-semibold whitespace-nowrap transition-colors ${
              activeTab === tab.key
                ? 'border-indigo-600 text-indigo-600 dark:text-indigo-400'
                : 'border-transparent text-gray-500 hover:text-gray-700 dark:text-gray-400'
            }`}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {/* Filter and Search Bar */}
      <div className="flex flex-col md:flex-row items-center gap-3 rounded-xl border border-gray-200 bg-white p-4 shadow-sm dark:border-gray-700 dark:bg-gray-800">
        <div className="relative flex-1 w-full">
          <Search className="absolute left-3 top-2.5 h-4 w-4 text-gray-400" />
          <input
            type="text"
            placeholder="Buscar por contato, empresa ou e-mail..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full rounded-lg border border-gray-300 pl-9 pr-4 py-2 text-xs dark:border-gray-600 dark:bg-gray-700 dark:text-white"
          />
        </div>

        <select
          value={selectedSource}
          onChange={(e) => setSelectedSource(e.target.value)}
          className="rounded-lg border border-gray-300 px-3 py-2 text-xs dark:border-gray-600 dark:bg-gray-700 dark:text-white"
        >
          <option value="">Todos os Canais de Origem</option>
          <option value="WEBSITE">Site / Formulário</option>
          <option value="INBOUND">Inbound / Contato Direto</option>
          <option value="OUTBOUND">Outbound / Ativo</option>
          <option value="REFERRAL">Indicação</option>
          <option value="EVENT">Eventos</option>
          <option value="SOCIAL_MEDIA">Redes Sociais</option>
          <option value="OTHER">Outros</option>
        </select>
      </div>

      {/* Table */}
      <div className="overflow-hidden rounded-xl border border-gray-200 bg-white shadow-sm dark:border-gray-700 dark:bg-gray-800">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead className="border-b border-gray-200 bg-gray-50 uppercase tracking-wider text-gray-500 dark:border-gray-700 dark:bg-gray-850 dark:text-gray-400">
              <tr>
                <th className="px-4 py-3">Contato / Empresa</th>
                <th className="px-4 py-3">Comunicação</th>
                <th className="px-4 py-3">Origem</th>
                <th className="px-4 py-3">Status</th>
                <th className="px-4 py-3">Orçamento Estimado</th>
                <th className="px-4 py-3 text-right">Ações & Conversão</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-200 dark:divide-gray-700">
              {filteredLeads.map((l) => (
                <tr key={l.id} className="hover:bg-gray-50 dark:hover:bg-gray-750 transition-colors">
                  <td className="px-4 py-3">
                    <div className="font-semibold text-gray-900 dark:text-white">{l.name}</div>
                    {l.companyName && (
                      <div className="flex items-center gap-1 text-[11px] text-gray-500 dark:text-gray-400">
                        <Building className="h-3 w-3" /> {l.companyName}
                      </div>
                    )}
                  </td>
                  <td className="px-4 py-3 text-gray-600 dark:text-gray-300">
                    {l.email && (
                      <div className="flex items-center gap-1">
                        <Mail className="h-3 w-3 text-gray-400" /> {l.email}
                      </div>
                    )}
                    {l.phone && (
                      <div className="flex items-center gap-1 text-gray-500">
                        <Phone className="h-3 w-3 text-gray-400" /> {l.phone}
                      </div>
                    )}
                  </td>
                  <td className="px-4 py-3 whitespace-nowrap">
                    <span className="rounded bg-gray-100 px-2 py-0.5 text-[10px] font-medium text-gray-700 dark:bg-gray-700 dark:text-gray-300">
                      {l.source}
                    </span>
                  </td>
                  <td className="px-4 py-3 whitespace-nowrap">
                    {getStatusBadge(l.status)}
                    {l.disqualificationReason && (
                      <div className="text-[10px] text-rose-500 mt-0.5 truncate max-w-xs" title={l.disqualificationReason}>
                        Motivo: {l.disqualificationReason}
                      </div>
                    )}
                  </td>
                  <td className="px-4 py-3 font-mono font-medium text-emerald-600 dark:text-emerald-400 whitespace-nowrap">
                    R$ {Number(l.estimatedBudget || 0).toLocaleString('pt-BR', { minimumFractionDigits: 2 })}
                  </td>
                  <td className="px-4 py-3 text-right whitespace-nowrap">
                    <div className="flex items-center justify-end gap-1.5">
                      {/* Qualification status transitions */}
                      {l.status === 'NEW' && (
                        <button
                          onClick={() => handleStatusChange(l, 'IN_QUALIFICATION')}
                          className="rounded bg-amber-50 px-2 py-1 text-[11px] font-medium text-amber-700 hover:bg-amber-100 dark:bg-amber-900/30 dark:text-amber-300"
                        >
                          Qualificar
                        </button>
                      )}
                      {l.status === 'IN_QUALIFICATION' && (
                        <button
                          onClick={() => handleStatusChange(l, 'QUALIFIED')}
                          className="rounded bg-emerald-50 px-2 py-1 text-[11px] font-medium text-emerald-700 hover:bg-emerald-100 dark:bg-emerald-900/30 dark:text-emerald-300"
                        >
                          Marcar Qualificado
                        </button>
                      )}

                      {/* Convert 1-click */}
                      {l.status !== 'CONVERTED' && l.status !== 'DISQUALIFIED' && (
                        <button
                          onClick={() => handleOpenConvert(l)}
                          className="inline-flex items-center gap-1 rounded bg-indigo-600 px-2.5 py-1 text-[11px] font-semibold text-white hover:bg-indigo-700 shadow-sm"
                          title="Converter em Cliente e Oportunidade"
                        >
                          <ArrowRight className="h-3 w-3" /> Converter
                        </button>
                      )}

                      {/* Disqualify button */}
                      {l.status !== 'CONVERTED' && l.status !== 'DISQUALIFIED' && (
                        <button
                          onClick={() => handleOpenDisqualify(l)}
                          className="rounded p-1 text-rose-500 hover:bg-rose-50 dark:hover:bg-rose-900/20"
                          title="Desqualificar"
                        >
                          <XCircle className="h-4 w-4" />
                        </button>
                      )}

                      {/* Edit */}
                      <button
                        onClick={() => {
                          setLeadToEdit(l);
                          setIsLeadModalOpen(true);
                        }}
                        className="rounded p-1 text-gray-500 hover:bg-gray-100 hover:text-indigo-600 dark:hover:bg-gray-700"
                        title="Editar"
                      >
                        <Edit className="h-4 w-4" />
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
              {filteredLeads.length === 0 && !loading && (
                <tr>
                  <td colSpan={6} className="py-8 text-center text-gray-500 dark:text-gray-400">
                    Nenhum lead encontrado para este filtro.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Modals */}
      <LeadFormModal
        isOpen={isLeadModalOpen}
        onClose={() => setIsLeadModalOpen(false)}
        leadToEdit={leadToEdit}
        onSaved={fetchLeads}
      />

      <ConvertLeadModal
        isOpen={isConvertModalOpen}
        onClose={() => setIsConvertModalOpen(false)}
        lead={leadToConvert}
        onConverted={fetchLeads}
      />

      {/* Disqualify Confirmation Modal */}
      {isDisqualifyModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
          <div className="w-full max-w-md rounded-xl bg-white p-6 shadow-2xl dark:bg-gray-800">
            <h3 className="text-base font-bold text-gray-900 dark:text-white">
              Desqualificar Lead: {leadToDisqualify?.name}
            </h3>
            <p className="mt-1 text-xs text-gray-500 dark:text-gray-400">
              Informe o motivo da desqualificação para alimentar a inteligência comercial:
            </p>

            <textarea
              rows={3}
              required
              placeholder="Ex: Sem orçamento no momento, fechou com concorrente, etc."
              value={disqualificationReason}
              onChange={(e) => setDisqualificationReason(e.target.value)}
              className="mt-3 w-full rounded-md border border-gray-300 p-2 text-xs dark:border-gray-600 dark:bg-gray-700 dark:text-white"
            />

            <div className="mt-4 flex justify-end gap-2">
              <button
                onClick={() => setIsDisqualifyModalOpen(false)}
                className="rounded-lg bg-gray-100 px-3 py-1.5 text-xs font-medium text-gray-700 hover:bg-gray-200 dark:bg-gray-700 dark:text-gray-300"
              >
                Cancelar
              </button>
              <button
                disabled={!disqualificationReason.trim()}
                onClick={handleConfirmDisqualify}
                className="rounded-lg bg-rose-600 px-3 py-1.5 text-xs font-semibold text-white hover:bg-rose-700 disabled:opacity-50"
              >
                Confirmar Desqualificação
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
