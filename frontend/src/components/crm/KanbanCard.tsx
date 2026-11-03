import React from 'react';
import { Deal, PipelineStage } from '../../types/crm';
import { Building2, AlertTriangle, Clock, Calendar, CheckCircle2, XCircle, User } from 'lucide-react';

interface KanbanCardProps {
  deal: Deal;
  stage: PipelineStage;
  onClick: (deal: Deal) => void;
  onDragStart: (e: React.DragEvent, deal: Deal) => void;
}

export const KanbanCard: React.FC<KanbanCardProps> = ({
  deal,
  stage,
  onClick,
  onDragStart,
}) => {
  // Calculate days in stage
  const updatedDate = new Date(deal.updatedAt || deal.createdAt);
  const diffDays = Math.max(0, Math.floor((Date.now() - updatedDate.getTime()) / (1000 * 60 * 60 * 24)));
  const isStagnant = stage.maxStagnantDays > 0 && diffDays >= stage.maxStagnantDays && deal.status === 'OPEN';

  // Calculate overdue expected date
  let isOverdue = false;
  let formattedCloseDate = '';
  if (deal.expectedCloseDate) {
    const closeDate = new Date(`${deal.expectedCloseDate}T23:59:59`);
    isOverdue = closeDate.getTime() < Date.now() && deal.status === 'OPEN';
    formattedCloseDate = new Date(`${deal.expectedCloseDate}T00:00:00`).toLocaleDateString('pt-BR');
  }

  // Weighted value calculation
  const rawValue = Number(deal.estimatedValue || 0);
  const weightedValue = rawValue * ((stage.winProbabilityPct || 0) / 100);

  return (
    <div
      draggable
      onDragStart={(e) => onDragStart(e, deal)}
      onClick={() => onClick(deal)}
      className={`group relative cursor-grab rounded-lg border bg-white p-3 shadow-sm transition hover:shadow-md active:cursor-grabbing dark:bg-gray-800 ${
        isStagnant
          ? 'border-amber-300 dark:border-amber-700/60 bg-amber-50/15'
          : 'border-gray-200 dark:border-gray-700 hover:border-indigo-400 dark:hover:border-indigo-500'
      }`}
    >
      {/* Top badges: Status / SLA warning */}
      <div className="flex items-start justify-between gap-2">
        <h4 className="text-xs font-bold text-gray-900 group-hover:text-indigo-600 dark:text-white dark:group-hover:text-indigo-400 line-clamp-2">
          {deal.title}
        </h4>

        {deal.status === 'WON' && (
          <span className="flex items-center gap-1 rounded bg-emerald-100 px-1.5 py-0.5 text-[10px] font-bold text-emerald-800 dark:bg-emerald-950/60 dark:text-emerald-300 shrink-0">
            <CheckCircle2 className="h-3 w-3" /> Ganho
          </span>
        )}
        {deal.status === 'LOST' && (
          <span className="flex items-center gap-1 rounded bg-red-100 px-1.5 py-0.5 text-[10px] font-bold text-red-800 dark:bg-red-950/60 dark:text-red-300 shrink-0">
            <XCircle className="h-3 w-3" /> Perdido
          </span>
        )}
        {deal.status === 'OPEN' && isStagnant && (
          <span
            className="flex items-center gap-1 rounded bg-amber-100 px-1.5 py-0.5 text-[10px] font-semibold text-amber-800 dark:bg-amber-950/60 dark:text-amber-300 shrink-0"
            title={`Parado há ${diffDays} dias nesta etapa (limite SLA: ${stage.maxStagnantDays} dias)`}
          >
            <AlertTriangle className="h-3 w-3" /> {diffDays}d (SLA)
          </span>
        )}
      </div>

      {/* Customer Name */}
      {deal.customerName && (
        <div className="mt-1.5 flex items-center gap-1 text-[11px] font-medium text-gray-600 dark:text-gray-300">
          <Building2 className="h-3 w-3 shrink-0 text-gray-400" />
          <span className="truncate">{deal.customerName}</span>
        </div>
      )}

      {/* Expected close date & SLA metadata chips */}
      <div className="mt-2 flex flex-wrap items-center gap-1.5 text-[10px]">
        {formattedCloseDate && (
          <span
            className={`inline-flex items-center gap-1 rounded px-1.5 py-0.5 font-medium ${
              isOverdue
                ? 'bg-red-100 text-red-700 dark:bg-red-950/50 dark:text-red-300 font-semibold'
                : 'bg-gray-100 text-gray-600 dark:bg-gray-700 dark:text-gray-300'
            }`}
            title={isOverdue ? 'Data prevista de fechamento vencida' : 'Previsão de fechamento'}
          >
            <Calendar className="h-3 w-3" />
            <span>{formattedCloseDate}</span>
          </span>
        )}

        <span
          className="inline-flex items-center gap-1 rounded bg-gray-100 px-1.5 py-0.5 font-mono text-gray-500 dark:bg-gray-700 dark:text-gray-400"
          title="Tempo nesta etapa"
        >
          <Clock className="h-2.5 w-2.5" />
          <span>{diffDays}d</span>
        </span>

        {deal.assignedUserName && (
          <span
            className="inline-flex items-center gap-1 rounded bg-indigo-50 px-1.5 py-0.5 text-indigo-700 dark:bg-indigo-950/40 dark:text-indigo-300 truncate max-w-[120px]"
            title={`Responsável: ${deal.assignedUserName}`}
          >
            <User className="h-2.5 w-2.5 shrink-0" />
            <span className="truncate">{deal.assignedUserName}</span>
          </span>
        )}
      </div>

      {/* Footer: Values */}
      <div className="mt-3 flex items-center justify-between border-t border-gray-100 pt-2 dark:border-gray-750">
        <div>
          <span className="text-xs font-bold text-emerald-600 dark:text-emerald-400 font-mono">
            R$ {rawValue.toLocaleString('pt-BR', { minimumFractionDigits: 2 })}
          </span>
          {stage.winProbabilityPct > 0 && stage.winProbabilityPct < 100 && deal.status === 'OPEN' && (
            <span
              className="ml-1.5 text-[10px] text-gray-400 dark:text-gray-500 font-mono"
              title={`Valor ponderado (${stage.winProbabilityPct}%)`}
            >
              (Pond: R$ {weightedValue.toLocaleString('pt-BR', { minimumFractionDigits: 0 })})
            </span>
          )}
        </div>

        <span
          className="rounded px-1 text-[9px] font-bold text-white font-mono"
          style={{ backgroundColor: stage.colorHex }}
          title={`Probabilidade da etapa: ${stage.winProbabilityPct}%`}
        >
          {stage.winProbabilityPct}%
        </span>
      </div>
    </div>
  );
};
