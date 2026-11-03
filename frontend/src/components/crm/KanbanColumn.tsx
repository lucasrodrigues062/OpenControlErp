import React, { useState } from 'react';
import { Deal, PipelineStage } from '../../types/crm';
import { KanbanCard } from './KanbanCard';
import { Plus, SlidersHorizontal } from 'lucide-react';

interface KanbanColumnProps {
  stage: PipelineStage;
  deals: Deal[];
  onDealClick: (deal: Deal) => void;
  onDropDeal: (dealId: string, targetStage: PipelineStage) => void;
  onQuickAddDeal: (stageId: string) => void;
  onEditStage?: (stage: PipelineStage) => void;
}

export const KanbanColumn: React.FC<KanbanColumnProps> = ({
  stage,
  deals,
  onDealClick,
  onDropDeal,
  onQuickAddDeal,
  onEditStage,
}) => {
  const [isDragOver, setIsDragOver] = useState(false);

  const totalValue = deals.reduce((acc, d) => acc + Number(d.estimatedValue || 0), 0);

  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragOver(true);
  };

  const handleDragLeave = () => {
    setIsDragOver(false);
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragOver(false);
    const dealId = e.dataTransfer.getData('dealId');
    if (dealId) {
      onDropDeal(dealId, stage);
    }
  };

  const handleDragStart = (e: React.DragEvent, deal: Deal) => {
    e.dataTransfer.setData('dealId', deal.id);
  };

  return (
    <div
      id={`kanban-stage-${stage.id}`}
      onDragOver={handleDragOver}
      onDragLeave={handleDragLeave}
      onDrop={handleDrop}
      className={`flex h-full w-[285px] min-w-[285px] max-w-[285px] shrink-0 flex-col rounded-xl border bg-gray-50/90 p-3 transition-colors dark:bg-gray-850 shadow-sm ${
        isDragOver
          ? 'border-indigo-500 bg-indigo-50/40 ring-2 ring-indigo-400/20 dark:border-indigo-400 dark:bg-indigo-950/20'
          : 'border-gray-200 dark:border-gray-800'
      }`}
    >
      {/* Column Header */}
      <div className="shrink-0 mb-2.5 flex items-center justify-between border-b border-gray-200 pb-2 dark:border-gray-700">
        <div className="flex items-center gap-2">
          <span
            className="h-3 w-3 rounded-full shrink-0"
            style={{ backgroundColor: stage.colorHex }}
          />
          <h3 className="text-xs font-bold text-gray-900 dark:text-white truncate max-w-[130px]" title={stage.name}>
            {stage.name}
          </h3>
          <span className="rounded-full bg-gray-200 px-1.5 py-0.2 text-[10px] font-semibold text-gray-700 dark:bg-gray-700 dark:text-gray-300">
            {deals.length}
          </span>
        </div>

        <div className="flex items-center gap-1.5">
          <span className="text-[10px] text-gray-500 dark:text-gray-400 font-mono font-medium">
            {stage.winProbabilityPct}%
          </span>
          {onEditStage && (
            <button
              onClick={() => onEditStage(stage)}
              title="Configurar esta etapa"
              className="rounded p-1 text-gray-400 hover:bg-gray-200 hover:text-gray-600 dark:hover:bg-gray-700 dark:hover:text-gray-300 transition"
            >
              <SlidersHorizontal className="h-3 w-3" />
            </button>
          )}
        </div>
      </div>

      {/* Column Sub-Header: Total Value */}
      <div className="shrink-0 mb-2 text-[11px] font-bold text-gray-600 dark:text-gray-300 font-mono">
        R$ {totalValue.toLocaleString('pt-BR', { minimumFractionDigits: 2 })}
      </div>

      {/* Cards List */}
      <div className="flex-1 min-h-0 space-y-2 overflow-y-auto pr-1">
        {deals.map((deal) => (
          <KanbanCard
            key={deal.id}
            deal={deal}
            stage={stage}
            onClick={onDealClick}
            onDragStart={handleDragStart}
          />
        ))}

        {deals.length === 0 && (
          <div className="flex h-24 items-center justify-center rounded-lg border border-dashed border-gray-300 text-[11px] text-gray-400 dark:border-gray-700">
            Nenhum negócio aqui
          </div>
        )}
      </div>

      {/* Quick Add Button */}
      {stage.stageType === 'IN_PROGRESS' && (
        <button
          onClick={() => onQuickAddDeal(stage.id)}
          className="shrink-0 mt-2 flex items-center justify-center gap-1 rounded-lg border border-dashed border-gray-300 py-1.5 text-[11px] font-medium text-gray-500 hover:border-indigo-400 hover:text-indigo-600 dark:border-gray-700 dark:text-gray-400 dark:hover:border-indigo-400 dark:hover:text-indigo-400 transition"
        >
          <Plus className="h-3.5 w-3.5" /> Adicionar Oportunidade
        </button>
      )}
    </div>
  );
};
