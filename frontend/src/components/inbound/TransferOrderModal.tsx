import React, { useState, useEffect } from 'react';
import axios from 'axios';
import {
  X,
  ArrowRightLeft,
  Plus,
  Trash2,
  Sparkles,
  Warehouse as WarehouseIcon,
  Truck,
  Layers,
} from 'lucide-react';
import { inboundApi } from '../../services/inboundApi';
import { catalogApi } from '../../services/catalogApi';
import { inventoryApi } from '../../services/inventoryApi';
import { Product } from '../../types/catalog';
import { Warehouse } from '../../types/inventory';
import { StockTransferType, CreateTransferRequest } from '../../types/inbound';
import { generateTransferCode } from '../../utils/codeGenerator';

interface CompanyBranchOption {
  id: string;
  code: string;
  tradeName?: string;
  legalName: string;
}

interface TransferItemRow {
  productId: string;
  quantity: number;
  lotId?: string;
  serialNumbers: string[];
  snInput: string;
}

interface TransferOrderModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSuccess: () => void;
}

export const TransferOrderModal: React.FC<TransferOrderModalProps> = ({
  isOpen,
  onClose,
  onSuccess,
}) => {
  const [transferNumber, setTransferNumber] = useState('');
  const [transferType, setTransferType] = useState<StockTransferType>('INTERNAL_WAREHOUSE');
  const [originBranchId, setOriginBranchId] = useState('');
  const [originWarehouseId, setOriginWarehouseId] = useState('');
  const [destinationBranchId, setDestinationBranchId] = useState('');
  const [destinationWarehouseId, setDestinationWarehouseId] = useState('');
  const [trackingNotes, setTrackingNotes] = useState('');

  const [items, setItems] = useState<TransferItemRow[]>([
    { productId: '', quantity: 1, serialNumbers: [], snInput: '' },
  ]);

  const [branches, setBranches] = useState<CompanyBranchOption[]>([]);
  const [warehouses, setWarehouses] = useState<Warehouse[]>([]);
  const [products, setProducts] = useState<Product[]>([]);

  const [loading, setLoading] = useState(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  useEffect(() => {
    if (isOpen) {
      setTransferNumber(generateTransferCode());
      setErrorMsg(null);
      setItems([{ productId: '', quantity: 1, serialNumbers: [], snInput: '' }]);

      // Fetch branches
      axios
        .get<CompanyBranchOption[]>('/api/v1/companies', {
          headers: { Authorization: `Bearer ${localStorage.getItem('opencontrol_token')}` },
        })
        .then((res) => {
          setBranches(res.data || []);
          if (res.data?.length > 0) {
            setOriginBranchId(res.data[0].id);
            setDestinationBranchId(res.data[0].id);
          }
        })
        .catch((err) => console.error('Erro ao buscar filiais:', err));

      // Fetch warehouses
      inventoryApi
        .listWarehouses()
        .then((res) => {
          setWarehouses(res || []);
          if (res?.length > 1) {
            setOriginWarehouseId(res[0].id);
            setDestinationWarehouseId(res[1].id);
          } else if (res?.length === 1) {
            setOriginWarehouseId(res[0].id);
          }
        })
        .catch((err) => console.error('Erro ao buscar armazéns:', err));

      // Fetch products
      catalogApi
        .searchProducts({ size: 100 })
        .then((res) => setProducts(res.items || []))
        .catch((err) => console.error('Erro ao buscar produtos:', err));
    }
  }, [isOpen]);

  // Handle transfer type switch adjustments
  const handleTypeChange = (newType: StockTransferType) => {
    setTransferType(newType);
    if (newType === 'INTERNAL_WAREHOUSE') {
      setDestinationBranchId(originBranchId);
    } else {
      // Find a different destination branch if possible
      const diffBranch = branches.find((b) => b.id !== originBranchId);
      if (diffBranch) {
        setDestinationBranchId(diffBranch.id);
      }
    }
  };

  const handleAddItem = () => {
    setItems((prev) => [
      ...prev,
      { productId: '', quantity: 1, serialNumbers: [], snInput: '' },
    ]);
  };

  const handleRemoveItem = (index: number) => {
    if (items.length > 1) {
      setItems((prev) => prev.filter((_, i) => i !== index));
    }
  };

  const handleItemChange = (index: number, field: keyof TransferItemRow, value: any) => {
    setItems((prev) =>
      prev.map((row, i) => (i === index ? { ...row, [field]: value } : row))
    );
  };

  const handleAddSerial = (index: number) => {
    const row = items[index];
    const sn = row.snInput.trim().toUpperCase();
    if (!sn || row.serialNumbers.includes(sn)) return;

    handleItemChange(index, 'serialNumbers', [...row.serialNumbers, sn]);
    handleItemChange(index, 'snInput', '');
  };

  const handleRemoveSerial = (itemIndex: number, snIndex: number) => {
    const row = items[itemIndex];
    handleItemChange(
      itemIndex,
      'serialNumbers',
      row.serialNumbers.filter((_, i) => i !== snIndex)
    );
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMsg(null);

    if (!originWarehouseId || !destinationWarehouseId) {
      setErrorMsg('Selecione os armazéns de origem e destino.');
      return;
    }

    if (originWarehouseId === destinationWarehouseId) {
      setErrorMsg('O armazém de origem e de destino não podem ser o mesmo.');
      return;
    }

    if (transferType === 'INTERNAL_WAREHOUSE' && originBranchId !== destinationBranchId) {
      setErrorMsg('Transferências internas devem ser entre armazéns da mesma filial.');
      return;
    }

    if (transferType === 'INTER_BRANCH' && originBranchId === destinationBranchId) {
      setErrorMsg('Transferência entre filiais requer filiais de origem e destino distintas.');
      return;
    }

    const validItems = items.filter((it) => it.productId && it.quantity > 0);
    if (validItems.length === 0) {
      setErrorMsg('Adicione ao menos um item válido com quantidade maior que zero.');
      return;
    }

    setLoading(true);

    try {
      const payload: CreateTransferRequest = {
        transferNumber: transferNumber.trim(),
        transferType,
        originBranchId,
        originWarehouseId,
        destinationBranchId,
        destinationWarehouseId,
        trackingNotes: trackingNotes.trim() || undefined,
        items: validItems.map((it) => ({
          productId: it.productId,
          quantity: it.quantity,
          lotId: it.lotId || undefined,
          serialNumbers: it.serialNumbers.length > 0 ? it.serialNumbers : undefined,
        })),
      };

      await inboundApi.createTransfer(payload);
      onSuccess();
      onClose();
    } catch (err: any) {
      setErrorMsg(
        err.response?.data?.message ||
          err.response?.data?.detail ||
          err.message ||
          'Erro ao registrar transferência de estoque'
      );
    } finally {
      setLoading(false);
    }
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 overflow-y-auto bg-black bg-opacity-50 flex items-center justify-center p-4">
      <div className="bg-white rounded-xl shadow-2xl max-w-4xl w-full p-6 max-h-[92vh] flex flex-col">
        {/* Header */}
        <div className="flex justify-between items-center pb-4 border-b">
          <div className="flex items-center space-x-2">
            <ArrowRightLeft className="w-6 h-6 text-primary-600" />
            <div>
              <h2 className="text-xl font-bold text-gray-800">Nova Transferência de Estoque</h2>
              <p className="text-xs text-gray-500">
                Movimentação interna entre almoxarifados ou transferência interestadual/filial com controle em trânsito.
              </p>
            </div>
          </div>
          <button onClick={onClose} className="text-gray-400 hover:text-gray-600 transition">
            <X className="w-5 h-5" />
          </button>
        </div>

        {errorMsg && (
          <div className="mt-3 p-3 bg-red-50 border border-red-200 text-red-700 text-sm rounded-lg">
            {errorMsg}
          </div>
        )}

        {/* Form Body */}
        <form onSubmit={handleSubmit} className="flex-1 overflow-y-auto py-4 space-y-5">
          {/* Top Row: Type and Code */}
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            <div>
              <label className="block text-xs font-semibold text-gray-700 mb-1">
                Tipo de Transferência
              </label>
              <select
                value={transferType}
                onChange={(e) => handleTypeChange(e.target.value as StockTransferType)}
                className="w-full text-sm px-3 py-2 border rounded-lg focus:ring-2 focus:ring-primary-500 bg-white"
              >
                <option value="INTERNAL_WAREHOUSE">Interna (Entre Almoxarifados)</option>
                <option value="INTER_BRANCH">Entre Filiais (Em Trânsito)</option>
              </select>
            </div>

            <div>
              <label className="block text-xs font-semibold text-gray-700 mb-1 flex items-center justify-between">
                <span>Número da Transferência</span>
                <span className="text-[10px] text-primary-600 flex items-center">
                  <Sparkles className="w-3 h-3 mr-0.5" /> Automático
                </span>
              </label>
              <input
                type="text"
                required
                value={transferNumber}
                onChange={(e) => setTransferNumber(e.target.value)}
                className="w-full text-sm px-3 py-2 border rounded-lg focus:ring-2 focus:ring-primary-500 font-mono uppercase bg-gray-50"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-gray-700 mb-1">
                Notas / Conhecimento de Transporte
              </label>
              <input
                type="text"
                value={trackingNotes}
                onChange={(e) => setTrackingNotes(e.target.value)}
                placeholder="Ex: Veículo ABC-1234 / Motorista José"
                className="w-full text-sm px-3 py-2 border rounded-lg focus:ring-2 focus:ring-primary-500"
              />
            </div>
          </div>

          {/* Logistics Routing Section */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4 bg-gray-50 p-4 rounded-xl border border-gray-200">
            {/* Origin */}
            <div className="space-y-3">
              <h3 className="text-xs font-bold text-gray-700 uppercase tracking-wider flex items-center">
                <WarehouseIcon className="w-4 h-4 mr-1 text-red-500" /> Origem (Saída de Mercadoria)
              </h3>
              <div>
                <label className="block text-xs text-gray-600 mb-1">Filial de Origem</label>
                <select
                  value={originBranchId}
                  onChange={(e) => {
                    setOriginBranchId(e.target.value);
                    if (transferType === 'INTERNAL_WAREHOUSE') {
                      setDestinationBranchId(e.target.value);
                    }
                  }}
                  className="w-full text-sm px-3 py-1.5 border rounded-lg bg-white"
                >
                  {branches.map((b) => (
                    <option key={b.id} value={b.id}>
                      {b.code} - {b.tradeName || b.legalName}
                    </option>
                  ))}
                </select>
              </div>
              <div>
                <label className="block text-xs text-gray-600 mb-1">Armazém de Origem</label>
                <select
                  value={originWarehouseId}
                  onChange={(e) => setOriginWarehouseId(e.target.value)}
                  className="w-full text-sm px-3 py-1.5 border rounded-lg bg-white"
                >
                  <option value="">Selecione o armazém...</option>
                  {warehouses.map((w) => (
                    <option key={w.id} value={w.id}>
                      {w.code} - {w.name}
                    </option>
                  ))}
                </select>
              </div>
            </div>

            {/* Destination */}
            <div className="space-y-3">
              <h3 className="text-xs font-bold text-gray-700 uppercase tracking-wider flex items-center">
                <WarehouseIcon className="w-4 h-4 mr-1 text-emerald-500" /> Destino (Entrada de Mercadoria)
              </h3>
              <div>
                <label className="block text-xs text-gray-600 mb-1">Filial de Destino</label>
                <select
                  value={destinationBranchId}
                  disabled={transferType === 'INTERNAL_WAREHOUSE'}
                  onChange={(e) => setDestinationBranchId(e.target.value)}
                  className="w-full text-sm px-3 py-1.5 border rounded-lg bg-white disabled:bg-gray-100 disabled:text-gray-500"
                >
                  {branches.map((b) => (
                    <option key={b.id} value={b.id}>
                      {b.code} - {b.tradeName || b.legalName}
                    </option>
                  ))}
                </select>
              </div>
              <div>
                <label className="block text-xs text-gray-600 mb-1">Armazém de Destino</label>
                <select
                  value={destinationWarehouseId}
                  onChange={(e) => setDestinationWarehouseId(e.target.value)}
                  className="w-full text-sm px-3 py-1.5 border rounded-lg bg-white"
                >
                  <option value="">Selecione o armazém...</option>
                  {warehouses.map((w) => (
                    <option key={w.id} value={w.id}>
                      {w.code} - {w.name}
                    </option>
                  ))}
                </select>
              </div>
            </div>
          </div>

          {/* Items Section */}
          <div className="space-y-3">
            <div className="flex justify-between items-center">
              <h3 className="text-xs font-bold text-gray-700 uppercase tracking-wider flex items-center">
                <Layers className="w-4 h-4 mr-1 text-primary-600" /> Produtos a Transferir
              </h3>
              <button
                type="button"
                onClick={handleAddItem}
                className="flex items-center text-xs font-semibold text-primary-600 hover:text-primary-800"
              >
                <Plus className="w-3.5 h-3.5 mr-1" /> Adicionar Produto
              </button>
            </div>

            <div className="space-y-3">
              {items.map((row, idx) => (
                <div key={idx} className="p-3 bg-white border border-gray-200 rounded-lg shadow-sm space-y-2">
                  <div className="grid grid-cols-1 sm:grid-cols-12 gap-3 items-center">
                    <div className="sm:col-span-7">
                      <label className="block text-[11px] text-gray-500 font-medium">Produto</label>
                      <select
                        required
                        value={row.productId}
                        onChange={(e) => handleItemChange(idx, 'productId', e.target.value)}
                        className="w-full text-xs px-2.5 py-1.5 border rounded bg-white"
                      >
                        <option value="">Selecione o produto no catálogo...</option>
                        {products.map((p) => (
                          <option key={p.id} value={p.id}>
                            {p.sku} - {p.name}
                          </option>
                        ))}
                      </select>
                    </div>

                    <div className="sm:col-span-4">
                      <label className="block text-[11px] text-gray-500 font-medium">Quantidade</label>
                      <input
                        type="number"
                        min="0.0001"
                        step="0.0001"
                        required
                        value={row.quantity}
                        onChange={(e) =>
                          handleItemChange(idx, 'quantity', parseFloat(e.target.value) || 0)
                        }
                        className="w-full text-xs px-2.5 py-1.5 border rounded font-mono"
                      />
                    </div>

                    <div className="sm:col-span-1 flex justify-end pt-4">
                      <button
                        type="button"
                        onClick={() => handleRemoveItem(idx)}
                        disabled={items.length === 1}
                        className="text-red-400 hover:text-red-600 disabled:opacity-30 p-1"
                        title="Remover produto"
                      >
                        <Trash2 className="w-4 h-4" />
                      </button>
                    </div>
                  </div>

                  {/* Serial Numbers (Optional) */}
                  <div className="pt-2 border-t border-dashed flex flex-wrap gap-2 items-center">
                    <span className="text-[11px] text-gray-500 font-medium">Números de Série:</span>
                    {row.serialNumbers.map((sn, sIdx) => (
                      <span
                        key={sIdx}
                        className="inline-flex items-center text-[10px] font-mono bg-blue-50 text-blue-700 border border-blue-200 px-1.5 py-0.5 rounded"
                      >
                        {sn}
                        <button
                          type="button"
                          onClick={() => handleRemoveSerial(idx, sIdx)}
                          className="ml-1 text-red-500 hover:text-red-700"
                        >
                          &times;
                        </button>
                      </span>
                    ))}
                    <div className="flex items-center space-x-1">
                      <input
                        type="text"
                        placeholder="+ Serial"
                        value={row.snInput}
                        onChange={(e) => handleItemChange(idx, 'snInput', e.target.value)}
                        onKeyDown={(e) => {
                          if (e.key === 'Enter') {
                            e.preventDefault();
                            handleAddSerial(idx);
                          }
                        }}
                        className="text-[11px] px-2 py-0.5 border rounded uppercase font-mono w-28"
                      />
                      <button
                        type="button"
                        onClick={() => handleAddSerial(idx)}
                        className="text-[10px] px-2 py-0.5 bg-gray-100 hover:bg-gray-200 rounded border"
                      >
                        Add
                      </button>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* Footer actions */}
          <div className="pt-4 border-t flex justify-end space-x-3">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 border rounded-lg text-sm text-gray-700 hover:bg-gray-50"
            >
              Cancelar
            </button>
            <button
              type="submit"
              disabled={loading}
              className="px-5 py-2 bg-primary-600 text-white rounded-lg text-sm font-medium hover:bg-primary-700 shadow-sm disabled:opacity-50 flex items-center"
            >
              <Truck className="w-4 h-4 mr-2" />
              {loading ? 'Processando...' : 'Confirmar Transferência'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
