import React from 'react';
import { GoodsReceipt } from '../../types/inbound';
import { X, CheckCircle2, AlertTriangle, Layers } from 'lucide-react';

interface GoodsReceiptModalProps {
  isOpen: boolean;
  onClose: () => void;
  goodsReceipt: GoodsReceipt | null;
}

export const GoodsReceiptModal: React.FC<GoodsReceiptModalProps> = ({
  isOpen,
  onClose,
  goodsReceipt,
}) => {
  if (!isOpen || !goodsReceipt) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4 backdrop-blur-xs">
      <div className="w-full max-w-2xl rounded-2xl bg-white shadow-2xl p-6 border border-gray-100 dark:bg-gray-800 dark:border-gray-700">
        <div className="flex items-center justify-between border-b border-gray-100 pb-4 mb-4 dark:border-gray-700">
          <div className="flex items-center gap-2">
            <div className="rounded-xl bg-blue-50 p-2 text-blue-600 dark:bg-blue-900/30">
              <Layers className="h-5 w-5" />
            </div>
            <div>
              <h2 className="text-lg font-bold text-gray-900 dark:text-white">
                Conferência de Recebimento {goodsReceipt.receiptNumber}
              </h2>
              <p className="text-xs text-gray-500">Data: {goodsReceipt.receiptDate} | Depósito: {goodsReceipt.warehouseName || 'Principal'}</p>
            </div>
          </div>
          <button onClick={onClose} className="rounded-lg p-1.5 text-gray-400 hover:bg-gray-100">
            <X className="h-5 w-5" />
          </button>
        </div>

        <div className="space-y-4">
          <div className="flex items-center justify-between bg-gray-50 p-3 rounded-xl border border-gray-100 dark:bg-gray-700/50">
            <span className="text-xs font-semibold text-gray-600 dark:text-gray-300">Status da Conferência:</span>
            {goodsReceipt.status === 'CONFERRED_OK' ? (
              <span className="inline-flex items-center gap-1 text-xs font-bold text-emerald-700 bg-emerald-100 px-2.5 py-1 rounded-full">
                <CheckCircle2 className="h-4 w-4" /> Conferência Sem Divergências
              </span>
            ) : goodsReceipt.status === 'CONFERRED_WITH_DIVERGENCE' ? (
              <span className="inline-flex items-center gap-1 text-xs font-bold text-amber-700 bg-amber-100 px-2.5 py-1 rounded-full">
                <AlertTriangle className="h-4 w-4" /> Divergência Detectada
              </span>
            ) : (
              <span className="text-xs font-bold text-gray-600 bg-gray-200 px-2.5 py-1 rounded-full">
                {goodsReceipt.status}
              </span>
            )}
          </div>

          <div className="divide-y divide-gray-100 border border-gray-100 rounded-xl overflow-hidden dark:border-gray-700">
            {goodsReceipt.items?.map((it, idx) => (
              <div key={idx} className="p-3.5 flex items-center justify-between text-xs hover:bg-gray-50/50">
                <div>
                  <div className="font-bold text-gray-900 dark:text-white">{it.productName || 'Produto'}</div>
                  <div className="text-gray-500 font-mono mt-0.5">SKU: {it.productSku || '—'}</div>
                  {it.lotNumber && (
                    <div className="text-[11px] font-mono text-indigo-600 font-semibold mt-0.5">
                      Lote: {it.lotNumber} (Validade: {it.expirationDate || 'N/A'})
                    </div>
                  )}
                </div>

                <div className="text-right font-mono space-y-0.5">
                  <div className="text-gray-500">Esperado: <strong>{Number(it.expectedQuantity)}</strong></div>
                  <div className={`font-bold ${it.hasDivergence ? 'text-amber-600' : 'text-emerald-600'}`}>
                    Recebido: <strong>{Number(it.receivedQuantity)}</strong>
                  </div>
                </div>
              </div>
            ))}
          </div>

          {goodsReceipt.conferenceNotes && (
            <div className="p-3 bg-gray-50 rounded-xl text-xs text-gray-600 dark:bg-gray-700/50">
              <span className="font-bold block mb-1">Observações da Doca:</span>
              {goodsReceipt.conferenceNotes}
            </div>
          )}

          <div className="flex justify-end pt-3 border-t border-gray-100">
            <button
              onClick={onClose}
              className="px-4 py-2 bg-gray-900 text-white rounded-lg text-xs font-semibold hover:bg-gray-800"
            >
              Fechar
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};
