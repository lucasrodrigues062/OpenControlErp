import React, { useState, useEffect } from 'react';
import {
  ProductLot,
  ProductSerialNumber,
  Warehouse,
  CreateLotRequest,
  CreateSerialNumberRequest,
  SerialNumberStatus,
  LotStatus,
} from '../../types/inventory';
import { Product } from '../../types/catalog';
import { inventoryApi } from '../../services/inventoryApi';
import { catalogApi } from '../../services/catalogApi';
import {
  ShieldCheck,
  Calendar,
  Barcode,
  Search,
  Plus,
  AlertTriangle,
  CheckCircle2,
  X,
  Layers,
  Clock,
  Laptop,
} from 'lucide-react';

export const LotsAndSerialsPage: React.FC = () => {
  const [activeTab, setActiveTab] = useState<'lots' | 'serials'>('lots');
  const [products, setProducts] = useState<Product[]>([]);
  const [warehouses, setWarehouses] = useState<Warehouse[]>([]);
  const [selectedProduct, setSelectedProduct] = useState<string>('');
  const [selectedWarehouse, setSelectedWarehouse] = useState<string>('');
  const [loading, setLoading] = useState(false);

  // Lots state
  const [lots, setLots] = useState<ProductLot[]>([]);
  const [lotModalOpen, setLotModalOpen] = useState(false);
  const [lotNumber, setLotNumber] = useState('');
  const [mfgDate, setMfgDate] = useState('');
  const [expDate, setExpDate] = useState('');
  const [lotQty, setLotQty] = useState('0');
  const [lotError, setLotError] = useState('');

  // Serials state
  const [serials, setSerials] = useState<ProductSerialNumber[]>([]);
  const [serialSearch, setSerialSearch] = useState('');
  const [selectedSerialStatus, setSelectedSerialStatus] = useState<string>('');
  const [serialModalOpen, setSerialModalOpen] = useState(false);
  const [serialNumber, setSerialNumber] = useState('');
  const [warrantyUntil, setWarrantyUntil] = useState('');
  const [serialLotId, setSerialLotId] = useState('');
  const [serialError, setSerialError] = useState('');

  const fetchAuxData = async () => {
    try {
      const [prodRes, wList] = await Promise.all([
        catalogApi.searchProducts({ size: 100 }),
        inventoryApi.listWarehouses(),
      ]);
      setProducts(prodRes.items);
      setWarehouses(wList);
      if (prodRes.items.length > 0 && !selectedProduct) {
        setSelectedProduct(prodRes.items[0].id);
      }
      if (wList.length > 0 && !selectedWarehouse) {
        setSelectedWarehouse(wList[0].id);
      }
    } catch (err) {
      console.error('Error fetching aux data:', err);
    }
  };

  const fetchLots = async () => {
    if (!selectedProduct) return;
    setLoading(true);
    try {
      const data = await inventoryApi.listLots({
        productId: selectedProduct,
        warehouseId: selectedWarehouse || undefined,
      });
      setLots(data);
    } catch (err) {
      console.error('Error fetching lots:', err);
    } finally {
      setLoading(false);
    }
  };

  const fetchSerials = async () => {
    setLoading(true);
    try {
      const data = await inventoryApi.listSerialNumbers({
        productId: selectedProduct || undefined,
        serialNumber: serialSearch || undefined,
        status: selectedSerialStatus || undefined,
      });
      setSerials(data);
    } catch (err) {
      console.error('Error fetching serial numbers:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchAuxData();
  }, []);

  useEffect(() => {
    if (activeTab === 'lots') {
      fetchLots();
    } else {
      fetchSerials();
    }
  }, [activeTab, selectedProduct, selectedWarehouse, selectedSerialStatus]);

  const handleCreateLot = async (e: React.FormEvent) => {
    e.preventDefault();
    setLotError('');
    try {
      const payload: CreateLotRequest = {
        productId: selectedProduct,
        warehouseId: selectedWarehouse,
        lotNumber: lotNumber.trim().toUpperCase(),
        manufacturingDate: mfgDate,
        expirationDate: expDate,
        quantityOnHand: Number(lotQty || '0'),
      };
      await inventoryApi.createLot(payload);
      setLotModalOpen(false);
      setLotNumber('');
      setMfgDate('');
      setExpDate('');
      setLotQty('0');
      fetchLots();
    } catch (err: any) {
      setLotError(err.response?.data?.detail || err.message || 'Erro ao registrar lote');
    }
  };

  const handleCreateSerial = async (e: React.FormEvent) => {
    e.preventDefault();
    setSerialError('');
    try {
      const payload: CreateSerialNumberRequest = {
        productId: selectedProduct,
        warehouseId: selectedWarehouse,
        serialNumber: serialNumber.trim().toUpperCase(),
        lotId: serialLotId || null,
        warrantyUntil: warrantyUntil || null,
      };
      await inventoryApi.createSerialNumber(payload);
      setSerialModalOpen(false);
      setSerialNumber('');
      setWarrantyUntil('');
      setSerialLotId('');
      fetchSerials();
    } catch (err: any) {
      setSerialError(err.response?.data?.detail || err.message || 'Erro ao registrar número de série');
    }
  };

  const getDaysToExpiry = (expDateStr: string) => {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const exp = new Date(expDateStr + 'T00:00:00');
    const diffTime = exp.getTime() - today.getTime();
    return Math.ceil(diffTime / (1000 * 60 * 60 * 24));
  };

  const formatLotStatusBadge = (status: LotStatus, expDateStr: string) => {
    const days = getDaysToExpiry(expDateStr);
    if (days < 0 || status === 'EXPIRED') {
      return (
        <span className="inline-flex items-center gap-1 text-[11px] font-semibold text-red-700 bg-red-50 border border-red-200 px-2 py-0.5 rounded-full">
          <AlertTriangle className="h-3 w-3" /> Vencido ({Math.abs(days)}d atrás)
        </span>
      );
    }
    if (days <= 30) {
      return (
        <span className="inline-flex items-center gap-1 text-[11px] font-semibold text-amber-700 bg-amber-50 border border-amber-200 px-2 py-0.5 rounded-full">
          <Clock className="h-3 w-3" /> Vence em {days} dias
        </span>
      );
    }
    return (
      <span className="inline-flex items-center gap-1 text-[11px] font-semibold text-emerald-700 bg-emerald-50 border border-emerald-200 px-2 py-0.5 rounded-full">
        <CheckCircle2 className="h-3 w-3" /> Liberado ({days}d)
      </span>
    );
  };

  const formatSerialStatusBadge = (status: SerialNumberStatus) => {
    switch (status) {
      case 'IN_STOCK':
        return <span className="px-2 py-0.5 text-xs font-semibold rounded-full bg-emerald-50 text-emerald-700 border border-emerald-200">Em Estoque</span>;
      case 'RESERVED':
        return <span className="px-2 py-0.5 text-xs font-semibold rounded-full bg-amber-50 text-amber-700 border border-amber-200">Reservado</span>;
      case 'DISPATCHED_SOLD':
        return <span className="px-2 py-0.5 text-xs font-semibold rounded-full bg-blue-50 text-blue-700 border border-blue-200">Vendido / Entregue</span>;
      case 'RMA_MAINTENANCE':
        return <span className="px-2 py-0.5 text-xs font-semibold rounded-full bg-purple-50 text-purple-700 border border-purple-200">Em Manutenção (RMA)</span>;
      case 'WRITTEN_OFF':
        return <span className="px-2 py-0.5 text-xs font-semibold rounded-full bg-gray-100 text-gray-600">Baixado / Descartado</span>;
    }
  };

  return (
    <div className="p-6 space-y-6">
      {/* Header */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 flex items-center gap-2">
            <ShieldCheck className="h-7 w-7 text-indigo-600" />
            Rastreabilidade: Lotes (FEFO) & Números de Série
          </h1>
          <p className="text-sm text-gray-500 mt-1">
            Controle de validade (First Expire, First Out) para perecíveis/farmacêuticos e rastreamento unitário de equipamentos
          </p>
        </div>

        <div className="flex items-center gap-2">
          {activeTab === 'lots' ? (
            <button
              onClick={() => setLotModalOpen(true)}
              className="inline-flex items-center gap-2 px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white font-medium rounded-lg shadow-sm transition"
            >
              <Plus className="h-4 w-4" />
              Novo Lote
            </button>
          ) : (
            <button
              onClick={() => setSerialModalOpen(true)}
              className="inline-flex items-center gap-2 px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white font-medium rounded-lg shadow-sm transition"
            >
              <Plus className="h-4 w-4" />
              Registrar Nº de Série
            </button>
          )}
        </div>
      </div>

      {/* Tabs */}
      <div className="flex border-b border-gray-200">
        <button
          onClick={() => setActiveTab('lots')}
          className={`flex items-center gap-2 py-3 px-5 text-sm font-semibold border-b-2 transition ${
            activeTab === 'lots'
              ? 'border-indigo-600 text-indigo-600'
              : 'border-transparent text-gray-500 hover:text-gray-700'
          }`}
        >
          <Calendar className="h-4 w-4" />
          Controle por Lote & Validade (FEFO)
        </button>
        <button
          onClick={() => setActiveTab('serials')}
          className={`flex items-center gap-2 py-3 px-5 text-sm font-semibold border-b-2 transition ${
            activeTab === 'serials'
              ? 'border-indigo-600 text-indigo-600'
              : 'border-transparent text-gray-500 hover:text-gray-700'
          }`}
        >
          <Barcode className="h-4 w-4" />
          Números de Série Unitários
        </button>
      </div>

      {/* Filter Bar */}
      <div className="bg-white p-4 rounded-xl border border-gray-200 shadow-sm flex flex-col md:flex-row gap-3 items-center">
        <div className="flex-1 w-full">
          <label className="block text-[11px] font-semibold text-gray-500 mb-1">Produto Selecionado</label>
          <select
            value={selectedProduct}
            onChange={(e) => setSelectedProduct(e.target.value)}
            className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm bg-white"
          >
            {products.map((p) => (
              <option key={p.id} value={p.id}>
                {p.sku} — {p.name} ({p.trackingMode})
              </option>
            ))}
          </select>
        </div>

        <div className="w-full md:w-64">
          <label className="block text-[11px] font-semibold text-gray-500 mb-1">Depósito</label>
          <select
            value={selectedWarehouse}
            onChange={(e) => setSelectedWarehouse(e.target.value)}
            className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm bg-white"
          >
            <option value="">Todos os Depósitos</option>
            {warehouses.map((w) => (
              <option key={w.id} value={w.id}>
                {w.code} - {w.name}
              </option>
            ))}
          </select>
        </div>

        {activeTab === 'serials' && (
          <>
            <div className="w-full md:w-48">
              <label className="block text-[11px] font-semibold text-gray-500 mb-1">Status do Ativo</label>
              <select
                value={selectedSerialStatus}
                onChange={(e) => setSelectedSerialStatus(e.target.value)}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm bg-white"
              >
                <option value="">Todos os Status</option>
                <option value="IN_STOCK">Em Estoque</option>
                <option value="RESERVED">Reservado</option>
                <option value="DISPATCHED_SOLD">Vendido / Entregue</option>
                <option value="RMA_MAINTENANCE">Em Manutenção</option>
                <option value="WRITTEN_OFF">Baixado</option>
              </select>
            </div>
            <div className="w-full md:w-64">
              <label className="block text-[11px] font-semibold text-gray-500 mb-1">Buscar Serial</label>
              <div className="flex items-center gap-2 px-3 py-2 border border-gray-300 rounded-lg bg-gray-50">
                <Search className="h-4 w-4 text-gray-400" />
                <input
                  type="text"
                  placeholder="Escanear nº de série..."
                  value={serialSearch}
                  onChange={(e) => setSerialSearch(e.target.value)}
                  onKeyDown={(e) => e.key === 'Enter' && fetchSerials()}
                  className="bg-transparent border-none outline-none text-xs w-full"
                />
              </div>
            </div>
          </>
        )}
      </div>

      {/* LOTS TAB CONTENT */}
      {activeTab === 'lots' && (
        <div className="bg-white rounded-xl border border-gray-200 shadow-sm overflow-hidden">
          {loading ? (
            <div className="p-12 text-center text-gray-500">Carregando lotes...</div>
          ) : lots.length === 0 ? (
            <div className="p-12 text-center">
              <Layers className="h-12 w-12 text-gray-300 mx-auto mb-3" />
              <h3 className="text-base font-semibold text-gray-700">Nenhum lote registrado para este produto</h3>
              <p className="text-sm text-gray-500 mt-1">
                Cadastre o lote inicial ou receba mercadorias informando número do lote e data de validade.
              </p>
            </div>
          ) : (
            <table className="w-full text-left text-sm">
              <thead className="bg-gray-50 text-gray-600 font-semibold border-b border-gray-200 text-xs uppercase tracking-wider">
                <tr>
                  <th className="py-3 px-4">Lote / Batch</th>
                  <th className="py-3 px-4">Data Fabricação</th>
                  <th className="py-3 px-4">Data Validade</th>
                  <th className="py-3 px-4">Saldo em Estoque</th>
                  <th className="py-3 px-4">Status & FEFO</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {lots.map((lot) => (
                  <tr key={lot.id} className="hover:bg-gray-50/80 transition">
                    <td className="py-3.5 px-4 font-mono font-bold text-gray-900">
                      {lot.lotNumber}
                    </td>
                    <td className="py-3.5 px-4 text-xs text-gray-600 font-medium">
                      {new Date(lot.manufacturingDate + 'T00:00:00').toLocaleDateString('pt-BR')}
                    </td>
                    <td className="py-3.5 px-4 text-xs font-semibold text-gray-900">
                      {new Date(lot.expirationDate + 'T00:00:00').toLocaleDateString('pt-BR')}
                    </td>
                    <td className="py-3.5 px-4 text-xs">
                      <span className="font-semibold text-gray-900">
                        {Number(lot.quantityOnHand).toFixed(2)}
                      </span>
                      {Number(lot.quantityReserved) > 0 && (
                        <span className="text-gray-400 ml-1">
                          (Reservado: {Number(lot.quantityReserved).toFixed(2)})
                        </span>
                      )}
                    </td>
                    <td className="py-3.5 px-4">
                      {formatLotStatusBadge(lot.status, lot.expirationDate)}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      )}

      {/* SERIALS TAB CONTENT */}
      {activeTab === 'serials' && (
        <div className="bg-white rounded-xl border border-gray-200 shadow-sm overflow-hidden">
          {loading ? (
            <div className="p-12 text-center text-gray-500">Carregando números de série...</div>
          ) : serials.length === 0 ? (
            <div className="p-12 text-center">
              <Laptop className="h-12 w-12 text-gray-300 mx-auto mb-3" />
              <h3 className="text-base font-semibold text-gray-700">Nenhum número de série cadastrado</h3>
              <p className="text-sm text-gray-500 mt-1">
                Cadastre os números de série individuais para rastreamento de garantia e ciclo de vida.
              </p>
            </div>
          ) : (
            <table className="w-full text-left text-sm">
              <thead className="bg-gray-50 text-gray-600 font-semibold border-b border-gray-200 text-xs uppercase tracking-wider">
                <tr>
                  <th className="py-3 px-4">Número de Série</th>
                  <th className="py-3 px-4">Garantia até</th>
                  <th className="py-3 px-4">Cadastrado em</th>
                  <th className="py-3 px-4">Status do Ativo</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {serials.map((s) => (
                  <tr key={s.id} className="hover:bg-gray-50/80 transition">
                    <td className="py-3.5 px-4 font-mono font-bold text-gray-900">
                      {s.serialNumber}
                    </td>
                    <td className="py-3.5 px-4 text-xs text-gray-600">
                      {s.warrantyUntil
                        ? new Date(s.warrantyUntil + 'T00:00:00').toLocaleDateString('pt-BR')
                        : '—'}
                    </td>
                    <td className="py-3.5 px-4 text-xs text-gray-500">
                      {new Date(s.createdAt).toLocaleDateString('pt-BR')}
                    </td>
                    <td className="py-3.5 px-4">
                      {formatSerialStatusBadge(s.status)}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      )}

      {/* Lot Modal */}
      {lotModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 backdrop-blur-sm p-4">
          <div className="bg-white rounded-2xl max-w-md w-full p-6 shadow-xl border border-gray-100">
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-lg font-bold text-gray-900">Cadastrar Novo Lote</h2>
              <button onClick={() => setLotModalOpen(false)} className="text-gray-400 hover:text-gray-600 p-1">
                <X className="h-5 w-5" />
              </button>
            </div>

            {lotError && (
              <div className="mb-4 p-3 bg-red-50 border border-red-200 text-red-700 text-xs rounded-lg">
                {lotError}
              </div>
            )}

            <form onSubmit={handleCreateLot} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-gray-700 mb-1">Identificador do Lote *</label>
                <input
                  type="text"
                  required
                  placeholder="EX: LOTE-2026-001"
                  value={lotNumber}
                  onChange={(e) => setLotNumber(e.target.value.toUpperCase())}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm font-mono focus:ring-2 focus:ring-indigo-500 focus:outline-none"
                />
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-semibold text-gray-700 mb-1">Data Fabricação *</label>
                  <input
                    type="date"
                    required
                    value={mfgDate}
                    onChange={(e) => setMfgDate(e.target.value)}
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-indigo-500 focus:outline-none"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-gray-700 mb-1">Data Validade *</label>
                  <input
                    type="date"
                    required
                    value={expDate}
                    onChange={(e) => setExpDate(e.target.value)}
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-indigo-500 focus:outline-none"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-semibold text-gray-700 mb-1">Quantidade Inicial do Lote</label>
                <input
                  type="number"
                  step="0.0001"
                  required
                  value={lotQty}
                  onChange={(e) => setLotQty(e.target.value)}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-indigo-500 focus:outline-none"
                />
              </div>

              <div className="flex items-center justify-end gap-3 pt-4 border-t border-gray-100">
                <button
                  type="button"
                  onClick={() => setLotModalOpen(false)}
                  className="px-4 py-2 border border-gray-300 text-gray-700 rounded-lg text-sm font-medium hover:bg-gray-50"
                >
                  Cancelar
                </button>
                <button
                  type="submit"
                  className="px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg text-sm font-medium shadow-sm"
                >
                  Salvar Lote
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Serial Modal */}
      {serialModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 backdrop-blur-sm p-4">
          <div className="bg-white rounded-2xl max-w-md w-full p-6 shadow-xl border border-gray-100">
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-lg font-bold text-gray-900">Registrar Número de Série</h2>
              <button onClick={() => setSerialModalOpen(false)} className="text-gray-400 hover:text-gray-600 p-1">
                <X className="h-5 w-5" />
              </button>
            </div>

            {serialError && (
              <div className="mb-4 p-3 bg-red-50 border border-red-200 text-red-700 text-xs rounded-lg">
                {serialError}
              </div>
            )}

            <form onSubmit={handleCreateSerial} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-gray-700 mb-1">Número de Série Único *</label>
                <input
                  type="text"
                  required
                  placeholder="EX: SN-DELL-9948210"
                  value={serialNumber}
                  onChange={(e) => setSerialNumber(e.target.value.toUpperCase())}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm font-mono focus:ring-2 focus:ring-indigo-500 focus:outline-none"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-gray-700 mb-1">Garantia Válida até</label>
                <input
                  type="date"
                  value={warrantyUntil}
                  onChange={(e) => setWarrantyUntil(e.target.value)}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-indigo-500 focus:outline-none"
                />
              </div>

              <div className="flex items-center justify-end gap-3 pt-4 border-t border-gray-100">
                <button
                  type="button"
                  onClick={() => setSerialModalOpen(false)}
                  className="px-4 py-2 border border-gray-300 text-gray-700 rounded-lg text-sm font-medium hover:bg-gray-50"
                >
                  Cancelar
                </button>
                <button
                  type="submit"
                  className="px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg text-sm font-medium shadow-sm"
                >
                  Registrar Serial
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
