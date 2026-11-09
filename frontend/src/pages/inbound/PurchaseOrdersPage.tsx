import React, { useState, useEffect } from 'react';
import { PurchaseOrder, PurchaseOrderStatus } from '../../types/inbound';
import { inboundApi } from '../../services/inboundApi';
import { PurchaseOrderModal } from '../../components/inbound/PurchaseOrderModal';
import {
  ShoppingCart,
  Plus,
  CheckCircle2,
  XCircle,
  Clock,
  Truck,
  DollarSign,
  Layers,
} from 'lucide-react';

export const PurchaseOrdersPage: React.FC = () => {
  const [orders, setOrders] = useState<PurchaseOrder[]>([]);
  const [loading, setLoading] = useState(false);
  const [filterStatus, setFilterStatus] = useState<string>('');
  const [modalOpen, setModalOpen] = useState(false);
  const [currentBranchId, setCurrentBranchId] = useState<string>('');

  useEffect(() => {
    const branch = localStorage.getItem('opencontrol_current_branch_id');
    if (branch) {
      setCurrentBranchId(branch);
    }
  }, []);

  const fetchOrders = async () => {
    setLoading(true);
    try {
      const data = await inboundApi.listPurchaseOrders({
        status: filterStatus || undefined,
      });
      setOrders(data);
    } catch (err) {
      console.error('Error fetching purchase orders:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchOrders();
  }, [filterStatus]);

  const handleApprove = async (id: string) => {
    if (window.confirm('Confirma a aprovação deste Pedido de Compra? O pedido será formalizado junto ao fornecedor.')) {
      try {
        await inboundApi.approvePurchaseOrder(id);
        fetchOrders();
      } catch (err: any) {
        alert(err.response?.data?.detail || 'Erro ao aprovar pedido');
      }
    }
  };

  const handleCancel = async (id: string) => {
    if (window.confirm('Deseja realmente cancelar este Pedido de Compra?')) {
      try {
        await inboundApi.cancelPurchaseOrder(id);
        fetchOrders();
      } catch (err: any) {
        alert(err.response?.data?.detail || 'Erro ao cancelar pedido');
      }
    }
  };

  const renderStatusBadge = (status: PurchaseOrderStatus) => {
    switch (status) {
      case 'DRAFT':
        return (
          <span className="inline-flex items-center gap-1 text-[11px] font-semibold bg-gray-100 text-gray-700 px-2 py-0.5 rounded-full">
            <Clock className="h-3 w-3" /> Rascunho
          </span>
        );
      case 'APPROVED':
        return (
          <span className="inline-flex items-center gap-1 text-[11px] font-semibold bg-blue-50 text-blue-700 px-2 py-0.5 rounded-full border border-blue-200">
            <CheckCircle2 className="h-3 w-3" /> Aprovado
          </span>
        );
      case 'PARTIALLY_RECEIVED':
        return (
          <span className="inline-flex items-center gap-1 text-[11px] font-semibold bg-amber-50 text-amber-700 px-2 py-0.5 rounded-full border border-amber-200">
            <Truck className="h-3 w-3" /> Parcial
          </span>
        );
      case 'FULLY_RECEIVED':
        return (
          <span className="inline-flex items-center gap-1 text-[11px] font-semibold bg-emerald-50 text-emerald-700 px-2 py-0.5 rounded-full border border-emerald-200">
            <CheckCircle2 className="h-3 w-3" /> Recebido Total
          </span>
        );
      case 'CANCELLED':
        return (
          <span className="inline-flex items-center gap-1 text-[11px] font-semibold bg-red-50 text-red-700 px-2 py-0.5 rounded-full border border-red-200">
            <XCircle className="h-3 w-3" /> Cancelado
          </span>
        );
    }
  };

  return (
    <div className="p-6 space-y-6">
      {/* Top Header */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 flex items-center gap-2">
            <ShoppingCart className="h-7 w-7 text-indigo-600" />
            Pedidos de Compra a Fornecedores
          </h1>
          <p className="text-sm text-gray-500 mt-1">
            Gestão de pedidos de suprimento, compromissos comerciais e conferência de entregas
          </p>
        </div>
        <button
          onClick={() => setModalOpen(true)}
          className="inline-flex items-center gap-2 px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white font-medium rounded-lg shadow-sm transition cursor-pointer"
        >
          <Plus className="h-4 w-4" />
          Novo Pedido de Compra
        </button>
      </div>

      {/* Metrics Bar */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <div className="p-4 bg-white rounded-xl border border-gray-200 shadow-xs flex items-center justify-between">
          <div>
            <span className="text-xs font-semibold text-gray-500 uppercase tracking-wider">Total de Pedidos</span>
            <div className="text-2xl font-bold font-mono text-gray-900 mt-0.5">{orders.length}</div>
          </div>
          <Layers className="h-8 w-8 text-gray-300" />
        </div>

        <div className="p-4 bg-white rounded-xl border border-gray-200 shadow-xs flex items-center justify-between">
          <div>
            <span className="text-xs font-semibold text-gray-500 uppercase tracking-wider">Aprovados / Pendentes</span>
            <div className="text-2xl font-bold font-mono text-blue-600 mt-0.5">
              {orders.filter((o) => o.status === 'APPROVED' || o.status === 'PARTIALLY_RECEIVED').length}
            </div>
          </div>
          <Truck className="h-8 w-8 text-blue-300" />
        </div>

        <div className="p-4 bg-white rounded-xl border border-gray-200 shadow-xs flex items-center justify-between">
          <div>
            <span className="text-xs font-semibold text-gray-500 uppercase tracking-wider">Valor em Aberto</span>
            <div className="text-2xl font-bold font-mono text-indigo-600 mt-0.5">
              R$ {orders
                .filter((o) => o.status === 'APPROVED' || o.status === 'DRAFT')
                .reduce((acc, o) => acc + Number(o.totalAmount || 0), 0)
                .toFixed(2)}
            </div>
          </div>
          <DollarSign className="h-8 w-8 text-indigo-300" />
        </div>
      </div>

      {/* Filter */}
      <div className="flex items-center gap-3 bg-white p-3 rounded-xl border border-gray-200 shadow-xs">
        <span className="text-xs font-semibold text-gray-600">Filtrar por Status:</span>
        <select
          value={filterStatus}
          onChange={(e) => setFilterStatus(e.target.value)}
          className="text-xs rounded-lg border border-gray-300 px-3 py-1.5 bg-white font-medium focus:outline-none"
        >
          <option value="">Todos os status</option>
          <option value="DRAFT">Rascunho</option>
          <option value="APPROVED">Aprovado</option>
          <option value="PARTIALLY_RECEIVED">Recebido Parcial</option>
          <option value="FULLY_RECEIVED">Recebido Total</option>
          <option value="CANCELLED">Cancelado</option>
        </select>
      </div>

      {/* Orders List Table */}
      <div className="bg-white rounded-xl border border-gray-200 shadow-xs overflow-hidden">
        {loading ? (
          <div className="p-12 text-center text-sm text-gray-400">Carregando pedidos de compra...</div>
        ) : orders.length === 0 ? (
          <div className="p-12 text-center text-sm text-gray-500">
            Nenhum pedido de compra encontrado. Clique em "Novo Pedido de Compra" para criar.
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="bg-gray-50 text-gray-600 font-semibold border-b border-gray-200 text-xs uppercase tracking-wider">
                <tr>
                  <th className="py-3 px-4">Número</th>
                  <th className="py-3 px-4">Fornecedor</th>
                  <th className="py-3 px-4">Data Emissão</th>
                  <th className="py-3 px-4">Previsão Entrega</th>
                  <th className="py-3 px-4">Itens</th>
                  <th className="py-3 px-4">Total</th>
                  <th className="py-3 px-4">Status</th>
                  <th className="py-3 px-4 text-right">Ações</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {orders.map((po) => (
                  <tr key={po.id} className="hover:bg-gray-50/60 transition">
                    <td className="py-3.5 px-4 font-mono font-bold text-gray-900">{po.orderNumber}</td>
                    <td className="py-3.5 px-4 font-medium text-gray-800">{po.supplierName || '—'}</td>
                    <td className="py-3.5 px-4 text-xs text-gray-600">{po.orderDate}</td>
                    <td className="py-3.5 px-4 text-xs text-gray-600">{po.expectedDeliveryDate || '—'}</td>
                    <td className="py-3.5 px-4 text-xs text-gray-500">{po.items?.length || 0} produtos</td>
                    <td className="py-3.5 px-4 font-mono font-bold text-indigo-700">
                      R$ {Number(po.totalAmount).toFixed(2)}
                    </td>
                    <td className="py-3.5 px-4">{renderStatusBadge(po.status)}</td>
                    <td className="py-3.5 px-4 text-right">
                      <div className="flex items-center justify-end gap-2">
                        {po.status === 'DRAFT' && (
                          <button
                            onClick={() => handleApprove(po.id)}
                            className="px-2.5 py-1 bg-blue-600 hover:bg-blue-700 text-white rounded-lg text-xs font-semibold transition"
                          >
                            Aprovar
                          </button>
                        )}
                        {po.status !== 'CANCELLED' && po.status !== 'FULLY_RECEIVED' && (
                          <button
                            onClick={() => handleCancel(po.id)}
                            className="px-2.5 py-1 border border-gray-300 text-gray-600 hover:bg-gray-50 rounded-lg text-xs font-semibold transition"
                          >
                            Cancelar
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      <PurchaseOrderModal
        isOpen={modalOpen}
        onClose={() => setModalOpen(false)}
        onSuccess={fetchOrders}
        branchId={currentBranchId || '00000000-0000-0000-0000-000000000000'}
      />
    </div>
  );
};
