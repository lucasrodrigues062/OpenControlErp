import React, { useState, useRef, useEffect, useCallback } from 'react';
import { Deal, PipelineStage, SalesPipeline } from '../../types/crm';
import { KanbanColumn } from './KanbanColumn';
import { DealLossReasonModal } from './DealLossReasonModal';
import { crmApi } from '../../services/crmApi';
import { Plus, ChevronLeft, ChevronRight, Layers, ArrowRight } from 'lucide-react';

interface KanbanBoardProps {
  pipeline: SalesPipeline;
  deals: Deal[];
  onDealClick: (deal: Deal) => void;
  onQuickAddDeal: (stageId: string) => void;
  onRefresh: () => void;
  onEditPipeline?: () => void;
  onEditStage?: (stage: PipelineStage) => void;
}

export const KanbanBoard: React.FC<KanbanBoardProps> = ({
  pipeline,
  deals,
  onDealClick,
  onQuickAddDeal,
  onRefresh,
  onEditPipeline,
  onEditStage,
}) => {
  const [dealPendingLoss, setDealPendingLoss] = useState<{ deal: Deal; targetStage: PipelineStage } | null>(null);
  const scrollContainerRef = useRef<HTMLDivElement>(null);
  const [canScrollLeft, setCanScrollLeft] = useState(false);
  const [canScrollRight, setCanScrollRight] = useState(false);
  const [activeStageId, setActiveStageId] = useState<string | null>(null);

  const sortedStages = [...pipeline.stages].sort((a, b) => a.orderIndex - b.orderIndex);

  const checkScroll = useCallback(() => {
    const el = scrollContainerRef.current;
    if (!el) return;
    const { scrollLeft, scrollWidth, clientWidth } = el;
    setCanScrollLeft(scrollLeft > 15);
    setCanScrollRight(scrollLeft + clientWidth < scrollWidth - 15);
  }, []);

  useEffect(() => {
    const el = scrollContainerRef.current;
    if (!el) return;

    checkScroll();

    el.addEventListener('scroll', checkScroll, { passive: true });
    window.addEventListener('resize', checkScroll);

    const observer = new ResizeObserver(() => {
      checkScroll();
    });
    observer.observe(el);

    return () => {
      el.removeEventListener('scroll', checkScroll);
      window.removeEventListener('resize', checkScroll);
      observer.disconnect();
    };
  }, [checkScroll, sortedStages.length, deals.length]);

  const scrollBy = (offset: number) => {
    if (scrollContainerRef.current) {
      scrollContainerRef.current.scrollBy({ left: offset, behavior: 'smooth' });
    }
  };

  const scrollToStage = (stageId: string) => {
    setActiveStageId(stageId);
    const target = document.getElementById(`kanban-stage-${stageId}`);
    if (target) {
      target.scrollIntoView({ behavior: 'smooth', block: 'nearest', inline: 'center' });
    }
  };

  const handleDropDeal = async (dealId: string, targetStage: PipelineStage) => {
    const deal = deals.find((d) => d.id === dealId);
    if (!deal) return;
    if (deal.stageId === targetStage.id) return;

    if (targetStage.stageType === 'LOST') {
      setDealPendingLoss({ deal, targetStage });
      return;
    }

    try {
      await crmApi.updateDealStage(dealId, targetStage.id);
      onRefresh();
    } catch (err) {
      console.error('Error updating deal stage:', err);
      onRefresh();
    }
  };

  const handleConfirmLoss = async (reason: string) => {
    if (!dealPendingLoss) return;
    try {
      await crmApi.updateDealStage(dealPendingLoss.deal.id, dealPendingLoss.targetStage.id, reason);
      setDealPendingLoss(null);
      onRefresh();
    } catch (err) {
      console.error('Error marking deal as lost:', err);
    }
  };

  return (
    <div className="flex-1 min-h-0 flex flex-col space-y-2 overflow-hidden">
      {/* Stage Navigator / Minimap Ribbon (Fixed shrink-0) */}
      <div className="shrink-0 flex flex-wrap items-center justify-between gap-2 rounded-xl border border-gray-200/90 bg-white/80 p-2 shadow-sm backdrop-blur-sm dark:border-gray-800 dark:bg-gray-850">
        <div className="flex items-center gap-1.5 text-xs font-semibold text-gray-700 dark:text-gray-300">
          <Layers className="h-4 w-4 text-indigo-500 shrink-0" />
          <span className="hidden sm:inline">Etapas do Funil ({sortedStages.length}):</span>
        </div>

        {/* Stage quick-nav pill buttons */}
        <div className="flex flex-1 items-center gap-1.5 overflow-x-auto py-0.5 px-1 scrollbar-none">
          {sortedStages.map((stage) => {
            const count = deals.filter((d) => d.stageId === stage.id).length;
            const isActive = activeStageId === stage.id;
            return (
              <button
                key={stage.id}
                onClick={() => scrollToStage(stage.id)}
                title={`Ir para etapa: ${stage.name}`}
                className={`group flex items-center gap-1.5 rounded-lg border px-2.5 py-1 text-xs font-medium transition-all shrink-0 ${
                  isActive
                    ? 'border-indigo-500 bg-indigo-50 text-indigo-700 shadow-sm dark:bg-indigo-950/60 dark:text-indigo-300'
                    : 'border-gray-200 bg-gray-50/70 text-gray-600 hover:border-gray-300 hover:bg-gray-100 dark:border-gray-750 dark:bg-gray-800 dark:text-gray-300 dark:hover:bg-gray-750'
                }`}
              >
                <span
                  className="h-2 w-2 rounded-full shrink-0 transition-transform group-hover:scale-125"
                  style={{ backgroundColor: stage.colorHex }}
                />
                <span className="truncate max-w-[120px]">{stage.name}</span>
                <span
                  className={`rounded-full px-1.5 py-0.2 text-[10px] font-bold ${
                    count > 0
                      ? 'bg-indigo-100 text-indigo-700 dark:bg-indigo-900/60 dark:text-indigo-300'
                      : 'bg-gray-200 text-gray-500 dark:bg-gray-700 dark:text-gray-400'
                  }`}
                >
                  {count}
                </span>
              </button>
            );
          })}
        </div>

        {/* Scroll status indicator / action */}
        {canScrollRight && (
          <button
            onClick={() => scrollBy(340)}
            className="flex items-center gap-1 rounded-lg bg-indigo-50 px-2.5 py-1 text-xs font-semibold text-indigo-600 hover:bg-indigo-100 dark:bg-indigo-950/60 dark:text-indigo-400 shrink-0 animate-pulse"
            title="Existem mais colunas à direita. Clique para rolar."
          >
            <span>Mais colunas</span>
            <ArrowRight className="h-3.5 w-3.5" />
          </button>
        )}
      </div>

      {/* Main Kanban Columns Container with Floating Nav Arrows */}
      <div className="relative flex-1 min-h-0 flex flex-col overflow-hidden group/board">
        {/* Left Floating Nav Button & Gradient Shadow */}
        {canScrollLeft && (
          <div className="absolute left-0 top-0 bottom-3 z-20 flex items-center pl-1 pr-8 bg-gradient-to-r from-slate-50 via-slate-50/80 to-transparent dark:from-slate-950 dark:via-slate-950/80 pointer-events-none transition-opacity duration-200">
            <button
              onClick={() => scrollBy(-320)}
              className="pointer-events-auto flex h-10 w-10 items-center justify-center rounded-full border border-gray-200 bg-white shadow-lg text-gray-700 hover:bg-gray-50 hover:text-indigo-600 dark:border-gray-700 dark:bg-gray-800 dark:text-gray-200 dark:hover:text-indigo-400 transition-transform hover:scale-110 active:scale-95"
              title="Rolar para a esquerda"
              aria-label="Rolar para a esquerda"
            >
              <ChevronLeft className="h-6 w-6" />
            </button>
          </div>
        )}

        {/* Right Floating Nav Button & Gradient Shadow */}
        {canScrollRight && (
          <div className="absolute right-0 top-0 bottom-3 z-20 flex items-center pr-1 pl-8 bg-gradient-to-l from-slate-50 via-slate-50/80 to-transparent dark:from-slate-950 dark:via-slate-950/80 pointer-events-none transition-opacity duration-200">
            <button
              onClick={() => scrollBy(320)}
              className="pointer-events-auto flex h-10 w-10 items-center justify-center rounded-full border border-indigo-200 bg-white shadow-lg text-indigo-600 hover:bg-indigo-50 dark:border-indigo-800 dark:bg-gray-800 dark:text-indigo-400 dark:hover:bg-gray-750 transition-transform hover:scale-110 active:scale-95 ring-2 ring-indigo-400/20"
              title="Rolar para a direita (ver mais etapas)"
              aria-label="Rolar para a direita"
            >
              <ChevronRight className="h-6 w-6" />
            </button>
          </div>
        )}

        {/* Scrollable Columns Row */}
        <div
          ref={scrollContainerRef}
          className="flex-1 min-h-0 flex gap-3.5 overflow-x-auto overflow-y-hidden pb-2 pt-0.5 items-stretch scroll-smooth"
          style={{
            scrollbarWidth: 'thin',
          }}
        >
          {sortedStages.map((stage) => {
            const stageDeals = deals.filter((d) => d.stageId === stage.id);
            return (
              <KanbanColumn
                key={stage.id}
                stage={stage}
                deals={stageDeals}
                onDealClick={onDealClick}
                onDropDeal={handleDropDeal}
                onQuickAddDeal={onQuickAddDeal}
                onEditStage={onEditStage}
              />
            );
          })}

          {/* Add Stage Column Placeholder Button */}
          {onEditPipeline && (
            <div className="flex h-full min-h-[200px] w-[240px] min-w-[240px] shrink-0 flex-col items-center justify-center rounded-xl border-2 border-dashed border-gray-300 bg-gray-50/50 p-6 text-center hover:border-indigo-400 hover:bg-indigo-50/30 dark:border-gray-750 dark:bg-gray-850/50 dark:hover:border-indigo-500 transition">
              <button
                onClick={onEditPipeline}
                className="flex flex-col items-center gap-2 text-xs font-semibold text-gray-500 hover:text-indigo-600 dark:text-gray-400 dark:hover:text-indigo-400"
              >
                <div className="flex h-10 w-10 items-center justify-center rounded-full bg-indigo-50 text-indigo-600 dark:bg-indigo-950/60 dark:text-indigo-400 shadow-sm">
                  <Plus className="h-5 w-5" />
                </div>
                <span>Adicionar Nova Etapa</span>
              </button>
            </div>
          )}
        </div>
      </div>

      {/* Loss Reason Interception Modal */}
      <DealLossReasonModal
        isOpen={!!dealPendingLoss}
        onClose={() => setDealPendingLoss(null)}
        deal={dealPendingLoss?.deal || null}
        onConfirm={handleConfirmLoss}
      />
    </div>
  );
};
