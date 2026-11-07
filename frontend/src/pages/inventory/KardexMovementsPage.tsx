import React, { useState, useEffect } from 'react';
import { StockMovement, StockMovementType, Warehouse } from '../../types/inventory';
import { Product } from '../../types/catalog';
import { inventoryApi } from '../../services/inventoryApi';
import { catalogApi } from '../../services/catalogApi';
import {
  History,
  ArrowUpRight,
  ArrowDownRight,
  Repeat,
  Scale,
  Layers,
  ChevronLeft,
  ChevronRight,
  FileText,
} from 'lucide-react';

export const KardexMovementsPage: React.FC = () => {
  const [movements, setMovements] = useState<StockMovement[]>([]);
  const [products, setProducts] = useState<Product[]>([]);
  const [warehouses, setWarehouses] = useState<Warehouse[]>([]);

  const [selectedProduct, setSelectedProduct] = useState<string>('');
  const [selectedWarehouse, setSelectedWarehouse] = useState<string>('');
  const [selectedType, setSelectedType] = useState<string>('');

  const [loading, setLoading] = useState(false);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [totalElements, setTotalElements] = useState(0);
  const pageSize = 20;

  const fetchAux = async () => {
    try {
      const [pRes, wList] = await Promise.all([
        catalogApi.searchProducts({ size: 100 }),
        inventoryApi.listWarehouses(),
      ]);
      setProducts(pRes.items);
      setWarehouses(wList);
    } catch (err) {
      console.error('Error fetching filter data:', err);
    }
  };

  const fetchMovements = async () => {
    setLoading(true);
    try {
      const res = await inventoryApi.listMovements({
        productId: selectedProduct || undefined,
        warehouseId: selectedWarehouse || undefined,
        movementType: selectedType || undefined,
        page,
        size: pageSize,
      });
      setMovements(res.items);
      setTotalPages(res.totalPages);
      setTotalElements(res.totalElements);
    } catch (err) {
      console.error('Error fetching movements:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchAux();
  }, []);

  useEffect(() => {
    fetchMovements();
  }, [page, selectedProduct, selectedWarehouse, selectedType]);

  const formatMovementTypeBadge = (type: StockMovementType) => {
    switch (type) {
      case 'INBOUND_PURCHASE':
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-emerald-50 text-emerald-700 border border-emerald-200">
            <ArrowUpRight className="h-3.5 w-3.5" /> Compra / Entrada
          </span>
        );
      case 'OUTBOUND_SALE':
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-blue-50 text-blue-700 border border-blue-200">
            <ArrowDownRight className="h-3.5 w-3.5" /> Venda / Expedição
          </span>
        );
      case 'TRANSFER_OUT':
      case 'TRANSFER_IN':
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-indigo-50 text-indigo-700 border border-indigo-200">
            <Repeat className="h-3.5 w-3.5" /> Transferência
          </span>
        );
      case 'ADJUSTMENT_INVENTORY':
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-amber-50 text-amber-700 border border-amber-200">
            <Scale className="h-3.5 w-3.5" /> Ajuste Manual
          </span>
        );
      case 'RETURN_CUSTOMER':
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-teal-50 text-teal-700 border border-teal-200">
            <ArrowUpRight className="h-3.5 w-3.5" /> Devolução Cliente
          </span>
        );
      case 'RETURN_SUPPLIER':
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-red-50 text-red-700 border border-red-200">
            <ArrowDownRight className="h-3.5 w-3.5" /> Devolução Fornecedor
          </span>
        );
    }
  };

  return (
    <div className="p-6 space-y-6">
      {/* Header */}
      <div>
        <h1 className="text-2xl font-bold text-gray-900 flex items-center gap-2">
          <History className="h-7 w-7 text-indigo-600" />
          Extrato Kardex de Movimentações
        </h1>
        <p className="text-sm text-gray-500 mt-1">
          Registro cronológico imutável de todas as entradas, saídas, transferências e ajustes com auditoria e saldo resultante
        </p>
      </div>

      {/* Filter Bar */}
      <div className="bg-white p-4 rounded-xl border border-gray-200 shadow-sm flex flex-col md:flex-row gap-3 items-center">
        <div className="flex-1 w-full">
          <select
            value={selectedProduct}
            onChange={(e) => {
              setSelectedProduct(e.target.value);
              setPage(0);
            }}
            className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm bg-white"
          >
            <option value="">Todos os Produtos</option>
            {products.map((p) => (
              <option key={p.id} value={p.id}>
                {p.sku} — {p.name}
              </option>
            ))}
          </select>
        </div>

        <div className="w-full md:w-64">
          <select
            value={selectedWarehouse}
            onChange={(e) => {
              setSelectedWarehouse(e.target.value);
              setPage(0);
            }}
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

        <div className="w-full md:w-56">
          <select
            value={selectedType}
            onChange={(e) => {
              setSelectedType(e.target.value);
              setPage(0);
            }}
            className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm bg-white"
          >
            <option value="">Todos os Tipos</option>
            <option value="INBOUND_PURCHASE">Compras / Entradas</option>
            <option value="OUTBOUND_SALE">Vendas / Expedições</option>
            <option value="TRANSFER_OUT">Transferências</option>
            <option value="ADJUSTMENT_INVENTORY">Ajustes Manuais</option>
            <option value="RETURN_CUSTOMER">Devoluções de Clientes</option>
          </select>
        </div>
      </div>

      {/* Timeline Table */}
      <div className="bg-white rounded-xl border border-gray-200 shadow-sm overflow-hidden">
        {loading ? (
          <div className="p-12 text-center text-gray-500">Carregando livro Kardex...</div>
        ) : movements.length === 0 ? (
          <div className="p-12 text-center">
            <Layers className="h-12 w-12 text-gray-300 mx-auto mb-3" />
            <h3 className="text-base font-semibold text-gray-700">Nenhuma movimentação registrada</h3>
            <p className="text-sm text-gray-500 mt-1">
              Todas as transações de compras, vendas e ajustes físicos alimentarão automaticamente este extrato.
            </p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="bg-gray-50 text-gray-600 font-semibold border-b border-gray-200 text-xs uppercase tracking-wider">
                <tr>
                  <th className="py-3 px-4">Data & Hora</th>
                  <th className="py-3 px-4">Tipo de Movimento</th>
                  <th className="py-3 px-4">Produto & SKU</th>
                  <th className="py-3 px-4">Origem / Destino</th>
                  <th className="py-3 px-4 text-right">Qtd Movimentada</th>
                  <th className="py-3 px-4 text-right">Custo Unitário</th>
                  <th className="py-3 px-4 text-right">Saldo Resultante</th>
                  <th className="py-3 px-4">Doc / Justificativa</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {movements.map((m) => (
                  <tr key={m.id} className="hover:bg-gray-50/80 transition">
                    <td className="py-3.5 px-4 text-xs font-mono text-gray-600">
                      {new Date(m.createdAt).toLocaleString('pt-BR')}
                    </td>
                    <td className="py-3.5 px-4">{formatMovementTypeBadge(m.movementType)}</td>
                    <td className="py-3.5 px-4">
                      <div className="font-semibold text-gray-900 text-xs">{m.productName || 'Produto'}</div>
                      <span className="font-mono text-[11px] text-gray-400">{m.productSku}</span>
                    </td>
                    <td className="py-3.5 px-4 text-xs text-gray-600">
                      {m.sourceWarehouseName && <div>De: {m.sourceWarehouseName}</div>}
                      {m.destinationWarehouseName && <div>Para: {m.destinationWarehouseName}</div>}
                      {!m.sourceWarehouseName && !m.destinationWarehouseName && '—'}
                    </td>
                    <td className="py-3.5 px-4 text-right font-mono font-bold text-gray-900">
                      {Number(m.quantity).toFixed(4)}
                    </td>
                    <td className="py-3.5 px-4 text-right font-mono text-xs text-gray-600">
                      R$ {Number(m.unitCost).toFixed(2)}
                    </td>
                    <td className="py-3.5 px-4 text-right font-mono font-bold text-indigo-700">
                      {Number(m.balanceAfter).toFixed(4)}
                    </td>
                    <td className="py-3.5 px-4 text-xs">
                      {m.referenceDocument && (
                        <div className="font-medium text-gray-800 flex items-center gap-1">
                          <FileText className="h-3 w-3 text-gray-400" />
                          {m.referenceDocument}
                        </div>
                      )}
                      {m.reasonCode && (
                        <span className="text-[11px] font-mono text-gray-500 bg-gray-100 px-1.5 py-0.2 rounded">
                          {m.reasonCode}
                        </span>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {/* Pagination */}
        {totalPages > 1 && (
          <div className="flex items-center justify-between p-4 border-t border-gray-100 bg-gray-50/50">
            <span className="text-xs text-gray-500">
              Total de <strong>{totalElements}</strong> transações registradas no Kardex
            </span>
            <div className="flex items-center gap-2">
              <button
                disabled={page === 0}
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                className="p-1.5 border border-gray-300 rounded-lg hover:bg-white text-gray-600 disabled:opacity-40 disabled:hover:bg-transparent"
              >
                <ChevronLeft className="h-4 w-4" />
              </button>
              <span className="text-xs text-gray-600 font-medium px-2">
                Página {page + 1} de {totalPages}
              </span>
              <button
                disabled={page >= totalPages - 1}
                onClick={() => setPage((p) => p + 1)}
                className="p-1.5 border border-gray-300 rounded-lg hover:bg-white text-gray-600 disabled:opacity-40 disabled:hover:bg-transparent"
              >
                <ChevronRight className="h-4 w-4" />
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};
