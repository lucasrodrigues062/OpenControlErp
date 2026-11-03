import React, { useState, useEffect } from 'react';
import { SalesPipeline, PipelineStage, StageType } from '../../types/crm';
import { crmApi } from '../../services/crmApi';
import {
  X,
  Plus,
  Trash2,
  ArrowUp,
  ArrowDown,
  Check,
  Settings,
  AlertCircle,
  HelpCircle,
} from 'lucide-react';

interface PipelineSettingsModalProps {
  isOpen: boolean;
  onClose: () => void;
  pipeline: SalesPipeline | null;
  onPipelineUpdated: () => void;
}

export const PipelineSettingsModal: React.FC<PipelineSettingsModalProps> = ({
  isOpen,
  onClose,
  pipeline,
  onPipelineUpdated,
}) => {
  const [pipelineName, setPipelineName] = useState('');
  const [isDefault, setIsDefault] = useState(false);
  const [stages, setStages] = useState<PipelineStage[]>([]);
  const [loading, setLoading] = useState(false);
  const [savingPipeline, setSavingPipeline] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  // New stage form state
  const [newStageName, setNewStageName] = useState('');
  const [newStageColor, setNewStageColor] = useState('#6366F1');
  const [newStageProb, setNewStageProb] = useState(20);
  const [newStageSla, setNewStageSla] = useState(15);
  const [newStageType, setNewStageType] = useState<StageType>('IN_PROGRESS');
  const [isAddingStage, setIsAddingStage] = useState(false);

  useEffect(() => {
    if (pipeline) {
      setPipelineName(pipeline.name);
      setIsDefault(pipeline.isDefault);
      setStages([...pipeline.stages].sort((a, b) => a.orderIndex - b.orderIndex));
      setErrorMessage(null);
      setSuccessMessage(null);
    }
  }, [pipeline, isOpen]);

  if (!isOpen || !pipeline) return null;

  const showFeedback = (msg: string, isError = false) => {
    if (isError) {
      setErrorMessage(msg);
      setSuccessMessage(null);
    } else {
      setSuccessMessage(msg);
      setErrorMessage(null);
      setTimeout(() => setSuccessMessage(null), 3000);
    }
  };

  const handleSavePipelineInfo = async (e: React.FormEvent) => {
    e.preventDefault();
    setSavingPipeline(true);
    setErrorMessage(null);
    try {
      await crmApi.updatePipeline(pipeline.id, {
        name: pipelineName.trim(),
        isDefault,
      });
      showFeedback('Configurações gerais do funil salvas com sucesso!');
      onPipelineUpdated();
    } catch (err: any) {
      console.error('Error updating pipeline:', err);
      showFeedback(err.response?.data?.detail || 'Erro ao atualizar informações do funil', true);
    } finally {
      setSavingPipeline(false);
    }
  };

  const handleUpdateStage = async (stage: PipelineStage) => {
    setLoading(true);
    try {
      await crmApi.updateStage(pipeline.id, stage.id, {
        name: stage.name,
        winProbabilityPct: stage.winProbabilityPct,
        colorHex: stage.colorHex,
        maxStagnantDays: stage.maxStagnantDays,
        stageType: stage.stageType,
      });
      showFeedback(`Etapa "${stage.name}" atualizada com sucesso!`);
      onPipelineUpdated();
    } catch (err: any) {
      console.error('Error updating stage:', err);
      showFeedback(err.response?.data?.detail || 'Erro ao salvar etapa', true);
    } finally {
      setLoading(false);
    }
  };

  const handleDeleteStage = async (stageId: string, stageName: string) => {
    if (!confirm(`Deseja realmente remover a etapa "${stageName}"?`)) return;
    setLoading(true);
    try {
      await crmApi.deleteStage(pipeline.id, stageId);
      setStages((prev) => prev.filter((s) => s.id !== stageId));
      showFeedback(`Etapa "${stageName}" excluída.`);
      onPipelineUpdated();
    } catch (err: any) {
      console.error('Error deleting stage:', err);
      const msg = err.response?.data?.detail || 'Não foi possível excluir a etapa. Verifique se existem oportunidades nela.';
      showFeedback(msg, true);
    } finally {
      setLoading(false);
    }
  };

  const handleMoveStage = async (index: number, direction: 'up' | 'down') => {
    const targetIndex = direction === 'up' ? index - 1 : index + 1;
    if (targetIndex < 0 || targetIndex >= stages.length) return;

    const newStages = [...stages];
    const [moved] = newStages.splice(index, 1);
    newStages.splice(targetIndex, 0, moved);

    // Update order indices locally
    const reordered = newStages.map((s, idx) => ({ ...s, orderIndex: idx }));
    setStages(reordered);

    // Save order on backend
    try {
      await crmApi.reorderStages(
        pipeline.id,
        reordered.map((s) => s.id)
      );
      onPipelineUpdated();
    } catch (err: any) {
      console.error('Error reordering stages:', err);
      showFeedback('Erro ao reordenar etapas', true);
    }
  };

  const handleAddStage = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newStageName.trim()) return;

    setLoading(true);
    try {
      const created = await crmApi.addStage(pipeline.id, {
        name: newStageName.trim(),
        colorHex: newStageColor,
        winProbabilityPct: Number(newStageProb),
        maxStagnantDays: Number(newStageSla),
        stageType: newStageType,
      });

      setStages((prev) => [...prev, created]);
      setNewStageName('');
      setNewStageProb(20);
      setNewStageSla(15);
      setIsAddingStage(false);
      showFeedback(`Etapa "${created.name}" criada com sucesso!`);
      onPipelineUpdated();
    } catch (err: any) {
      console.error('Error adding stage:', err);
      showFeedback(err.response?.data?.detail || 'Erro ao adicionar nova etapa', true);
    } finally {
      setLoading(false);
    }
  };

  const handleStageFieldChange = (index: number, field: keyof PipelineStage, value: any) => {
    setStages((prev) => {
      const copy = [...prev];
      copy[index] = { ...copy[index], [field]: value };
      return copy;
    });
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4 backdrop-blur-sm overflow-y-auto">
      <div className="w-full max-w-4xl rounded-xl bg-white shadow-xl dark:bg-gray-800 my-8 flex flex-col max-h-[90vh]">
        {/* Header */}
        <div className="flex items-center justify-between border-b border-gray-200 px-6 py-4 dark:border-gray-700">
          <div className="flex items-center gap-2">
            <Settings className="h-5 w-5 text-indigo-600 dark:text-indigo-400" />
            <h2 className="text-lg font-bold text-gray-900 dark:text-white">
              Personalizar Funil: {pipeline.name}
            </h2>
          </div>
          <button
            onClick={onClose}
            className="rounded-lg p-1 text-gray-400 hover:bg-gray-100 hover:text-gray-500 dark:hover:bg-gray-700"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        {/* Alerts */}
        {errorMessage && (
          <div className="mx-6 mt-4 flex items-center gap-2 rounded-lg bg-red-50 p-3 text-sm text-red-700 dark:bg-red-950/50 dark:text-red-300 border border-red-200 dark:border-red-900">
            <AlertCircle className="h-4 w-4 shrink-0" />
            <span>{errorMessage}</span>
          </div>
        )}
        {successMessage && (
          <div className="mx-6 mt-4 flex items-center gap-2 rounded-lg bg-emerald-50 p-3 text-sm text-emerald-700 dark:bg-emerald-950/50 dark:text-emerald-300 border border-emerald-200 dark:border-emerald-900">
            <Check className="h-4 w-4 shrink-0" />
            <span>{successMessage}</span>
          </div>
        )}

        <div className="flex-1 overflow-y-auto px-6 py-4 space-y-6">
          {/* General Settings */}
          <form onSubmit={handleSavePipelineInfo} className="rounded-lg border border-gray-200 p-4 dark:border-gray-700 bg-gray-50/50 dark:bg-gray-850">
            <h3 className="text-xs font-bold uppercase tracking-wider text-gray-600 dark:text-gray-400 mb-3">
              Informações do Funil
            </h3>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4 items-end">
              <div className="md:col-span-2">
                <label className="block text-xs font-medium text-gray-700 dark:text-gray-300 mb-1">
                  Nome do Funil
                </label>
                <input
                  type="text"
                  required
                  value={pipelineName}
                  onChange={(e) => setPipelineName(e.target.value)}
                  className="w-full rounded-lg border border-gray-300 px-3 py-1.5 text-sm dark:border-gray-600 dark:bg-gray-700 dark:text-white"
                />
              </div>

              <div className="flex items-center justify-between gap-4">
                <label className="flex items-center gap-2 text-xs font-medium text-gray-700 dark:text-gray-300 cursor-pointer">
                  <input
                    type="checkbox"
                    checked={isDefault}
                    onChange={(e) => setIsDefault(e.target.checked)}
                    className="h-4 w-4 rounded border-gray-300 text-indigo-600 focus:ring-indigo-500"
                  />
                  <span>Funil Padrão</span>
                </label>

                <button
                  type="submit"
                  disabled={savingPipeline}
                  className="rounded-lg bg-indigo-600 px-3 py-1.5 text-xs font-semibold text-white hover:bg-indigo-700 disabled:opacity-50"
                >
                  {savingPipeline ? 'Salvando...' : 'Salvar Nome'}
                </button>
              </div>
            </div>
          </form>

          {/* Stages List */}
          <div>
            <div className="flex items-center justify-between mb-3">
              <div>
                <h3 className="text-sm font-bold text-gray-900 dark:text-white">
                  Colunas / Etapas do Funil
                </h3>
                <p className="text-xs text-gray-500 dark:text-gray-400">
                  Edite nomes, cores, probabilidade de fechamento, SLA de estagnação e ordene as colunas do Kanban
                </p>
              </div>
              <button
                type="button"
                onClick={() => setIsAddingStage(!isAddingStage)}
                className="inline-flex items-center gap-1 rounded-lg border border-indigo-600 bg-indigo-50 px-3 py-1.5 text-xs font-semibold text-indigo-700 hover:bg-indigo-100 dark:bg-indigo-950/40 dark:text-indigo-300 dark:border-indigo-500"
              >
                <Plus className="h-3.5 w-3.5" /> Adicionar Coluna
              </button>
            </div>

            {/* Add New Stage Collapsible Form */}
            {isAddingStage && (
              <form
                onSubmit={handleAddStage}
                className="mb-4 rounded-lg border-2 border-dashed border-indigo-300 bg-indigo-50/30 p-4 dark:border-indigo-700 dark:bg-indigo-950/20"
              >
                <h4 className="text-xs font-bold text-indigo-900 dark:text-indigo-300 mb-3">
                  Nova Etapa do Kanban
                </h4>
                <div className="grid grid-cols-1 md:grid-cols-6 gap-3 items-end">
                  <div className="md:col-span-2">
                    <label className="block text-[11px] font-medium text-gray-700 dark:text-gray-300 mb-1">
                      Nome da Etapa
                    </label>
                    <input
                      type="text"
                      required
                      placeholder="Ex: Demonstração Agendada"
                      value={newStageName}
                      onChange={(e) => setNewStageName(e.target.value)}
                      className="w-full rounded-md border border-gray-300 px-2.5 py-1.5 text-xs dark:border-gray-600 dark:bg-gray-700 dark:text-white"
                    />
                  </div>

                  <div>
                    <label className="block text-[11px] font-medium text-gray-700 dark:text-gray-300 mb-1">
                      Cor
                    </label>
                    <div className="flex items-center gap-1.5">
                      <input
                        type="color"
                        value={newStageColor}
                        onChange={(e) => setNewStageColor(e.target.value)}
                        className="h-7 w-8 cursor-pointer rounded border border-gray-300 p-0"
                      />
                      <span className="text-[10px] font-mono text-gray-500">{newStageColor}</span>
                    </div>
                  </div>

                  <div>
                    <label className="block text-[11px] font-medium text-gray-700 dark:text-gray-300 mb-1">
                      Probabilidade %
                    </label>
                    <input
                      type="number"
                      min="0"
                      max="100"
                      value={newStageProb}
                      onChange={(e) => setNewStageProb(Number(e.target.value))}
                      className="w-full rounded-md border border-gray-300 px-2 py-1.5 text-xs dark:border-gray-600 dark:bg-gray-700 dark:text-white font-mono"
                    />
                  </div>

                  <div>
                    <label className="block text-[11px] font-medium text-gray-700 dark:text-gray-300 mb-1">
                      SLA (dias)
                    </label>
                    <input
                      type="number"
                      min="1"
                      value={newStageSla}
                      onChange={(e) => setNewStageSla(Number(e.target.value))}
                      className="w-full rounded-md border border-gray-300 px-2 py-1.5 text-xs dark:border-gray-600 dark:bg-gray-700 dark:text-white font-mono"
                    />
                  </div>

                  <div>
                    <label className="block text-[11px] font-medium text-gray-700 dark:text-gray-300 mb-1">
                      Tipo
                    </label>
                    <select
                      value={newStageType}
                      onChange={(e) => setNewStageType(e.target.value as StageType)}
                      className="w-full rounded-md border border-gray-300 px-2 py-1.5 text-xs dark:border-gray-600 dark:bg-gray-700 dark:text-white"
                    >
                      <option value="IN_PROGRESS">Em Andamento</option>
                      <option value="WON">Ganho (Fechado)</option>
                      <option value="LOST">Perdido (Cancelado)</option>
                    </select>
                  </div>
                </div>

                <div className="mt-3 flex justify-end gap-2">
                  <button
                    type="button"
                    onClick={() => setIsAddingStage(false)}
                    className="rounded-md border border-gray-300 px-3 py-1 text-xs text-gray-600 hover:bg-gray-100 dark:border-gray-600 dark:text-gray-300"
                  >
                    Cancelar
                  </button>
                  <button
                    type="submit"
                    disabled={loading}
                    className="rounded-md bg-indigo-600 px-3 py-1 text-xs font-semibold text-white hover:bg-indigo-700 disabled:opacity-50"
                  >
                    Criar Coluna
                  </button>
                </div>
              </form>
            )}

            {/* Stages Table / Cards */}
            <div className="space-y-2">
              {stages.map((stage, idx) => (
                <div
                  key={stage.id}
                  className="flex flex-wrap items-center justify-between gap-3 rounded-lg border border-gray-200 bg-white p-3 shadow-sm transition dark:border-gray-700 dark:bg-gray-800"
                >
                  {/* Order & Swatch */}
                  <div className="flex items-center gap-2">
                    <div className="flex flex-col">
                      <button
                        type="button"
                        disabled={idx === 0}
                        onClick={() => handleMoveStage(idx, 'up')}
                        className="p-0.5 text-gray-400 hover:text-indigo-600 disabled:opacity-20 dark:hover:text-indigo-400"
                        title="Mover coluna para a esquerda"
                      >
                        <ArrowUp className="h-3.5 w-3.5" />
                      </button>
                      <button
                        type="button"
                        disabled={idx === stages.length - 1}
                        onClick={() => handleMoveStage(idx, 'down')}
                        className="p-0.5 text-gray-400 hover:text-indigo-600 disabled:opacity-20 dark:hover:text-indigo-400"
                        title="Mover coluna para a direita"
                      >
                        <ArrowDown className="h-3.5 w-3.5" />
                      </button>
                    </div>

                    <input
                      type="color"
                      value={stage.colorHex}
                      onChange={(e) => handleStageFieldChange(idx, 'colorHex', e.target.value)}
                      className="h-8 w-8 cursor-pointer rounded border border-gray-300 p-0"
                      title="Escolher cor da coluna"
                    />
                  </div>

                  {/* Name */}
                  <div className="flex-1 min-w-[160px]">
                    <label className="text-[10px] text-gray-400 block mb-0.5">Nome da Etapa</label>
                    <input
                      type="text"
                      value={stage.name}
                      onChange={(e) => handleStageFieldChange(idx, 'name', e.target.value)}
                      className="w-full rounded-md border border-gray-300 px-2.5 py-1 text-xs font-semibold dark:border-gray-600 dark:bg-gray-700 dark:text-white"
                    />
                  </div>

                  {/* Probability */}
                  <div className="w-20">
                    <label className="text-[10px] text-gray-400 block mb-0.5">Probab. %</label>
                    <input
                      type="number"
                      min="0"
                      max="100"
                      value={stage.winProbabilityPct}
                      onChange={(e) =>
                        handleStageFieldChange(idx, 'winProbabilityPct', Number(e.target.value))
                      }
                      className="w-full rounded-md border border-gray-300 px-2 py-1 text-xs dark:border-gray-600 dark:bg-gray-700 dark:text-white font-mono"
                    />
                  </div>

                  {/* SLA Stagnant Days */}
                  <div className="w-20">
                    <label className="text-[10px] text-gray-400 block mb-0.5">SLA (dias)</label>
                    <input
                      type="number"
                      min="1"
                      value={stage.maxStagnantDays}
                      onChange={(e) =>
                        handleStageFieldChange(idx, 'maxStagnantDays', Number(e.target.value))
                      }
                      className="w-full rounded-md border border-gray-300 px-2 py-1 text-xs dark:border-gray-600 dark:bg-gray-700 dark:text-white font-mono"
                    />
                  </div>

                  {/* Type */}
                  <div className="w-32">
                    <label className="text-[10px] text-gray-400 block mb-0.5">Tipo</label>
                    <select
                      value={stage.stageType}
                      onChange={(e) =>
                        handleStageFieldChange(idx, 'stageType', e.target.value as StageType)
                      }
                      className="w-full rounded-md border border-gray-300 px-2 py-1 text-xs dark:border-gray-600 dark:bg-gray-700 dark:text-white"
                    >
                      <option value="IN_PROGRESS">Em Andamento</option>
                      <option value="WON">Ganho</option>
                      <option value="LOST">Perdido</option>
                    </select>
                  </div>

                  {/* Action Buttons */}
                  <div className="flex items-center gap-1.5 pt-3">
                    <button
                      type="button"
                      onClick={() => handleUpdateStage(stage)}
                      disabled={loading}
                      title="Salvar alterações desta etapa"
                      className="rounded-md bg-emerald-50 border border-emerald-300 p-1.5 text-emerald-700 hover:bg-emerald-100 dark:bg-emerald-950/40 dark:border-emerald-600 dark:text-emerald-300"
                    >
                      <Check className="h-3.5 w-3.5" />
                    </button>

                    <button
                      type="button"
                      onClick={() => handleDeleteStage(stage.id, stage.name)}
                      disabled={loading}
                      title="Excluir etapa"
                      className="rounded-md bg-red-50 border border-red-200 p-1.5 text-red-600 hover:bg-red-100 dark:bg-red-950/40 dark:border-red-800 dark:text-red-400"
                    >
                      <Trash2 className="h-3.5 w-3.5" />
                    </button>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>

        {/* Footer */}
        <div className="flex items-center justify-between border-t border-gray-200 px-6 py-4 dark:border-gray-700 bg-gray-50 dark:bg-gray-850 rounded-b-xl">
          <div className="flex items-center gap-1 text-xs text-gray-500 dark:text-gray-400">
            <HelpCircle className="h-4 w-4" />
            <span>Colunas são exibidas no Kanban na ordem acima (de cima para baixo = esquerda para a direita).</span>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="rounded-lg bg-gray-200 px-4 py-2 text-xs font-semibold text-gray-800 hover:bg-gray-300 dark:bg-gray-700 dark:text-gray-200 dark:hover:bg-gray-600"
          >
            Concluir
          </button>
        </div>
      </div>
    </div>
  );
};
