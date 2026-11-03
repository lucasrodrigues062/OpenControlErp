import React, { useState, useEffect } from 'react';
import { SalesPipeline, Deal, PipelineMetrics } from '../../types/crm';
import { crmApi } from '../../services/crmApi';
import { KanbanBoard } from '../../components/crm/KanbanBoard';
import { DealFormModal } from '../../components/crm/DealFormModal';
import { PipelineModal } from '../../components/crm/PipelineModal';
import { PipelineSettingsModal } from '../../components/crm/PipelineSettingsModal';
import {
  Trello,
  List,
  Plus,
  Search,
  DollarSign,
  TrendingUp,
  Percent,
  Briefcase,
  CheckCircle,
  XCircle,
  GitBranch,
  Settings,
  ChevronDown,
  ChevronUp,
} from 'lucide-react';

export const SalesPipelinePage: React.FC = () => {
  const [pipelines, setPipelines] = useState<SalesPipeline[]>([]);
  const [selectedPipelineId, setSelectedPipelineId] = useState<string>('');
  const [deals, setDeals] = useState<Deal[]>([]);
  const [metrics, setMetrics] = useState<PipelineMetrics | null>(null);
  const [viewMode, setViewMode] = useState<'kanban' | 'list'>('kanban');
  const [searchQuery, setSearchQuery] = useState('');
  const [showMetrics, setShowMetrics] = useState(true);

  // Modals
  const [isDealModalOpen, setIsDealModalOpen] = useState(false);
  const [dealToEdit, setDealToEdit] = useState<Deal | null>(null);
  const [defaultStageId, setDefaultStageId] = useState<string | undefined>(undefined);
  const [isPipelineModalOpen, setIsPipelineModalOpen] = useState(false);
  const [isSettingsModalOpen, setIsSettingsModalOpen] = useState(false);

  const fetchPipelines = async () => {
    try {
      const data = await crmApi.listPipelines();
      setPipelines(data);
      if (data.length > 0) {
        if (!selectedPipelineId || !data.some((p) => p.id === selectedPipelineId)) {
          const def = data.find((p) => p.isDefault) || data[0];
          setSelectedPipelineId(def.id);
        }
      }
    } catch (err) {
      console.error('Error fetching pipelines:', err);
    }
  };

  const fetchDealsAndMetrics = async () => {
    if (!selectedPipelineId) return;
    try {
      const [dealsData, metricsData] = await Promise.all([
        crmApi.listDeals({ pipelineId: selectedPipelineId }),
        crmApi.getPipelineMetrics(selectedPipelineId),
      ]);
      setDeals(dealsData);
      setMetrics(metricsData);
    } catch (err) {
      console.error('Error fetching deals or metrics:', err);
    }
  };

  useEffect(() => {
    fetchPipelines();
  }, []);

  useEffect(() => {
    if (selectedPipelineId) {
      fetchDealsAndMetrics();
    }
  }, [selectedPipelineId]);

  const activePipeline = pipelines.find((p) => p.id === selectedPipelineId);

  const filteredDeals = deals.filter((d) => {
    if (!searchQuery.trim()) return true;
    const q = searchQuery.toLowerCase();
    return (
      d.title.toLowerCase().includes(q) ||
      (d.customerName && d.customerName.toLowerCase().includes(q))
    );
  });

  const handleQuickAdd = (stageId: string) => {
    setDealToEdit(null);
    setDefaultStageId(stageId);
    setIsDealModalOpen(true);
  };

  const handleCardClick = (deal: Deal) => {
    setDealToEdit(deal);
    setDefaultStageId(deal.stageId);
    setIsDealModalOpen(true);
  };

  return (
    <div className="flex-1 min-h-0 flex flex-col space-y-2.5 w-full overflow-hidden">
      {/* Top Header */}
      <div className="shrink-0 flex flex-col md:flex-row md:items-center justify-between gap-3">
        <div>
          <div className="flex items-center gap-2">
            <Trello className="h-6 w-6 text-indigo-600 dark:text-indigo-400" />
            <h1 className="text-xl md:text-2xl font-bold text-gray-900 dark:text-white">
              Funil de Vendas & Oportunidades
            </h1>
          </div>
          <p className="text-xs md:text-sm text-gray-500 dark:text-gray-400">
            Pipeline visual de negociações comerciais e projeção de receita ponderada
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-2">
          {/* Pipeline Selector & Actions */}
          <div className="flex items-center gap-1.5">
            <select
              value={selectedPipelineId}
              onChange={(e) => setSelectedPipelineId(e.target.value)}
              className="rounded-lg border border-gray-300 px-3 py-1.5 text-xs font-semibold dark:border-gray-600 dark:bg-gray-800 dark:text-white shadow-sm"
            >
              {pipelines.map((p) => (
                <option key={p.id} value={p.id}>
                  {p.name} {p.isDefault ? '(Padrão)' : ''}
                </option>
              ))}
            </select>
            <button
              onClick={() => setIsPipelineModalOpen(true)}
              title="Criar novo funil de vendas"
              className="inline-flex items-center gap-1 rounded-lg border border-gray-300 bg-white px-2.5 py-1.5 text-xs font-medium text-gray-700 hover:bg-gray-50 dark:border-gray-600 dark:bg-gray-800 dark:text-gray-300 dark:hover:bg-gray-700 shadow-sm"
            >
              <GitBranch className="h-3.5 w-3.5" /> Novo Funil
            </button>
            <button
              onClick={() => setIsSettingsModalOpen(true)}
              title="Personalizar etapas, nomes, cores e SLAs deste funil"
              className="inline-flex items-center gap-1 rounded-lg border border-indigo-200 bg-indigo-50/60 px-2.5 py-1.5 text-xs font-semibold text-indigo-700 hover:bg-indigo-100 dark:border-indigo-800 dark:bg-indigo-950/40 dark:text-indigo-300 dark:hover:bg-indigo-900/60 shadow-sm"
            >
              <Settings className="h-3.5 w-3.5 text-indigo-600 dark:text-indigo-400" /> Configurar Funil
            </button>
          </div>

          {/* View mode toggle */}
          <div className="flex rounded-lg border border-gray-300 p-0.5 bg-gray-100 dark:border-gray-700 dark:bg-gray-800">
            <button
              onClick={() => setViewMode('kanban')}
              className={`flex items-center gap-1 rounded-md px-2.5 py-1 text-xs font-medium ${
                viewMode === 'kanban'
                  ? 'bg-white shadow text-indigo-600 dark:bg-gray-700 dark:text-white'
                  : 'text-gray-600 dark:text-gray-400'
              }`}
            >
              <Trello className="h-3.5 w-3.5" /> Kanban
            </button>
            <button
              onClick={() => setViewMode('list')}
              className={`flex items-center gap-1 rounded-md px-2.5 py-1 text-xs font-medium ${
                viewMode === 'list'
                  ? 'bg-white shadow text-indigo-600 dark:bg-gray-700 dark:text-white'
                  : 'text-gray-600 dark:text-gray-400'
              }`}
            >
              <List className="h-3.5 w-3.5" /> Lista
            </button>
          </div>

          <button
            onClick={() => setShowMetrics(!showMetrics)}
            title={showMetrics ? 'Recolher indicadores KPI' : 'Expandir indicadores KPI'}
            className="inline-flex items-center gap-1 rounded-lg border border-gray-300 bg-white px-2 py-1.5 text-xs font-medium text-gray-600 hover:bg-gray-50 dark:border-gray-600 dark:bg-gray-800 dark:text-gray-300 shadow-sm"
          >
            {showMetrics ? <ChevronUp className="h-3.5 w-3.5" /> : <ChevronDown className="h-3.5 w-3.5" />}
            <span className="hidden sm:inline">KPIs</span>
          </button>

          <button
            onClick={() => {
              setDealToEdit(null);
              setDefaultStageId(undefined);
              setIsDealModalOpen(true);
            }}
            className="inline-flex items-center gap-1.5 rounded-lg bg-indigo-600 px-3.5 py-1.5 text-xs font-semibold text-white shadow hover:bg-indigo-700 transition"
          >
            <Plus className="h-4 w-4" /> Nova Oportunidade
          </button>
        </div>
      </div>

      {/* KPI Metrics Ribbon - Compact & Clean & Collapsible */}
      {metrics && showMetrics && (
        <div className="shrink-0 grid grid-cols-2 md:grid-cols-4 gap-2.5 transition-all duration-200">
          <div className="rounded-xl border border-gray-200 bg-white p-2.5 shadow-sm dark:border-gray-700 dark:bg-gray-800">
            <div className="flex items-center justify-between">
              <span className="text-[11px] font-medium text-gray-500 dark:text-gray-400">Negócios Abertos</span>
              <Briefcase className="h-3.5 w-3.5 text-indigo-500" />
            </div>
            <div className="mt-0.5 text-lg font-bold text-gray-900 dark:text-white font-mono">
              {metrics.totalOpenDeals}
            </div>
          </div>

          <div className="rounded-xl border border-gray-200 bg-white p-2.5 shadow-sm dark:border-gray-700 dark:bg-gray-800">
            <div className="flex items-center justify-between">
              <span className="text-[11px] font-medium text-gray-500 dark:text-gray-400">Total do Funil</span>
              <DollarSign className="h-3.5 w-3.5 text-emerald-500" />
            </div>
            <div className="mt-0.5 text-lg font-bold text-emerald-600 dark:text-emerald-400 font-mono">
              R$ {Number(metrics.totalPipelineValue || 0).toLocaleString('pt-BR', { minimumFractionDigits: 2 })}
            </div>
          </div>

          <div className="rounded-xl border border-gray-200 bg-white p-2.5 shadow-sm dark:border-gray-700 dark:bg-gray-800">
            <div className="flex items-center justify-between">
              <span className="text-[11px] font-medium text-gray-500 dark:text-gray-400">Receita Ponderada</span>
              <TrendingUp className="h-3.5 w-3.5 text-purple-500" />
            </div>
            <div className="mt-0.5 text-lg font-bold text-purple-600 dark:text-purple-400 font-mono">
              R$ {Number(metrics.weightedPipelineValue || 0).toLocaleString('pt-BR', { minimumFractionDigits: 2 })}
            </div>
          </div>

          <div className="rounded-xl border border-gray-200 bg-white p-2.5 shadow-sm dark:border-gray-700 dark:bg-gray-800">
            <div className="flex items-center justify-between">
              <span className="text-[11px] font-medium text-gray-500 dark:text-gray-400">Taxa de Conversão</span>
              <Percent className="h-3.5 w-3.5 text-amber-500" />
            </div>
            <div className="mt-0.5 text-lg font-bold text-amber-600 dark:text-amber-400 font-mono">
              {Number(metrics.winRatePct || 0).toFixed(1)}%
            </div>
          </div>
        </div>
      )}

      {/* Search / Filter Bar */}
      <div className="shrink-0 flex items-center justify-between gap-4">
        <div className="relative w-full max-w-sm">
          <Search className="absolute left-3 top-1/2 h-3.5 w-3.5 -translate-y-1/2 text-gray-400" />
          <input
            type="text"
            placeholder="Buscar por oportunidade ou cliente..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full rounded-lg border border-gray-300 py-1.5 pl-8 pr-3 text-xs dark:border-gray-600 dark:bg-gray-800 dark:text-white"
          />
        </div>
      </div>

      {/* Main Board View: Kanban or List */}
      {viewMode === 'kanban' ? (
        activePipeline && activePipeline.stages && activePipeline.stages.length > 0 ? (
          <div className="flex-1 min-h-0 flex flex-col overflow-hidden">
            <KanbanBoard
              pipeline={activePipeline}
              deals={filteredDeals}
              onDealClick={handleCardClick}
              onQuickAddDeal={handleQuickAdd}
              onRefresh={fetchDealsAndMetrics}
              onEditPipeline={() => setIsSettingsModalOpen(true)}
              onEditStage={() => setIsSettingsModalOpen(true)}
            />
          </div>
        ) : (
          <div className="flex-1 flex flex-col items-center justify-center rounded-xl border border-dashed border-gray-300 p-8 text-center dark:border-gray-700">
            <Trello className="h-10 w-10 text-gray-400 mb-2" />
            <h3 className="text-sm font-semibold text-gray-700 dark:text-gray-300">
              Nenhuma etapa configurada neste funil
            </h3>
            <p className="mt-1 text-xs text-gray-500">
              Configure as etapas deste funil para começar a organizar suas oportunidades.
            </p>
            <button
              onClick={() => setIsSettingsModalOpen(true)}
              className="mt-4 rounded-lg bg-indigo-600 px-4 py-2 text-xs font-semibold text-white hover:bg-indigo-700"
            >
              Configurar Etapas do Funil
            </button>
          </div>
        )
      ) : (
        /* List View */
        <div className="rounded-xl border border-gray-200 bg-white shadow-sm dark:border-gray-700 dark:bg-gray-800 overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="border-b border-gray-200 bg-gray-50 dark:border-gray-750 dark:bg-gray-850">
                <tr>
                  <th className="px-4 py-3 font-semibold text-gray-600 dark:text-gray-300">Oportunidade</th>
                  <th className="px-4 py-3 font-semibold text-gray-600 dark:text-gray-300">Cliente</th>
                  <th className="px-4 py-3 font-semibold text-gray-600 dark:text-gray-300">Etapa</th>
                  <th className="px-4 py-3 font-semibold text-gray-600 dark:text-gray-300">Valor Estimado</th>
                  <th className="px-4 py-3 font-semibold text-gray-600 dark:text-gray-300">Status</th>
                  <th className="px-4 py-3 font-semibold text-gray-600 dark:text-gray-300">Previsão Fechamento</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-200 dark:divide-gray-700">
                {filteredDeals.map((deal) => (
                  <tr
                    key={deal.id}
                    onClick={() => handleCardClick(deal)}
                    className="hover:bg-gray-50 dark:hover:bg-gray-750 transition-colors cursor-pointer"
                  >
                    <td className="px-4 py-3 font-semibold text-gray-900 dark:text-white">
                      {deal.title}
                    </td>
                    <td className="px-4 py-3 text-gray-600 dark:text-gray-300">
                      {deal.customerName || 'Cliente não informado'}
                    </td>
                    <td className="px-4 py-3 whitespace-nowrap">
                      <span
                        className="inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-[11px] font-medium"
                        style={{
                          backgroundColor: deal.stageColorHex ? `${deal.stageColorHex}20` : '#E0E7FF',
                          color: deal.stageColorHex || '#3730A3',
                        }}
                      >
                        <span
                          className="h-1.5 w-1.5 rounded-full"
                          style={{ backgroundColor: deal.stageColorHex || '#4F46E5' }}
                        />
                        {deal.stageName}
                      </span>
                    </td>
                    <td className="px-4 py-3 font-mono font-bold text-emerald-600 dark:text-emerald-400 whitespace-nowrap">
                      R$ {Number(deal.estimatedValue || 0).toLocaleString('pt-BR', { minimumFractionDigits: 2 })}
                    </td>
                    <td className="px-4 py-3 whitespace-nowrap">
                      {deal.status === 'OPEN' && (
                        <span className="rounded-full bg-blue-100 px-2 py-0.5 text-[10px] font-semibold text-blue-800 dark:bg-blue-900/40 dark:text-blue-300">
                          Em Aberto
                        </span>
                      )}
                      {deal.status === 'WON' && (
                        <span className="inline-flex items-center gap-1 rounded-full bg-emerald-100 px-2 py-0.5 text-[10px] font-semibold text-emerald-800 dark:bg-emerald-900/40 dark:text-emerald-300">
                          <CheckCircle className="h-3 w-3" /> Ganho
                        </span>
                      )}
                      {deal.status === 'LOST' && (
                        <span className="inline-flex items-center gap-1 rounded-full bg-rose-100 px-2 py-0.5 text-[10px] font-semibold text-rose-800 dark:bg-rose-900/40 dark:text-rose-300">
                          <XCircle className="h-3 w-3" /> Perdido
                        </span>
                      )}
                    </td>
                    <td className="px-4 py-3 text-gray-500 dark:text-gray-400 whitespace-nowrap">
                      {deal.expectedCloseDate || 'Não definida'}
                    </td>
                  </tr>
                ))}
                {filteredDeals.length === 0 && (
                  <tr>
                    <td colSpan={6} className="py-8 text-center text-gray-500 dark:text-gray-400">
                      Nenhuma oportunidade cadastrada neste funil.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Deal Create / Edit Modal */}
      <DealFormModal
        isOpen={isDealModalOpen}
        onClose={() => setIsDealModalOpen(false)}
        dealToEdit={dealToEdit}
        defaultPipelineId={selectedPipelineId}
        defaultStageId={defaultStageId}
        pipelines={pipelines}
        onSaved={fetchDealsAndMetrics}
      />

      {/* Pipeline Create Modal */}
      <PipelineModal
        isOpen={isPipelineModalOpen}
        onClose={() => setIsPipelineModalOpen(false)}
        onSaved={async (newId) => {
          await fetchPipelines();
          setSelectedPipelineId(newId);
        }}
      />

      {/* Pipeline Settings & Columns Modal */}
      <PipelineSettingsModal
        isOpen={isSettingsModalOpen}
        onClose={() => setIsSettingsModalOpen(false)}
        pipeline={activePipeline || null}
        onPipelineUpdated={async () => {
          await fetchPipelines();
          await fetchDealsAndMetrics();
        }}
      />
    </div>
  );
};
