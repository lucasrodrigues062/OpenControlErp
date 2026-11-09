import React, { useState, useEffect } from 'react';
import { CreatePurchaseOrderRequest } from '../../types/inbound';
import { inboundApi } from '../../services/inboundApi';
import { catalogApi } from '../../services/catalogApi';
import { crmApi } from '../../services/crmApi';
import { Product } from '../../types/catalog';
import { Customer } from '../../types/crm';
import { X, ShoppingCart, Plus, Trash2, Sparkles } from 'lucide-react';
import { generatePurchaseOrderCode } from '../../utils/codeGenerator';

interface PurchaseOrderModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSuccess: () => void;
  branchId: string;
}

interface OrderItemRow {
  productId: string;
  quantityOrdered: string;
  unitPrice: string;
  discountAmount: string;
}

export const PurchaseOrderModal: React.FC<PurchaseOrderModalProps> = ({
  isOpen,
  onClose,
  onSuccess,
  branchId,
}) => {
  const [orderNumber, setOrderNumber] = useState('');
  const [supplierId, setSupplierId] = useState('');
  const [orderDate, setOrderDate] = useState(new Date().toISOString().split('T')[0]);
  const [expectedDeliveryDate, setExpectedDeliveryDate] = useState('');
  const [paymentTerms, setPaymentTerms] = useState('30 dias');
  const [notes, setNotes] = useState('');
  const [items, setItems] = useState<OrderItemRow[]>([]);

  const [suppliers, setSuppliers] = useState<Customer[]>([]);
  const [products, setProducts] = useState<Product[]>([]);
  const [loading, setLoading] = useState(false);
  const [errorMsg, setErrorMsg] = useState('');

  useEffect(() => {
    if (isOpen) {
      setOrderNumber(generatePurchaseOrderCode());
      setSupplierId('');
      setOrderDate(new Date().toISOString().split('T')[0]);
      setExpectedDeliveryDate('');
      setPaymentTerms('30 dias');
      setNotes('');
      setItems([{ productId: '', quantityOrdered: '1', unitPrice: '0.00', discountAmount: '0.00' }]);
      setErrorMsg('');

      // Fetch suppliers and products
      crmApi.listCustomers({ size: 100 })
        .then((res) => setSuppliers(res.content || []))
        .catch((err) => console.error('Error loading suppliers:', err));

      catalogApi.searchProducts({ size: 100 })
        .then((res) => setProducts(res.items || []))
        .catch((err) => console.error('Error loading products:', err));
    }
  }, [isOpen]);

  if (!isOpen) return null;

  const handleAddItem = () => {
    setItems([...items, { productId: '', quantityOrdered: '1', unitPrice: '0.00', discountAmount: '0.00' }]);
  };

  const handleRemoveItem = (index: number) => {
    if (items.length > 1) {
      setItems(items.filter((_, i) => i !== index));
    }
  };

  const handleItemChange = (index: number, field: keyof OrderItemRow, value: string) => {
    const updated = [...items];
    updated[index][field] = value;
    if (field === 'productId') {
      const p = products.find((prod) => prod.id === value);
      if (p) {
        updated[index].unitPrice = Number(p.averageCost || p.lastAcquisitionCost || 0).toFixed(2);
      }
    }
    setItems(updated);
  };

  const calculateTotal = () => {
    return items.reduce((acc, it) => {
      const q = Number(it.quantityOrdered) || 0;
      const p = Number(it.unitPrice) || 0;
      const d = Number(it.discountAmount) || 0;
      return acc + (q * p - d);
    }, 0).toFixed(2);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!supplierId) {
      setErrorMsg('Selecione um fornecedor.');
      return;
    }
    const invalidItem = items.find((it) => !it.productId || Number(it.quantityOrdered) <= 0);
    if (invalidItem) {
      setErrorMsg('Todos os itens devem ter um produto válido e quantidade maior que zero.');
      return;
    }

    setLoading(true);
    setErrorMsg('');
    try {
      const payload: CreatePurchaseOrderRequest = {
        orderNumber: orderNumber.trim() || generatePurchaseOrderCode(),
        branchId,
        supplierId,
        orderDate,
        expectedDeliveryDate: expectedDeliveryDate || null,
        paymentTerms: paymentTerms || null,
        notes: notes || null,
        items: items.map((it) => ({
          productId: it.productId,
          quantityOrdered: Number(it.quantityOrdered),
          unitPrice: Number(it.unitPrice),
          discountAmount: Number(it.discountAmount) || 0,
        })),
      };

      await inboundApi.createPurchaseOrder(payload);
      onSuccess();
      onClose();
    } catch (err: any) {
      setErrorMsg(err.response?.data?.detail || err.message || 'Erro ao criar pedido de compra');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4 backdrop-blur-xs">
      <div className="w-full max-w-3xl rounded-2xl bg-white shadow-2xl dark:bg-gray-800 border border-gray-100 dark:border-gray-700 max-h-[90vh] flex flex-col">
        {/* Header */}
        <div className="flex items-center justify-between border-b border-gray-100 p-5 dark:border-gray-700">
          <div className="flex items-center gap-2.5">
            <div className="rounded-xl bg-indigo-50 p-2 text-indigo-600 dark:bg-indigo-900/30 dark:text-indigo-400">
              <ShoppingCart className="h-5 w-5" />
            </div>
            <div>
              <h2 className="text-lg font-bold text-gray-900 dark:text-white">Novo Pedido de Compra</h2>
              <p className="text-xs text-gray-500">Compromisso comercial e aquisição de estoque com fornecedor</p>
            </div>
          </div>
          <button onClick={onClose} className="rounded-lg p-1.5 text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-700">
            <X className="h-5 w-5" />
          </button>
        </div>

        {errorMsg && (
          <div className="mx-6 mt-4 rounded-xl bg-red-50 p-3 text-xs text-red-700 border border-red-200 dark:bg-red-900/20 dark:text-red-300">
            {errorMsg}
          </div>
        )}

        {/* Form Body */}
        <form onSubmit={handleSubmit} className="flex-1 overflow-y-auto p-6 space-y-5">
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div>
              <div className="flex items-center justify-between mb-1">
                <label className="text-xs font-semibold text-gray-700 dark:text-gray-300">Número do Pedido *</label>
                <button
                  type="button"
                  onClick={() => setOrderNumber(generatePurchaseOrderCode())}
                  className="flex items-center gap-1 text-[11px] font-medium text-indigo-600 hover:text-indigo-700 dark:text-indigo-400"
                >
                  <Sparkles className="h-3 w-3" /> Gerar outro
                </button>
              </div>
              <input
                type="text"
                required
                value={orderNumber}
                onChange={(e) => setOrderNumber(e.target.value.toUpperCase())}
                className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm font-mono dark:border-gray-600 dark:bg-gray-700 dark:text-white"
              />
            </div>

            <div className="md:col-span-2">
              <label className="block text-xs font-semibold text-gray-700 dark:text-gray-300 mb-1">Fornecedor *</label>
              <select
                required
                value={supplierId}
                onChange={(e) => setSupplierId(e.target.value)}
                className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm dark:border-gray-600 dark:bg-gray-700 dark:text-white"
              >
                <option value="">Selecione o fornecedor homologado...</option>
                {suppliers.map((s) => (
                  <option key={s.id} value={s.id}>
                    {s.tradeName || s.legalName} ({s.document || 'Sem documento'})
                  </option>
                ))}
              </select>
            </div>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div>
              <label className="block text-xs font-semibold text-gray-700 dark:text-gray-300 mb-1">Data de Emissão</label>
              <input
                type="date"
                required
                value={orderDate}
                onChange={(e) => setOrderDate(e.target.value)}
                className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm dark:border-gray-600 dark:bg-gray-700 dark:text-white"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-gray-700 dark:text-gray-300 mb-1">Previsão de Entrega</label>
              <input
                type="date"
                value={expectedDeliveryDate}
                onChange={(e) => setExpectedDeliveryDate(e.target.value)}
                className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm dark:border-gray-600 dark:bg-gray-700 dark:text-white"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-gray-700 dark:text-gray-300 mb-1">Condição de Pagamento</label>
              <input
                type="text"
                placeholder="Ex: 30/60 dias, À vista"
                value={paymentTerms}
                onChange={(e) => setPaymentTerms(e.target.value)}
                className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm dark:border-gray-600 dark:bg-gray-700 dark:text-white"
              />
            </div>
          </div>

          {/* Product Items */}
          <div className="pt-2">
            <div className="flex items-center justify-between pb-2 border-b border-gray-100 dark:border-gray-700">
              <h3 className="text-xs font-bold uppercase tracking-wider text-gray-600 dark:text-gray-400">
                Itens Solicitados
              </h3>
              <button
                type="button"
                onClick={handleAddItem}
                className="flex items-center gap-1 text-xs font-semibold text-indigo-600 hover:text-indigo-700 dark:text-indigo-400"
              >
                <Plus className="h-3.5 w-3.5" /> Adicionar Produto
              </button>
            </div>

            <div className="mt-3 space-y-3">
              {items.map((it, idx) => (
                <div key={idx} className="flex items-center gap-3 p-3 bg-gray-50 dark:bg-gray-700/50 rounded-xl border border-gray-200/60 dark:border-gray-600">
                  <div className="flex-1">
                    <label className="block text-[10px] font-semibold text-gray-500 mb-0.5">Produto *</label>
                    <select
                      required
                      value={it.productId}
                      onChange={(e) => handleItemChange(idx, 'productId', e.target.value)}
                      className="w-full rounded-lg border border-gray-300 px-2.5 py-1.5 text-xs bg-white dark:border-gray-600 dark:bg-gray-700 dark:text-white"
                    >
                      <option value="">Selecione um produto...</option>
                      {products.map((p) => (
                        <option key={p.id} value={p.id}>
                          {p.sku} - {p.name}
                        </option>
                      ))}
                    </select>
                  </div>

                  <div className="w-24">
                    <label className="block text-[10px] font-semibold text-gray-500 mb-0.5">Qtd *</label>
                    <input
                      type="number"
                      step="0.0001"
                      required
                      min="0.0001"
                      value={it.quantityOrdered}
                      onChange={(e) => handleItemChange(idx, 'quantityOrdered', e.target.value)}
                      className="w-full rounded-lg border border-gray-300 px-2.5 py-1.5 text-xs bg-white text-right font-mono dark:border-gray-600 dark:bg-gray-700 dark:text-white"
                    />
                  </div>

                  <div className="w-28">
                    <label className="block text-[10px] font-semibold text-gray-500 mb-0.5">Preço Unit. (R$)</label>
                    <input
                      type="number"
                      step="0.01"
                      required
                      min="0"
                      value={it.unitPrice}
                      onChange={(e) => handleItemChange(idx, 'unitPrice', e.target.value)}
                      className="w-full rounded-lg border border-gray-300 px-2.5 py-1.5 text-xs bg-white text-right font-mono dark:border-gray-600 dark:bg-gray-700 dark:text-white"
                    />
                  </div>

                  <div className="w-24">
                    <label className="block text-[10px] font-semibold text-gray-500 mb-0.5">Desconto</label>
                    <input
                      type="number"
                      step="0.01"
                      min="0"
                      value={it.discountAmount}
                      onChange={(e) => handleItemChange(idx, 'discountAmount', e.target.value)}
                      className="w-full rounded-lg border border-gray-300 px-2.5 py-1.5 text-xs bg-white text-right font-mono dark:border-gray-600 dark:bg-gray-700 dark:text-white"
                    />
                  </div>

                  <div className="pt-3.5">
                    <button
                      type="button"
                      disabled={items.length === 1}
                      onClick={() => handleRemoveItem(idx)}
                      className="p-1.5 text-gray-400 hover:text-red-600 hover:bg-red-50 rounded-lg transition disabled:opacity-30"
                    >
                      <Trash2 className="h-4 w-4" />
                    </button>
                  </div>
                </div>
              ))}
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold text-gray-700 dark:text-gray-300 mb-1">Observações Internas</label>
            <textarea
              rows={2}
              placeholder="Instruções de entrega, doca de recebimento, contatos..."
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              className="w-full rounded-lg border border-gray-300 px-3 py-2 text-xs dark:border-gray-600 dark:bg-gray-700 dark:text-white"
            />
          </div>

          {/* Footer Total */}
          <div className="flex items-center justify-between p-4 bg-indigo-50/70 dark:bg-indigo-900/20 rounded-xl border border-indigo-100 dark:border-indigo-800">
            <span className="text-xs font-bold text-indigo-900 dark:text-indigo-300 uppercase tracking-wider">
              Total Geral Estimado:
            </span>
            <div className="text-xl font-bold font-mono text-indigo-700 dark:text-indigo-400">
              R$ {calculateTotal()}
            </div>
          </div>

          <div className="flex justify-end gap-3 pt-3 border-t border-gray-100 dark:border-gray-700">
            <button
              type="button"
              onClick={onClose}
              disabled={loading}
              className="rounded-lg border border-gray-300 px-4 py-2 text-xs font-medium text-gray-700 hover:bg-gray-50 dark:border-gray-600 dark:text-gray-300"
            >
              Cancelar
            </button>
            <button
              type="submit"
              disabled={loading}
              className="rounded-lg bg-indigo-600 px-5 py-2 text-xs font-semibold text-white hover:bg-indigo-700 disabled:opacity-50 transition"
            >
              {loading ? 'Salvando...' : 'Salvar Pedido de Compra'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
