import React, { useEffect, useState } from 'react';
import {
  ArrowRightLeft,
  Plus,
  Truck,
  CheckCircle2,
  Clock,
  Search,
  Eye,
  X,
} from 'lucide-react';
import { inboundApi } from '../../services/inboundApi';
import { StockTransfer, StockTransferStatus } from '../../types/inbound';
import { TransferOrderModal } from '../../components/inbound/TransferOrderModal';

export const StockTransfersPage: React.FC = () => {
  const [transfers, setTransfers] = useState<StockTransfer[]>([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState<string>('ALL');
  const [typeFilter, setTypeFilter] = useState<string>('ALL');

  const [isModalOpen, setIsModalOpen] = useState(false);
  const [selectedTransfer, setSelectedTransfer] = useState<StockTransfer | null>(null);

  const [actionLoading, setActionLoading] = useState(false);
  const [feedback, setFeedback] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  const loadTransfers = async () => {
    try {
      setLoading(true);
      const data = await inboundApi.listTransfers();
      setTransfers(data);
    } catch (err: any) {
      console.error('Erro ao listar transferências:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadTransfers();
  }, []);

  const handleReceiveTransfer = async (transferId: string) => {
    if (!window.confirm('Confirmar o recebimento físico e apropriação dos produtos no armazém de destino?')) {
      return;
    }

    setActionLoading(true);
    setFeedback(null);

    try {
      await inboundApi.receiveTransfer(transferId);
      setFeedback({ type: 'success', text: 'Transferência recebida com sucesso! Saldo e CMP atualizados no destino.' });
      loadTransfers();
      if (selectedTransfer?.id === transferId) {
        setSelectedTransfer(null);
      }
    } catch (err: any) {
      setFeedback({
        type: 'error',
        text: err.response?.data?.message || 'Falha ao processar recebimento da transferência.',
      });
    } finally {
      setActionLoading(false);
    }
  };

  const getStatusBadge = (status: StockTransferStatus) => {
    switch (status) {
      case 'RECEIVED_COMPLETED':
        return (
          <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold bg-emerald-50 text-emerald-700 border border-emerald-200">
            <CheckCircle2 className="w-3 h-3 mr-1 text-emerald-600" /> Concluída
          </span>
        );
      case 'DISPATCHED_IN_TRANSIT':
        return (
          <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold bg-amber-50 text-amber-700 border border-amber-200 animate-pulse">
            <Truck className="w-3 h-3 mr-1 text-amber-600" /> Em Trânsito
          </span>
        );
      case 'DRAFT':
        return (
          <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold bg-gray-100 text-gray-700 border border-gray-200">
            <Clock className="w-3 h-3 mr-1 text-gray-500" /> Rascunho
          </span>
        );
      default:
        return (
          <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold bg-red-50 text-red-700">
            {status}
          </span>
        );
    }
  };

  const filtered = transfers.filter((t) => {
    const matchesSearch =
      t.transferNumber.toLowerCase().includes(search.toLowerCase()) ||
      (t.originWarehouseName || '').toLowerCase().includes(search.toLowerCase()) ||
      (t.destinationWarehouseName || '').toLowerCase().includes(search.toLowerCase()) ||
      (t.trackingNotes || '').toLowerCase().includes(search.toLowerCase());

    const matchesStatus = statusFilter === 'ALL' || t.status === statusFilter;
    const matchesType = typeFilter === 'ALL' || t.transferType === typeFilter;

    return matchesSearch && matchesStatus && matchesType;
  });

  return (
    <div className="p-6 max-w-7xl mx-auto space-y-6">
      {/* Page Header */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 bg-white p-6 rounded-xl shadow-sm border border-gray-100">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 flex items-center">
            <ArrowRightLeft className="w-7 h-7 text-primary-600 mr-2" />
            Transferências de Estoque
          </h1>
          <p className="text-sm text-gray-500 mt-1">
            Gestão de transferências internas entre almoxarifados e entre filiais com acompanhamento em trânsito.
          </p>
        </div>
        <button
          onClick={() => setIsModalOpen(true)}
          className="flex items-center px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 text-sm font-medium shadow-sm transition"
        >
          <Plus className="w-4 h-4 mr-2" />
          Nova Transferência
        </button>
      </div>

      {feedback && (
        <div
          className={`p-4 rounded-xl border flex items-center justify-between text-sm ${
            feedback.type === 'success'
              ? 'bg-emerald-50 text-emerald-800 border-emerald-200'
              : 'bg-red-50 text-red-800 border-red-200'
          }`}
        >
          <span>{feedback.text}</span>
          <button onClick={() => setFeedback(null)} className="text-gray-400 hover:text-gray-600">
            &times;
          </button>
        </div>
      )}

      {/* Filter and Search Bar */}
      <div className="grid grid-cols-1 sm:grid-cols-4 gap-3 bg-white p-4 rounded-xl shadow-sm border border-gray-100">
        <div className="sm:col-span-2 relative">
          <Search className="w-4 h-4 absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
          <input
            type="text"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Buscar por número, armazém de origem/destino ou transporte..."
            className="w-full pl-9 pr-3 py-2 border rounded-lg text-sm focus:ring-2 focus:ring-primary-500 focus:outline-none"
          />
        </div>

        <div>
          <select
            value={typeFilter}
            onChange={(e) => setTypeFilter(e.target.value)}
            className="w-full py-2 px-3 border rounded-lg text-sm bg-white focus:ring-2 focus:ring-primary-500"
          >
            <option value="ALL">Todos os Tipos</option>
            <option value="INTERNAL_WAREHOUSE">Interna (Almoxarifados)</option>
            <option value="INTER_BRANCH">Entre Filiais</option>
          </select>
        </div>

        <div>
          <select
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
            className="w-full py-2 px-3 border rounded-lg text-sm bg-white focus:ring-2 focus:ring-primary-500"
          >
            <option value="ALL">Todos os Status</option>
            <option value="DISPATCHED_IN_TRANSIT">Em Trânsito</option>
            <option value="RECEIVED_COMPLETED">Concluídas</option>
          </select>
        </div>
      </div>

      {/* Transfers Table */}
      <div className="bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden">
        {loading ? (
          <div className="p-12 text-center text-gray-400">Carregando transferências...</div>
        ) : filtered.length === 0 ? (
          <div className="p-12 text-center text-gray-500">Nenhuma transferência de estoque encontrada.</div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="bg-gray-50 border-b text-xs uppercase text-gray-500">
                <tr>
                  <th className="px-6 py-3.5">Número</th>
                  <th className="px-6 py-3.5">Tipo</th>
                  <th className="px-6 py-3.5">Origem</th>
                  <th className="px-6 py-3.5">Destino</th>
                  <th className="px-6 py-3.5">Itens</th>
                  <th className="px-6 py-3.5">Status</th>
                  <th className="px-6 py-3.5 text-right">Ações</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {filtered.map((t) => (
                  <tr key={t.id} className="hover:bg-gray-50 transition">
                    <td className="px-6 py-4 font-mono font-bold text-gray-800">
                      {t.transferNumber}
                    </td>
                    <td className="px-6 py-4">
                      <span className="text-xs px-2 py-1 rounded bg-gray-100 text-gray-700 font-medium">
                        {t.transferType === 'INTERNAL_WAREHOUSE' ? 'Interna' : 'Entre Filiais'}
                      </span>
                    </td>
                    <td className="px-6 py-4">
                      <div className="text-xs">
                        <span className="font-semibold text-gray-900 block">
                          {t.originWarehouseName || 'Armazém Origem'}
                        </span>
                        <span className="text-gray-500">{t.originBranchName}</span>
                      </div>
                    </td>
                    <td className="px-6 py-4">
                      <div className="text-xs">
                        <span className="font-semibold text-gray-900 block">
                          {t.destinationWarehouseName || 'Armazém Destino'}
                        </span>
                        <span className="text-gray-500">{t.destinationBranchName}</span>
                      </div>
                    </td>
                    <td className="px-6 py-4 text-xs text-gray-600">
                      {t.items.length} {t.items.length === 1 ? 'produto' : 'produtos'}
                    </td>
                    <td className="px-6 py-4">{getStatusBadge(t.status)}</td>
                    <td className="px-6 py-4 text-right space-x-2">
                      <button
                        onClick={() => setSelectedTransfer(t)}
                        className="inline-flex items-center text-xs font-semibold text-gray-600 hover:text-gray-900 px-2 py-1 border rounded bg-white"
                        title="Ver detalhes"
                      >
                        <Eye className="w-3.5 h-3.5 mr-1" /> Detalhes
                      </button>

                      {t.status === 'DISPATCHED_IN_TRANSIT' && (
                        <button
                          onClick={() => handleReceiveTransfer(t.id)}
                          disabled={actionLoading}
                          className="inline-flex items-center text-xs font-semibold text-white bg-emerald-600 hover:bg-emerald-700 px-2.5 py-1 rounded shadow-sm transition"
                        >
                          <CheckCircle2 className="w-3.5 h-3.5 mr-1" /> Receber
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Detail Modal */}
      {selectedTransfer && (
        <div className="fixed inset-0 z-50 overflow-y-auto bg-black bg-opacity-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-xl shadow-2xl max-w-3xl w-full p-6 space-y-4">
            <div className="flex justify-between items-center pb-3 border-b">
              <div>
                <h3 className="text-lg font-bold text-gray-900">
                  Transferência {selectedTransfer.transferNumber}
                </h3>
                <span className="text-xs text-gray-500">
                  Criada em {new Date(selectedTransfer.createdAt).toLocaleString('pt-BR')}
                </span>
              </div>
              <button
                onClick={() => setSelectedTransfer(null)}
                className="text-gray-400 hover:text-gray-600"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="grid grid-cols-2 gap-4 bg-gray-50 p-3 rounded-lg text-xs">
              <div>
                <span className="text-gray-500 block">Origem:</span>
                <span className="font-semibold text-gray-900">
                  {selectedTransfer.originWarehouseName} ({selectedTransfer.originBranchName})
                </span>
              </div>
              <div>
                <span className="text-gray-500 block">Destino:</span>
                <span className="font-semibold text-gray-900">
                  {selectedTransfer.destinationWarehouseName} ({selectedTransfer.destinationBranchName})
                </span>
              </div>
              {selectedTransfer.trackingNotes && (
                <div className="col-span-2">
                  <span className="text-gray-500 block">Observações / Transporte:</span>
                  <span className="text-gray-800">{selectedTransfer.trackingNotes}</span>
                </div>
              )}
            </div>

            <div>
              <h4 className="text-xs font-bold text-gray-700 uppercase tracking-wider mb-2">
                Itens Transferidos
              </h4>
              <div className="border rounded-lg overflow-hidden">
                <table className="w-full text-xs text-left">
                  <thead className="bg-gray-50 border-b">
                    <tr>
                      <th className="p-2.5">Produto</th>
                      <th className="p-2.5 text-right">Qtd Solicitada</th>
                      <th className="p-2.5 text-right">Qtd Enviada</th>
                      <th className="p-2.5 text-right">Qtd Recebida</th>
                      <th className="p-2.5 text-right">Custo Unitário</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y">
                    {selectedTransfer.items.map((it, i) => (
                      <tr key={i}>
                        <td className="p-2.5 font-medium text-gray-900">
                          {it.productName || it.productId}
                          {it.serialNumbers && it.serialNumbers.length > 0 && (
                            <div className="text-[10px] text-blue-600 font-mono mt-0.5">
                              Série: {it.serialNumbers.join(', ')}
                            </div>
                          )}
                        </td>
                        <td className="p-2.5 text-right font-mono">{it.requestedQuantity}</td>
                        <td className="p-2.5 text-right font-mono">{it.shippedQuantity}</td>
                        <td className="p-2.5 text-right font-mono">{it.receivedQuantity}</td>
                        <td className="p-2.5 text-right font-mono">
                          {new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(
                            it.unitCost || 0
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>

            <div className="pt-3 border-t flex justify-end space-x-2">
              <button
                onClick={() => setSelectedTransfer(null)}
                className="px-4 py-2 border rounded-lg text-xs font-medium text-gray-700 hover:bg-gray-50"
              >
                Fechar
              </button>
              {selectedTransfer.status === 'DISPATCHED_IN_TRANSIT' && (
                <button
                  onClick={() => handleReceiveTransfer(selectedTransfer.id)}
                  disabled={actionLoading}
                  className="px-4 py-2 bg-emerald-600 text-white rounded-lg text-xs font-medium hover:bg-emerald-700"
                >
                  Confirmar Recebimento
                </button>
              )}
            </div>
          </div>
        </div>
      )}

      {/* Create Transfer Modal */}
      <TransferOrderModal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        onSuccess={() => {
          setFeedback({ type: 'success', text: 'Transferência registrada com sucesso!' });
          loadTransfers();
        }}
      />
    </div>
  );
};
