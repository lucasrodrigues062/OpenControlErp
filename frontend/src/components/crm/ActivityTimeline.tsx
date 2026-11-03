import React, { useState, useEffect } from 'react';
import { ActivityEntityType, ActivityType, CrmActivity, CreateCrmActivityRequest } from '../../types/crm';
import { crmApi } from '../../services/crmApi';
import {
  MessageSquare,
  Phone,
  Users,
  CheckSquare,
  FileText,
  Clock,
  Plus,
} from 'lucide-react';

interface ActivityTimelineProps {
  entityType: ActivityEntityType;
  entityId: string;
}

export const ActivityTimeline: React.FC<ActivityTimelineProps> = ({
  entityType,
  entityId,
}) => {
  const [activities, setActivities] = useState<CrmActivity[]>([]);
  const [loading, setLoading] = useState(false);
  const [activityType, setActivityType] = useState<ActivityType>('NOTE');
  const [summary, setSummary] = useState('');
  const [description, setDescription] = useState('');
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  const fetchActivities = async () => {
    try {
      setLoading(true);
      const data = await crmApi.listActivities({ entityType, entityId });
      setActivities(data);
    } catch (err) {
      console.error('Error fetching activities:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (entityId) {
      fetchActivities();
    }
  }, [entityId, entityType]);

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!summary.trim()) return;

    try {
      setSubmitting(true);
      const payload: CreateCrmActivityRequest = {
        entityType,
        entityId,
        activityType,
        summary: summary.trim(),
        description: description.trim() || undefined,
      };
      await crmApi.createActivity(payload);
      setSummary('');
      setDescription('');
      setIsFormOpen(false);
      await fetchActivities();
    } catch (err) {
      console.error('Error logging activity:', err);
    } finally {
      setSubmitting(false);
    }
  };

  const getActivityIcon = (type: ActivityType) => {
    switch (type) {
      case 'CALL':
        return <Phone className="h-4 w-4 text-blue-600 dark:text-blue-400" />;
      case 'WHATSAPP':
        return <MessageSquare className="h-4 w-4 text-emerald-600 dark:text-emerald-400" />;
      case 'MEETING':
        return <Users className="h-4 w-4 text-purple-600 dark:text-purple-400" />;
      case 'TASK':
        return <CheckSquare className="h-4 w-4 text-amber-600 dark:text-amber-400" />;
      case 'NOTE':
      default:
        return <FileText className="h-4 w-4 text-gray-600 dark:text-gray-400" />;
    }
  };

  return (
    <div className="space-y-4">
      {/* Header and Quick Add button */}
      <div className="flex items-center justify-between border-b border-gray-200 pb-3 dark:border-gray-700">
        <div className="flex items-center gap-2">
          <Clock className="h-5 w-5 text-indigo-600 dark:text-indigo-400" />
          <h3 className="text-sm font-bold text-gray-900 dark:text-white">
            Linha do Tempo de Atividades ({activities.length})
          </h3>
        </div>
        <button
          onClick={() => setIsFormOpen(!isFormOpen)}
          className="inline-flex items-center gap-1 rounded-lg bg-indigo-50 px-3 py-1.5 text-xs font-semibold text-indigo-700 hover:bg-indigo-100 dark:bg-indigo-900/40 dark:text-indigo-300"
        >
          <Plus className="h-3.5 w-3.5" /> Registrar Interação
        </button>
      </div>

      {/* Log Form */}
      {isFormOpen && (
        <form onSubmit={handleCreate} className="rounded-xl border border-indigo-200 bg-indigo-50/40 p-4 dark:border-indigo-900/50 dark:bg-indigo-950/20 space-y-3">
          <div className="flex flex-wrap gap-2">
            {[
              { type: 'NOTE', label: 'Nota Interna' },
              { type: 'CALL', label: 'Ligação' },
              { type: 'WHATSAPP', label: 'WhatsApp' },
              { type: 'MEETING', label: 'Reunião' },
              { type: 'TASK', label: 'Tarefa' },
            ].map((item) => (
              <button
                key={item.type}
                type="button"
                onClick={() => setActivityType(item.type as ActivityType)}
                className={`flex items-center gap-1.5 rounded-md px-2.5 py-1 text-xs font-medium transition ${
                  activityType === item.type
                    ? 'bg-indigo-600 text-white shadow-sm'
                    : 'bg-white text-gray-700 hover:bg-gray-100 dark:bg-gray-800 dark:text-gray-300'
                }`}
              >
                {getActivityIcon(item.type as ActivityType)}
                {item.label}
              </button>
            ))}
          </div>

          <div>
            <input
              type="text"
              required
              placeholder="Resumo da atividade (ex: Reunião para apresentação da proposta comercial)..."
              value={summary}
              onChange={(e) => setSummary(e.target.value)}
              className="w-full rounded-md border border-gray-300 px-3 py-1.5 text-xs dark:border-gray-600 dark:bg-gray-700 dark:text-white"
            />
          </div>

          <div>
            <textarea
              rows={2}
              placeholder="Detalhes ou anotações complementares..."
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              className="w-full rounded-md border border-gray-300 p-2 text-xs dark:border-gray-600 dark:bg-gray-700 dark:text-white"
            />
          </div>

          <div className="flex justify-end gap-2">
            <button
              type="button"
              onClick={() => setIsFormOpen(false)}
              className="rounded-md bg-gray-100 px-3 py-1 text-xs text-gray-600 hover:bg-gray-200 dark:bg-gray-700 dark:text-gray-300"
            >
              Cancelar
            </button>
            <button
              type="submit"
              disabled={submitting}
              className="rounded-md bg-indigo-600 px-3 py-1 text-xs font-semibold text-white hover:bg-indigo-700 disabled:opacity-50"
            >
              {submitting ? 'Salvando...' : 'Salvar Registro'}
            </button>
          </div>
        </form>
      )}

      {/* Activities List */}
      <div className="relative border-l-2 border-gray-200 dark:border-gray-700 ml-4 space-y-6 pt-2">
        {activities.map((act) => {
          const performedDate = new Date(act.performedAt);
          return (
            <div key={act.id} className="relative pl-6">
              {/* Timeline marker */}
              <div className="absolute -left-2.5 top-0 flex h-5 w-5 items-center justify-center rounded-full bg-white dark:bg-gray-800 border-2 border-indigo-500">
                <span className="h-1.5 w-1.5 rounded-full bg-indigo-600" />
              </div>

              <div className="rounded-lg border border-gray-200 bg-white p-3.5 shadow-sm dark:border-gray-700 dark:bg-gray-800">
                <div className="flex items-center justify-between gap-2">
                  <div className="flex items-center gap-2">
                    {getActivityIcon(act.activityType)}
                    <span className="font-semibold text-xs text-gray-900 dark:text-white">
                      {act.summary}
                    </span>
                  </div>
                  <span className="text-[10px] text-gray-400 font-mono">
                    {performedDate.toLocaleDateString('pt-BR')} às {performedDate.toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' })}
                  </span>
                </div>

                {act.description && (
                  <p className="mt-2 text-xs text-gray-600 dark:text-gray-300 whitespace-pre-line">
                    {act.description}
                  </p>
                )}
              </div>
            </div>
          );
        })}

        {activities.length === 0 && !loading && (
          <div className="pl-6 text-xs text-gray-400 italic">
            Nenhuma atividade registrada ainda nesta linha do tempo.
          </div>
        )}
      </div>
    </div>
  );
};
