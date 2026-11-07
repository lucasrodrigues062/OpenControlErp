import React, { useState, useEffect } from 'react';
import axios from 'axios';
import {
  CreateStockMovementRequest,
  StockMovementType,
  Warehouse,
} from '../../types/inventory';
import { Product } from '../../types/catalog';
import { inventoryApi } from '../../services/inventoryApi';
import { catalogApi } from '../../services/catalogApi';
import {
  Scale,
  X,
  AlertCircle,
  ArrowUpRight,
  ArrowDownRight,
  Repeat,
} from 'lucide-react';

interface ManualAdjustmentModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSuccess: () => void;
  defaultProductId?: string;
  defaultWarehouseId?: string;
}

export const ManualAdjustmentModal: React.FC<ManualAdjustmentModalProps> = ({
  isOpen,
  onClose,
  onSuccess,
  defaultProductId,
  defaultWarehouseId,
}) => {
  const [products, setProducts] = useState<Product[]>([]);
  const [warehouses, setWarehouses] = useState<Warehouse[]>([]);

  const [productId, setProductId] = useState(defaultProductId || '');
  const [warehouseId, setWarehouseId] = useState(defaultWarehouseId || '');
  const [destWarehouseId, setDestWarehouseId] = useState('');
  const [branchId, setBranchId] = useState('');
  const [actionType, setActionType] = useState<'ADD' | 'REMOVE' | 'TRANSFER'>('ADD');
  const [quantity, setQuantity] = useState('');
  const [reasonCode, setReasonCode] = useState('CONTAGEM_CICLICA');
  const [notes, setNotes] = useState('');
  const [errorMsg, setErrorMsg] = useState('');
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (isOpen) {
      const loadData = async () => {
        try {
          const [pRes, wList, bRes] = await Promise.all([
            catalogApi.searchProducts({ size: 100 }),
            inventoryApi.listWarehouses(),
            axios.get('/api/v1/companies', {
              headers: { Authorization: `Bearer ${localStorage.getItem('opencontrol_token')}` },
            }),
          ]);
          setProducts(pRes.items);
          setWarehouses(wList);

          if (pRes.items.length > 0 && !productId) {
            setProductId(defaultProductId || pRes.items[0].id);
          }
          if (wList.length > 0 && !warehouseId) {
            setWarehouseId(defaultWarehouseId || wList[0].id);
          }
          if (bRes.data.length > 0) {
            setBranchId(bRes.data[0].id);
          }
        } catch (err) {
          console.error('Error fetching adjustment modal data:', err);
        }
      };
      loadData();
      setErrorMsg('');
      setQuantity('');
      setNotes('');
    }
  }, [isOpen]);

  if (!isOpen) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMsg('');
    const qtyNum = Number(quantity);
    if (!qtyNum || qtyNum <= 0) {
      setErrorMsg('Informe uma quantidade válida superior a zero.');
      return;
    }

    setLoading(true);
    try {
      let movementType: StockMovementType = 'ADJUSTMENT_INVENTORY';
      let srcW: string | null = null;
      let dstW: string | null = null;

      if (actionType === 'ADD') {
        movementType = 'ADJUSTMENT_INVENTORY';
        dstW = warehouseId;
      } else if (actionType === 'REMOVE') {
        movementType = 'ADJUSTMENT_INVENTORY';
        srcW = warehouseId;
      } else if (actionType === 'TRANSFER') {
        movementType = 'TRANSFER_OUT';
        srcW = warehouseId;
        dstW = destWarehouseId;
        if (srcW === dstW) {
          setErrorMsg('O depósito de destino deve ser diferente da origem.');
          setLoading(false);
          return;
        }
      }

      const payload: CreateStockMovementRequest = {
        branchId,
        movementType,
        productId,
        sourceWarehouseId: srcW,
        destinationWarehouseId: dstW,
        quantity: qtyNum,
        reasonCode,
        notes: notes.trim() || undefined,
      };

      await inventoryApi.createMovement(payload);
      onSuccess();
      onClose();
    } catch (err: any) {
      setErrorMsg(err.response?.data?.detail || err.message || 'Erro ao processar movimentação');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 backdrop-blur-sm p-4">
      <div className="bg-white rounded-2xl max-w-lg w-full p-6 shadow-xl border border-gray-100">
        <div className="flex items-center justify-between pb-4 border-b border-gray-100">
          <div className="flex items-center gap-2">
            <Scale className="h-6 w-6 text-indigo-600" />
            <h2 className="text-lg font-bold text-gray-900">Ajuste Manual / Movimentação</h2>
          </div>
          <button onClick={onClose} className="text-gray-400 hover:text-gray-600 p-1">
            <X className="h-5 w-5" />
          </button>
        </div>

        {errorMsg && (
          <div className="mt-4 p-3 bg-red-50 border border-red-200 text-red-700 text-xs rounded-lg flex items-center gap-2">
            <AlertCircle className="h-4 w-4 flex-shrink-0" />
            <span>{errorMsg}</span>
          </div>
        )}

        {/* Action Type Selector */}
        <div className="grid grid-cols-3 gap-2 mt-4">
          <button
            type="button"
            onClick={() => setActionType('ADD')}
            className={`flex items-center justify-center gap-1.5 py-2.5 px-3 rounded-xl border text-xs font-semibold transition ${
              actionType === 'ADD'
                ? 'border-emerald-500 bg-emerald-50 text-emerald-800 ring-1 ring-emerald-500'
                : 'border-gray-200 text-gray-600 hover:bg-gray-50'
            }`}
          >
            <ArrowUpRight className="h-4 w-4 text-emerald-600" />
            Entrada / Acréscimo
          </button>
          <button
            type="button"
            onClick={() => setActionType('REMOVE')}
            className={`flex items-center justify-center gap-1.5 py-2.5 px-3 rounded-xl border text-xs font-semibold transition ${
              actionType === 'REMOVE'
                ? 'border-red-500 bg-red-50 text-red-800 ring-1 ring-red-500'
                : 'border-gray-200 text-gray-600 hover:bg-gray-50'
            }`}
          >
            <ArrowDownRight className="h-4 w-4 text-red-600" />
            Saída / Perda
          </button>
          <button
            type="button"
            onClick={() => setActionType('TRANSFER')}
            className={`flex items-center justify-center gap-1.5 py-2.5 px-3 rounded-xl border text-xs font-semibold transition ${
              actionType === 'TRANSFER'
                ? 'border-indigo-500 bg-indigo-50 text-indigo-800 ring-1 ring-indigo-500'
                : 'border-gray-200 text-gray-600 hover:bg-gray-50'
            }`}
          >
            <Repeat className="h-4 w-4 text-indigo-600" />
            Transferência
          </button>
        </div>

        <form onSubmit={handleSubmit} className="mt-4 space-y-4">
          <div>
            <label className="block text-xs font-semibold text-gray-700 mb-1">Produto *</label>
            <select
              required
              value={productId}
              onChange={(e) => setProductId(e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm bg-white"
            >
              {products.map((p) => (
                <option key={p.id} value={p.id}>
                  {p.sku} — {p.name}
                </option>
              ))}
            </select>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-semibold text-gray-700 mb-1">
                {actionType === 'TRANSFER' ? 'Depósito de Origem *' : 'Depósito *'}
              </label>
              <select
                required
                value={warehouseId}
                onChange={(e) => setWarehouseId(e.target.value)}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm bg-white"
              >
                {warehouses.map((w) => (
                  <option key={w.id} value={w.id}>
                    {w.code} - {w.name}
                  </option>
                ))}
              </select>
            </div>

            {actionType === 'TRANSFER' && (
              <div>
                <label className="block text-xs font-semibold text-gray-700 mb-1">Depósito de Destino *</label>
                <select
                  required
                  value={destWarehouseId}
                  onChange={(e) => setDestWarehouseId(e.target.value)}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm bg-white"
                >
                  <option value="">Selecione o destino...</option>
                  {warehouses
                    .filter((w) => w.id !== warehouseId)
                    .map((w) => (
                      <option key={w.id} value={w.id}>
                        {w.code} - {w.name}
                      </option>
                    ))}
                </select>
              </div>
            )}

            <div>
              <label className="block text-xs font-semibold text-gray-700 mb-1">Quantidade *</label>
              <input
                type="number"
                step="0.0001"
                required
                placeholder="0.0000"
                value={quantity}
                onChange={(e) => setQuantity(e.target.value)}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm font-semibold"
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold text-gray-700 mb-1">Motivo / Justificativa (Auditável) *</label>
            <select
              required
              value={reasonCode}
              onChange={(e) => setReasonCode(e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm bg-white"
            >
              <option value="CONTAGEM_CICLICA">Contagem Cíclica / Inventário Físico</option>
              <option value="AVARIA_QUEBRA">Avaria, Quebra ou Danificação</option>
              <option value="VENCIMENTO_PERDA">Validade Vencida / Descarte</option>
              <option value="TRANSFERENCIA_FILIAIS">Transferência entre Almoxarifados</option>
              <option value="CONSUMO_INTERNO">Uso e Consumo Interno</option>
              <option value="OUTRO">Outra Regularização de Saldo</option>
            </select>
          </div>

          <div>
            <label className="block text-xs font-semibold text-gray-700 mb-1">Observações / Detalhes</label>
            <textarea
              rows={2}
              placeholder="Descreva o motivo da divergência física ou número de documento..."
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg text-xs"
            />
          </div>

          <div className="flex items-center justify-end gap-3 pt-4 border-t border-gray-100">
            <button
              type="button"
              onClick={onClose}
              disabled={loading}
              className="px-4 py-2 border border-gray-300 text-gray-700 rounded-lg text-sm font-medium hover:bg-gray-50"
            >
              Cancelar
            </button>
            <button
              type="submit"
              disabled={loading}
              className="px-5 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg text-sm font-medium shadow-sm transition disabled:opacity-50"
            >
              {loading ? 'Processando...' : 'Confirmar Movimentação'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
