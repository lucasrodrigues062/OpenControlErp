import React, { useState, useEffect } from 'react';
import { InboundInvoice, InboundInvoiceItem } from '../../types/inbound';
import { Product } from '../../types/catalog';
import { Warehouse } from '../../types/inventory';
import { inboundApi } from '../../services/inboundApi';
import { catalogApi } from '../../services/catalogApi';
import { inventoryApi } from '../../services/inventoryApi';
import { NFeXmlUploadModal } from '../../components/inbound/NFeXmlUploadModal';
import {
  FileText,
  UploadCloud,
  CheckCircle2,
  Layers,
  PackageCheck,
  X,
} from 'lucide-react';

export const InboundInvoicesPage: React.FC = () => {
  const [invoices, setInvoices] = useState<InboundInvoice[]>([]);
  const [selectedInvoice, setSelectedInvoice] = useState<InboundInvoice | null>(null);
  const [loading, setLoading] = useState(false);
  const [uploadModalOpen, setUploadModalOpen] = useState(false);
  const [currentBranchId, setCurrentBranchId] = useState<string>('');

  // Item Matching Modal state
  const [matchingItem, setMatchingItem] = useState<InboundInvoiceItem | null>(null);
  const [selectedCatalogProductId, setSelectedCatalogProductId] = useState('');
  const [conversionFactor, setConversionFactor] = useState('1');
  const [products, setProducts] = useState<Product[]>([]);
  const [warehouses, setWarehouses] = useState<Warehouse[]>([]);
  const [selectedWarehouseId, setSelectedWarehouseId] = useState('');
  const [finalizeModalOpen, setFinalizeModalOpen] = useState(false);

  useEffect(() => {
    const branch = localStorage.getItem('opencontrol_current_branch_id') || '00000000-0000-0000-0000-000000000000';
    setCurrentBranchId(branch);
    catalogApi.searchProducts({ size: 100 }).then((res) => setProducts(res.items || []));
    inventoryApi.listWarehouses().then((res) => {
      setWarehouses(res || []);
      if (res && res.length > 0) setSelectedWarehouseId(res[0].id);
    });
  }, []);

  const fetchInvoices = async () => {
    setLoading(true);
    try {
      const data = await inboundApi.listInboundInvoices();
      setInvoices(data);
      if (data.length > 0 && !selectedInvoice) {
        setSelectedInvoice(data[0]);
      } else if (selectedInvoice) {
        const updated = data.find((inv) => inv.id === selectedInvoice.id);
        if (updated) setSelectedInvoice(updated);
      }
    } catch (err) {
      console.error('Error fetching invoices:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchInvoices();
  }, []);

  const openMatchModal = (item: InboundInvoiceItem) => {
    setMatchingItem(item);
    setSelectedCatalogProductId(item.matchedProductId || '');
    setConversionFactor(item.conversionFactor ? item.conversionFactor.toString() : '1');
  };

  const handleSaveMatch = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedInvoice || !matchingItem || !selectedCatalogProductId) return;

    try {
      await inboundApi.matchInvoiceItem(selectedInvoice.id, matchingItem.id, {
        productId: selectedCatalogProductId,
        conversionFactor: Number(conversionFactor) || 1,
      });
      setMatchingItem(null);
      fetchInvoices();
    } catch (err: any) {
      alert(err.response?.data?.detail || 'Erro ao vincular produto');
    }
  };

  const handleFinalizeStock = async () => {
    if (!selectedInvoice || !selectedWarehouseId) return;

    try {
      await inboundApi.finalizeInvoiceStock(selectedInvoice.id, {
        warehouseId: selectedWarehouseId,
      });
      setFinalizeModalOpen(false);
      fetchInvoices();
      alert('Nota fiscal estocada com sucesso! Saldos e Custo Médio Ponderado (CMP) atualizados.');
    } catch (err: any) {
      alert(err.response?.data?.detail || 'Erro ao estocar nota fiscal');
    }
  };

  const isAllItemsMatched = (inv: InboundInvoice) => {
    return inv.items.every((it) => it.matchedProductId != null);
  };

  return (
    <div className="p-6 space-y-6">
      {/* Top Header */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 flex items-center gap-2">
            <FileText className="h-7 w-7 text-emerald-600" />
            Entrada de Notas Fiscais (NF-e) & Custos Landed
          </h1>
          <p className="text-sm text-gray-500 mt-1">
            Importação XML SEFAZ, de-para com conversão de embalagem, rateio de frete e formação de preço de custo
          </p>
        </div>
        <button
          onClick={() => setUploadModalOpen(true)}
          className="inline-flex items-center gap-2 px-4 py-2 bg-emerald-600 hover:bg-emerald-700 text-white font-medium rounded-lg shadow-sm transition cursor-pointer"
        >
          <UploadCloud className="h-4 w-4" />
          Importar XML de NF-e
        </button>
      </div>

      {/* Main Grid: Left Invoice List, Right Invoice Detail & Items */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Left Column: Invoice cards */}
        <div className="space-y-4">
          <div className="bg-white rounded-xl border border-gray-200 p-4 shadow-xs space-y-3">
            <h3 className="text-xs font-bold text-gray-500 uppercase tracking-wider flex items-center gap-1.5">
              <Layers className="h-4 w-4 text-emerald-600" /> Notas Recebidas
            </h3>

            {loading ? (
              <div className="p-6 text-center text-xs text-gray-400">Carregando notas...</div>
            ) : invoices.length === 0 ? (
              <div className="p-6 text-center text-xs text-gray-500">
                Nenhuma nota fiscal importada. Clique em "Importar XML de NF-e".
              </div>
            ) : (
              <div className="space-y-2 max-h-[600px] overflow-y-auto">
                {invoices.map((inv) => {
                  const isSelected = selectedInvoice?.id === inv.id;
                  const allMatched = isAllItemsMatched(inv);

                  return (
                    <button
                      key={inv.id}
                      onClick={() => setSelectedInvoice(inv)}
                      className={`w-full text-left p-3.5 rounded-xl border transition flex flex-col gap-1.5 cursor-pointer ${
                        isSelected
                          ? 'border-emerald-500 bg-emerald-50/50 shadow-xs ring-1 ring-emerald-500'
                          : 'border-gray-200 hover:border-gray-300 hover:bg-gray-50'
                      }`}
                    >
                      <div className="flex items-center justify-between">
                        <span className="font-mono text-xs font-bold text-gray-900">
                          NF-e {inv.invoiceNumber} (Série {inv.series})
                        </span>
                        {inv.status === 'STOCKED' ? (
                          <span className="text-[10px] font-semibold bg-emerald-100 text-emerald-800 px-2 py-0.5 rounded-full">
                            Estocado
                          </span>
                        ) : allMatched ? (
                          <span className="text-[10px] font-semibold bg-blue-100 text-blue-800 px-2 py-0.5 rounded-full">
                            Pronto p/ Estocar
                          </span>
                        ) : (
                          <span className="text-[10px] font-semibold bg-amber-100 text-amber-800 px-2 py-0.5 rounded-full">
                            De-Para Pendente
                          </span>
                        )}
                      </div>

                      <div className="text-xs font-medium text-gray-800 truncate">
                        {inv.supplierName || 'Fornecedor'}
                      </div>

                      <div className="flex items-center justify-between text-xs text-gray-500 mt-1 font-mono">
                        <span>{inv.issuanceDate}</span>
                        <span className="font-bold text-emerald-700">R$ {Number(inv.totalInvoiceAmount).toFixed(2)}</span>
                      </div>
                    </button>
                  );
                })}
              </div>
            )}
          </div>
        </div>

        {/* Right Column: Invoice Detail & Items Table */}
        <div className="lg:col-span-2 bg-white rounded-xl border border-gray-200 p-5 shadow-xs flex flex-col justify-between">
          {selectedInvoice ? (
            <div className="space-y-5">
              {/* Invoice Banner */}
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-4 border-b border-gray-100">
                <div>
                  <div className="flex items-center gap-2">
                    <span className="text-lg font-bold font-mono text-gray-900">
                      NF-e nº {selectedInvoice.invoiceNumber}
                    </span>
                    <span className="text-xs text-gray-500 font-mono">Série: {selectedInvoice.series}</span>
                  </div>
                  <p className="text-xs text-gray-600 mt-0.5">
                    Fornecedor: <strong>{selectedInvoice.supplierName}</strong> | Emissão: {selectedInvoice.issuanceDate}
                  </p>
                  {selectedInvoice.accessKey && (
                    <p className="text-[10px] font-mono text-gray-400 mt-0.5">
                      Chave: {selectedInvoice.accessKey}
                    </p>
                  )}
                </div>

                {selectedInvoice.status !== 'STOCKED' && (
                  <button
                    onClick={() => setFinalizeModalOpen(true)}
                    disabled={!isAllItemsMatched(selectedInvoice)}
                    className="inline-flex items-center gap-1.5 px-4 py-2 bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-bold rounded-lg shadow-sm transition disabled:opacity-40 cursor-pointer"
                  >
                    <PackageCheck className="h-4 w-4" />
                    Dar Entrada no Estoque & Atualizar CMP
                  </button>
                )}
              </div>

              {/* Totals Summary */}
              <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 bg-gray-50 rounded-xl p-3 text-xs border border-gray-100 font-mono">
                <div>
                  <span className="text-[10px] text-gray-400 block">Total Produtos</span>
                  <span className="font-bold text-gray-800">R$ {Number(selectedInvoice.totalProducts).toFixed(2)}</span>
                </div>
                <div>
                  <span className="text-[10px] text-gray-400 block">Frete + Seguro</span>
                  <span className="font-bold text-gray-800">
                    R$ {(Number(selectedInvoice.totalFreight) + Number(selectedInvoice.totalInsurance)).toFixed(2)}
                  </span>
                </div>
                <div>
                  <span className="text-[10px] text-gray-400 block">Outras Despesas</span>
                  <span className="font-bold text-gray-800">R$ {Number(selectedInvoice.totalExpenses).toFixed(2)}</span>
                </div>
                <div>
                  <span className="text-[10px] text-emerald-600 font-bold block">Valor Total da Nota</span>
                  <span className="font-bold text-emerald-700 text-sm">
                    R$ {Number(selectedInvoice.totalInvoiceAmount).toFixed(2)}
                  </span>
                </div>
              </div>

              {/* Items List */}
              <div>
                <h4 className="text-xs font-bold text-gray-600 uppercase tracking-wider mb-2">
                  Itens da Nota ({selectedInvoice.items?.length || 0})
                </h4>

                <div className="divide-y divide-gray-100 border border-gray-100 rounded-xl overflow-hidden">
                  {selectedInvoice.items?.map((it) => (
                    <div key={it.id} className="p-3.5 flex flex-col md:flex-row md:items-center justify-between gap-3 hover:bg-gray-50/70 transition">
                      <div className="space-y-1">
                        <div className="flex items-center gap-2">
                          <span className="text-xs font-bold text-gray-900">{it.supplierItemDescription}</span>
                          <span className="font-mono text-[10px] bg-gray-100 text-gray-600 px-1.5 py-0.2 rounded font-semibold">
                            Cód: {it.supplierItemCode}
                          </span>
                        </div>

                        <div className="text-xs text-gray-500 flex items-center gap-2">
                          <span>Qtd Nota: {Number(it.invoicedQuantity)} {it.supplierUom}</span>
                          <span>•</span>
                          <span>Preço Nota: R$ {Number(it.unitPrice).toFixed(2)}</span>
                          <span>•</span>
                          <span>Frete Item: R$ {Number(it.allocatedFreight).toFixed(2)}</span>
                        </div>

                        {/* Matched product indicator */}
                        <div className="pt-0.5">
                          {it.matchedProductId ? (
                            <div className="inline-flex items-center gap-1.5 text-xs text-emerald-800 bg-emerald-50 px-2 py-0.5 rounded-md border border-emerald-200">
                              <CheckCircle2 className="h-3 w-3 text-emerald-600" />
                              <span>Vinculado a: <strong>{it.matchedProductName}</strong> ({it.matchedProductSku})</span>
                              {it.conversionFactor > 1 && (
                                <span className="text-[10px] font-bold font-mono text-emerald-700">
                                  [Fator: 1 {it.supplierUom} = {it.conversionFactor} {it.internalUom} $\rightarrow$ Qtd Entrada: {Number(it.internalQuantity)} {it.internalUom}]
                                </span>
                              )}
                            </div>
                          ) : (
                            <span className="inline-flex items-center gap-1 text-[11px] font-semibold text-amber-700 bg-amber-50 px-2 py-0.5 rounded border border-amber-200">
                              Não vinculado ao catálogo
                            </span>
                          )}
                        </div>
                      </div>

                      {/* Right Landed Cost & Match Button */}
                      <div className="flex items-center gap-4">
                        <div className="text-right font-mono">
                          <span className="text-[10px] text-gray-400 block">Custo Unitário Landed</span>
                          <div className="text-sm font-bold text-emerald-700">
                            R$ {Number(it.finalLandedUnitCost).toFixed(2)}
                          </div>
                        </div>

                        {selectedInvoice.status !== 'STOCKED' && (
                          <button
                            onClick={() => openMatchModal(it)}
                            className="px-3 py-1.5 border border-indigo-600 text-indigo-700 hover:bg-indigo-50 rounded-lg text-xs font-semibold transition cursor-pointer"
                          >
                            {it.matchedProductId ? 'Alterar Vínculo' : 'Vincular Produto'}
                          </button>
                        )}
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          ) : (
            <div className="p-16 text-center text-gray-400 text-sm">
              Selecione uma nota fiscal para ver detalhes, de-para e custos.
            </div>
          )}
        </div>
      </div>

      {/* Item Matching Modal */}
      {matchingItem && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4 backdrop-blur-xs">
          <div className="w-full max-w-md rounded-2xl bg-white shadow-2xl p-6 border border-gray-100">
            <div className="flex items-center justify-between pb-3 border-b border-gray-100 mb-4">
              <h3 className="text-base font-bold text-gray-900">Vincular Produto ao Catálogo</h3>
              <button onClick={() => setMatchingItem(null)} className="text-gray-400 hover:text-gray-600 p-1">
                <X className="h-5 w-5" />
              </button>
            </div>

            <form onSubmit={handleSaveMatch} className="space-y-4">
              <div>
                <label className="text-xs text-gray-500 block mb-0.5">Item do Fornecedor:</label>
                <div className="font-semibold text-sm text-gray-900">{matchingItem.supplierItemDescription}</div>
                <div className="text-xs text-gray-400 font-mono">Código: {matchingItem.supplierItemCode}</div>
              </div>

              <div>
                <label className="block text-xs font-semibold text-gray-700 mb-1">
                  Produto Correspondente no Catálogo Interno *
                </label>
                <select
                  required
                  value={selectedCatalogProductId}
                  onChange={(e) => setSelectedCatalogProductId(e.target.value)}
                  className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm"
                >
                  <option value="">Selecione o produto interno...</option>
                  {products.map((p) => (
                    <option key={p.id} value={p.id}>
                      {p.sku} - {p.name} ({p.unitOfMeasure})
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-xs font-semibold text-gray-700 mb-1">
                  Fator de Conversão de Embalagem *
                </label>
                <input
                  type="number"
                  step="0.0001"
                  required
                  min="0.0001"
                  value={conversionFactor}
                  onChange={(e) => setConversionFactor(e.target.value)}
                  className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm font-mono"
                />
                <span className="text-[11px] text-gray-400">
                  Ex: Se o fornecedor faturou em Caixa (CX) e a caixa contém 12 unidades internas, digite 12.
                </span>
              </div>

              <div className="flex justify-end gap-3 pt-3 border-t border-gray-100">
                <button
                  type="button"
                  onClick={() => setMatchingItem(null)}
                  className="px-4 py-2 border border-gray-300 text-gray-700 rounded-lg text-xs font-medium"
                >
                  Cancelar
                </button>
                <button
                  type="submit"
                  className="px-4 py-2 bg-emerald-600 hover:bg-emerald-700 text-white rounded-lg text-xs font-semibold"
                >
                  Salvar Vínculo
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Finalize Stock Modal */}
      {finalizeModalOpen && selectedInvoice && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4 backdrop-blur-xs">
          <div className="w-full max-w-md rounded-2xl bg-white shadow-2xl p-6 border border-gray-100">
            <div className="flex items-center justify-between pb-3 border-b border-gray-100 mb-4">
              <h3 className="text-base font-bold text-gray-900">Finalizar Entrada no Estoque</h3>
              <button onClick={() => setFinalizeModalOpen(false)} className="text-gray-400 hover:text-gray-600 p-1">
                <X className="h-5 w-5" />
              </button>
            </div>

            <div className="space-y-4">
              <p className="text-xs text-gray-600">
                Ao finalizar, o sistema dará entrada física nas mercadorias, registrará os movimentos no Kardex e recalculará o <strong>Custo Médio Ponderado (CMP)</strong> dos produtos com os custos landed.
              </p>

              <div>
                <label className="block text-xs font-semibold text-gray-700 mb-1">
                  Depósito de Destino *
                </label>
                <select
                  required
                  value={selectedWarehouseId}
                  onChange={(e) => setSelectedWarehouseId(e.target.value)}
                  className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm"
                >
                  {warehouses.map((w) => (
                    <option key={w.id} value={w.id}>
                      {w.code} - {w.name}
                    </option>
                  ))}
                </select>
              </div>

              <div className="flex justify-end gap-3 pt-3 border-t border-gray-100">
                <button
                  type="button"
                  onClick={() => setFinalizeModalOpen(false)}
                  className="px-4 py-2 border border-gray-300 text-gray-700 rounded-lg text-xs font-medium"
                >
                  Cancelar
                </button>
                <button
                  type="button"
                  onClick={handleFinalizeStock}
                  className="px-4 py-2 bg-emerald-600 hover:bg-emerald-700 text-white rounded-lg text-xs font-bold"
                >
                  Confirmar Entrada & Estocar
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      <NFeXmlUploadModal
        isOpen={uploadModalOpen}
        onClose={() => setUploadModalOpen(false)}
        onSuccess={fetchInvoices}
        branchId={currentBranchId}
      />
    </div>
  );
};
