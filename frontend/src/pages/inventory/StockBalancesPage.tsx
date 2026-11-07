import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { StockBalance, Warehouse } from '../../types/inventory';
import { inventoryApi } from '../../services/inventoryApi';
import { ManualAdjustmentModal } from '../../components/inventory/ManualAdjustmentModal';
import {
  Boxes,
  Search,
  Warehouse as WarehouseIcon,
  History,
  Scale,
  Layers,
} from 'lucide-react';

export const StockBalancesPage: React.FC = () => {
  const navigate = useNavigate();

  const [balances, setBalances] = useState<StockBalance[]>([]);
  const [warehouses, setWarehouses] = useState<Warehouse[]>([]);
  const [selectedWarehouse, setSelectedWarehouse] = useState<string>('');
  const [search, setSearch] = useState('');
  const [loading, setLoading] = useState(false);

  // Modal
  const [isAdjustmentModalOpen, setIsAdjustmentModalOpen] = useState(false);
  const [selectedProductId, setSelectedProductId] = useState<string | undefined>();
  const [selectedWarehouseId, setSelectedWarehouseId] = useState<string | undefined>();

  const fetchWarehouses = async () => {
    try {
      const data = await inventoryApi.listWarehouses();
      setWarehouses(data);
    } catch (err) {
      console.error('Error fetching warehouses:', err);
    }
  };

  const fetchBalances = async () => {
    setLoading(true);
    try {
      const data = await inventoryApi.listBalances({
        warehouseId: selectedWarehouse || undefined,
      });
      setBalances(data);
    } catch (err) {
      console.error('Error fetching balances:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchWarehouses();
  }, []);

  useEffect(() => {
    fetchBalances();
  }, [selectedWarehouse]);

  const openAdjustmentForBalance = (b: StockBalance) => {
    setSelectedProductId(b.productId);
    setSelectedWarehouseId(b.warehouseId);
    setIsAdjustmentModalOpen(true);
  };

  const openNewAdjustment = () => {
    setSelectedProductId(undefined);
    setSelectedWarehouseId(selectedWarehouse || undefined);
    setIsAdjustmentModalOpen(true);
  };

  const filteredBalances = balances.filter((b) =>
    (b.productName?.toLowerCase().includes(search.toLowerCase()) ?? false) ||
    (b.productSku?.toLowerCase().includes(search.toLowerCase()) ?? false) ||
    (b.warehouseName?.toLowerCase().includes(search.toLowerCase()) ?? false)
  );

  return (
    <div className="p-6 space-y-6">
      {/* Top Header */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 flex items-center gap-2">
            <Boxes className="h-7 w-7 text-indigo-600" />
            Posição & Saldos de Estoque
          </h1>
          <p className="text-sm text-gray-500 mt-1">
            Visualização consolidada de estoque físico (On Hand), reservado em vendas e saldo disponível para entrega
          </p>
        </div>

        <div className="flex items-center gap-3">
          <button
            onClick={() => navigate('/inventory/movements')}
            className="inline-flex items-center gap-2 px-3.5 py-2 border border-gray-300 hover:bg-gray-50 text-gray-700 text-sm font-medium rounded-lg transition"
          >
            <History className="h-4 w-4 text-gray-500" />
            Extrato Kardex
          </button>
          <button
            onClick={openNewAdjustment}
            className="inline-flex items-center gap-2 px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white font-medium rounded-lg shadow-sm transition"
          >
            <Scale className="h-4 w-4" />
            Ajuste / Transferência
          </button>
        </div>
      </div>

      {/* Filter Toolbar */}
      <div className="bg-white p-4 rounded-xl border border-gray-200 shadow-sm flex flex-col md:flex-row gap-3 items-center">
        <div className="flex-1 flex items-center gap-2 px-3 py-2 bg-gray-50 border border-gray-200 rounded-lg w-full">
          <Search className="h-4 w-4 text-gray-400" />
          <input
            type="text"
            placeholder="Buscar por nome do item, SKU ou armazém..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="flex-1 bg-transparent border-none outline-none text-sm text-gray-800 placeholder-gray-400"
          />
        </div>

        <div className="flex items-center gap-2 w-full md:w-auto">
          <WarehouseIcon className="h-4 w-4 text-gray-500" />
          <select
            value={selectedWarehouse}
            onChange={(e) => setSelectedWarehouse(e.target.value)}
            className="px-3 py-2 border border-gray-300 rounded-lg text-sm text-gray-700 focus:outline-none focus:ring-2 focus:ring-indigo-500 bg-white"
          >
            <option value="">Todos os Depósitos</option>
            {warehouses.map((w) => (
              <option key={w.id} value={w.id}>
                {w.code} - {w.name}
              </option>
            ))}
          </select>
        </div>
      </div>

      {/* Balances Table */}
      <div className="bg-white rounded-xl border border-gray-200 shadow-sm overflow-hidden">
        {loading ? (
          <div className="p-12 text-center text-gray-500">Carregando saldos de estoque...</div>
        ) : filteredBalances.length === 0 ? (
          <div className="p-12 text-center">
            <Layers className="h-12 w-12 text-gray-300 mx-auto mb-3" />
            <h3 className="text-base font-semibold text-gray-700">Nenhum saldo registrado</h3>
            <p className="text-sm text-gray-500 mt-1">
              Dê entrada em produtos via compras ou realize um ajuste inicial para movimentar o estoque.
            </p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="bg-gray-50 text-gray-600 font-semibold border-b border-gray-200 text-xs uppercase tracking-wider">
                <tr>
                  <th className="py-3 px-4">Item & SKU</th>
                  <th className="py-3 px-4">Depósito</th>
                  <th className="py-3 px-4">Endereço (Box)</th>
                  <th className="py-3 px-4 text-right">Saldo Físico</th>
                  <th className="py-3 px-4 text-right">Reservado</th>
                  <th className="py-3 px-4 text-right">Disponível</th>
                  <th className="py-3 px-4 text-right">Ação</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {filteredBalances.map((b) => (
                  <tr key={b.id} className="hover:bg-gray-50/80 transition">
                    <td className="py-3.5 px-4">
                      <div className="font-bold text-gray-900">{b.productName || 'Produto'}</div>
                      <span className="font-mono bg-gray-100 px-1.5 py-0.5 rounded text-[11px] text-gray-700 font-semibold">
                        {b.productSku || 'SKU'}
                      </span>
                    </td>
                    <td className="py-3.5 px-4 text-xs font-medium text-gray-700">
                      {b.warehouseName || '—'}
                    </td>
                    <td className="py-3.5 px-4 text-xs font-mono text-gray-600">
                      {b.locationAddress || 'Geral'}
                    </td>
                    <td className="py-3.5 px-4 text-right font-mono font-bold text-gray-900">
                      {Number(b.quantityOnHand).toFixed(4)}
                    </td>
                    <td className="py-3.5 px-4 text-right font-mono text-xs text-amber-700 font-semibold">
                      {Number(b.quantityReserved) > 0 ? Number(b.quantityReserved).toFixed(4) : '—'}
                    </td>
                    <td className="py-3.5 px-4 text-right font-mono font-bold text-emerald-700">
                      {Number(b.quantityAvailable).toFixed(4)}
                    </td>
                    <td className="py-3.5 px-4 text-right">
                      <button
                        onClick={() => openAdjustmentForBalance(b)}
                        className="px-2.5 py-1 text-xs font-medium border border-gray-300 hover:border-indigo-500 hover:text-indigo-600 rounded-lg transition"
                      >
                        Ajustar
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      <ManualAdjustmentModal
        isOpen={isAdjustmentModalOpen}
        onClose={() => setIsAdjustmentModalOpen(false)}
        onSuccess={fetchBalances}
        defaultProductId={selectedProductId}
        defaultWarehouseId={selectedWarehouseId}
      />
    </div>
  );
};
