import React, { useState, useEffect } from 'react';
import {
  PriceList,
  CreatePriceListRequest,
  SetProductPriceRequest,
  Product,
  ProductPrice,
} from '../../types/catalog';
import { catalogApi } from '../../services/catalogApi';
import {
  BadgePercent,
  Plus,
  DollarSign,
  Calendar,
  Tag,
  Calculator,
  X,
  TrendingUp,
  Sparkles,
} from 'lucide-react';
import { generatePriceListCode } from '../../utils/codeGenerator';

export const PriceListsPage: React.FC = () => {
  const [priceLists, setPriceLists] = useState<PriceList[]>([]);
  const [selectedList, setSelectedList] = useState<PriceList | null>(null);
  const [products, setProducts] = useState<Product[]>([]);
  const [loading, setLoading] = useState(false);
  const [pricesMap, setPricesMap] = useState<Record<string, ProductPrice>>({});
  const [loadingPrices, setLoadingPrices] = useState(false);
  const [successToast, setSuccessToast] = useState<string | null>(null);

  // Price List creation modal
  const [modalOpen, setModalOpen] = useState(false);
  const [code, setCode] = useState('');
  const [name, setName] = useState('');
  const [currency, setCurrency] = useState('BRL');
  const [markupPct, setMarkupPct] = useState('0');
  const [isDefault, setIsDefault] = useState(false);
  const [validFrom, setValidFrom] = useState('');
  const [validUntil, setValidUntil] = useState('');
  const [errorMsg, setErrorMsg] = useState('');

  // Set Product Price Modal
  const [priceModalOpen, setPriceModalOpen] = useState(false);
  const [selectedProduct, setSelectedProduct] = useState<Product | null>(null);
  const [salesPrice, setSalesPrice] = useState('');
  const [promotionalPrice, setPromotionalPrice] = useState('');
  const [promoStart, setPromoStart] = useState('');
  const [promoEnd, setPromoEnd] = useState('');
  const [priceError, setPriceError] = useState('');

  // Markup Calculator widget state
  const [calcCost, setCalcCost] = useState('100.00');
  const [calcMarkup, setCalcMarkup] = useState('50');

  const fetchPriceLists = async () => {
    setLoading(true);
    try {
      const data = await catalogApi.listPriceLists();
      setPriceLists(data);
      if (data.length > 0 && !selectedList) {
        setSelectedList(data[0]);
      }
    } catch (err) {
      console.error('Error fetching price lists:', err);
    } finally {
      setLoading(false);
    }
  };

  const fetchProducts = async () => {
    try {
      const res = await catalogApi.searchProducts({ size: 100 });
      setProducts(res.items);
    } catch (err) {
      console.error('Error fetching products:', err);
    }
  };

  const fetchPricesForList = async (listId: string) => {
    setLoadingPrices(true);
    try {
      const listPrices = await catalogApi.getPriceListPrices(listId);
      const map: Record<string, ProductPrice> = {};
      listPrices.forEach((lp) => {
        map[lp.productId] = lp;
      });
      setPricesMap(map);
    } catch (err) {
      console.error('Error fetching prices for list:', err);
    } finally {
      setLoadingPrices(false);
    }
  };

  useEffect(() => {
    fetchPriceLists();
    fetchProducts();
  }, []);

  useEffect(() => {
    if (selectedList) {
      fetchPricesForList(selectedList.id);
    } else {
      setPricesMap({});
    }
  }, [selectedList]);

  const openCreatePriceListModal = () => {
    setCode(generatePriceListCode());
    setName('');
    setMarkupPct('0');
    setIsDefault(false);
    setValidFrom('');
    setValidUntil('');
    setErrorMsg('');
    setModalOpen(true);
  };

  const handleCreatePriceList = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMsg('');
    try {
      const payload: CreatePriceListRequest = {
        code: (code.trim() || generatePriceListCode()).toUpperCase(),
        name: name.trim(),
        currency,
        discountOrMarkupPct: Number(markupPct || '0'),
        isDefault,
        validFrom: validFrom || null,
        validUntil: validUntil || null,
      };
      const created = await catalogApi.createPriceList(payload);
      setModalOpen(false);
      setCode('');
      setName('');
      setMarkupPct('0');
      setIsDefault(false);
      setValidFrom('');
      setValidUntil('');
      await fetchPriceLists();
      setSelectedList(created);
    } catch (err: any) {
      setErrorMsg(err.response?.data?.detail || err.message || 'Erro ao criar tabela de preços');
    }
  };

  const openSetPriceModal = (p: Product) => {
    setSelectedProduct(p);
    const existing = pricesMap[p.id];
    if (existing) {
      setSalesPrice(existing.salesPrice?.toString() || '');
      setPromotionalPrice(existing.promotionalPrice?.toString() || '');
      setPromoStart(existing.promoStart || '');
      setPromoEnd(existing.promoEnd || '');
    } else {
      // Pre-fill with markup suggestion if not set
      const cost = Number(p.averageCost || p.lastAcquisitionCost || 0);
      const suggested = (cost * (1 + Number(selectedList?.discountOrMarkupPct || 0) / 100)).toFixed(2);
      setSalesPrice(Number(suggested) > 0 ? suggested : '');
      setPromotionalPrice('');
      setPromoStart('');
      setPromoEnd('');
    }
    setPriceError('');
    setPriceModalOpen(true);
  };

  const handleSaveProductPrice = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedList || !selectedProduct) return;
    setPriceError('');
    try {
      const payload: SetProductPriceRequest = {
        salesPrice: Number(salesPrice),
        promotionalPrice: promotionalPrice ? Number(promotionalPrice) : null,
        promoStart: promoStart || null,
        promoEnd: promoEnd || null,
      };
      const saved = await catalogApi.setProductPrice(selectedList.id, selectedProduct.id, payload);
      setPricesMap((prev) => ({
        ...prev,
        [selectedProduct.id]: saved,
      }));
      setPriceModalOpen(false);
      setSuccessToast(`Preço do produto "${selectedProduct.name}" atualizado com sucesso!`);
      setTimeout(() => setSuccessToast(null), 4000);
    } catch (err: any) {
      setPriceError(err.response?.data?.detail || err.message || 'Erro ao salvar preço do produto');
    }
  };

  const calculatedSuggestedPrice = () => {
    const cost = Number(calcCost || '0');
    const markup = Number(calcMarkup || '0');
    return (cost * (1 + markup / 100)).toFixed(2);
  };

  return (
    <div className="p-6 space-y-6">
      {/* Toast Alert */}
      {successToast && (
        <div className="fixed top-5 right-5 z-50 flex items-center gap-2 bg-emerald-600 text-white px-4 py-3 rounded-xl shadow-lg animate-fade-in text-sm font-medium">
          <Sparkles className="h-4 w-4" />
          <span>{successToast}</span>
        </div>
      )}

      {/* Top Header */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 flex items-center gap-2">
            <BadgePercent className="h-7 w-7 text-emerald-600" />
            Tabelas de Preço & Formação de Custos
          </h1>
          <p className="text-sm text-gray-500 mt-1">
            Gestão de margens de markup, tabelas comerciais diferenciadas (Varejo / Atacado) e campanhas promocionais com agendamento
          </p>
        </div>
        <button
          onClick={openCreatePriceListModal}
          className="inline-flex items-center gap-2 px-4 py-2 bg-emerald-600 hover:bg-emerald-700 text-white font-medium rounded-lg shadow-sm transition cursor-pointer"
        >
          <Plus className="h-4 w-4" />
          Nova Tabela de Preços
        </button>
      </div>

      {/* Main Layout Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Left Column: Price Lists & Markup Calculator */}
        <div className="space-y-6">
          {/* Price Lists Sidebar */}
          <div className="bg-white rounded-xl border border-gray-200 p-5 shadow-sm space-y-3">
            <h3 className="text-sm font-bold text-gray-800 uppercase tracking-wider flex items-center gap-2">
              <Tag className="h-4 w-4 text-emerald-600" />
              Tabelas Comerciais
            </h3>

            {loading ? (
              <div className="p-4 text-xs text-gray-400 text-center">Carregando tabelas...</div>
            ) : priceLists.length === 0 ? (
              <div className="p-6 text-center text-xs text-gray-500">
                Nenhuma tabela de preços cadastrada. Crie a tabela padrão (ex: Varejo).
              </div>
            ) : (
              <div className="space-y-2">
                {priceLists.map((list) => {
                  const isSelected = selectedList?.id === list.id;
                  return (
                    <button
                      key={list.id}
                      onClick={() => setSelectedList(list)}
                      className={`w-full text-left p-3.5 rounded-xl border transition flex items-center justify-between ${
                        isSelected
                          ? 'border-emerald-500 bg-emerald-50/50 shadow-xs ring-1 ring-emerald-500'
                          : 'border-gray-200 hover:border-gray-300 hover:bg-gray-50'
                      }`}
                    >
                      <div>
                        <div className="flex items-center gap-2">
                          <span className="font-mono text-xs font-bold text-gray-900">{list.code}</span>
                          {list.isDefault && (
                            <span className="text-[10px] font-semibold bg-emerald-100 text-emerald-800 px-1.5 py-0.2 rounded">
                              Padrão
                            </span>
                          )}
                        </div>
                        <h4 className="text-sm font-semibold text-gray-800 mt-0.5">{list.name}</h4>
                        <span className="text-xs text-gray-500">
                          Margem base: <strong>{Number(list.discountOrMarkupPct)}%</strong>
                        </span>
                      </div>
                      <span className="font-bold text-sm text-gray-700">{list.currency}</span>
                    </button>
                  );
                })}
              </div>
            )}
          </div>

          {/* Real-time Markup Calculator */}
          <div className="bg-gradient-to-br from-emerald-900 to-teal-950 rounded-xl p-5 text-white shadow-md space-y-4">
            <div className="flex items-center gap-2">
              <Calculator className="h-5 w-5 text-emerald-400" />
              <h3 className="text-sm font-bold tracking-wide">Simulador de Markup</h3>
            </div>
            <p className="text-xs text-emerald-200/80">
              Cálculo instantâneo de preço sugerido aplicando percentual de markup sobre o Custo Médio Ponderado (CMP).
            </p>

            <div className="grid grid-cols-2 gap-3">
              <div>
                <label className="block text-[11px] font-semibold text-emerald-200 mb-1">Custo Médio (R$)</label>
                <input
                  type="number"
                  step="0.01"
                  value={calcCost}
                  onChange={(e) => setCalcCost(e.target.value)}
                  className="w-full px-2.5 py-1.5 bg-white/10 border border-emerald-500/40 rounded-lg text-sm text-white font-mono focus:outline-none focus:ring-1 focus:ring-emerald-400"
                />
              </div>
              <div>
                <label className="block text-[11px] font-semibold text-emerald-200 mb-1">Markup Alvo (%)</label>
                <input
                  type="number"
                  step="0.1"
                  value={calcMarkup}
                  onChange={(e) => setCalcMarkup(e.target.value)}
                  className="w-full px-2.5 py-1.5 bg-white/10 border border-emerald-500/40 rounded-lg text-sm text-white font-mono focus:outline-none focus:ring-1 focus:ring-emerald-400"
                />
              </div>
            </div>

            <div className="p-3 bg-white/10 rounded-xl border border-white/15 flex items-center justify-between">
              <div>
                <span className="text-[11px] uppercase tracking-wider text-emerald-300 font-semibold">Preço Sugerido</span>
                <div className="text-xl font-bold font-mono text-emerald-300">
                  R$ {calculatedSuggestedPrice()}
                </div>
              </div>
              <TrendingUp className="h-7 w-7 text-emerald-400" />
            </div>
          </div>
        </div>

        {/* Right Column: Products in the Selected Price List */}
        <div className="lg:col-span-2 bg-white rounded-xl border border-gray-200 p-5 shadow-sm flex flex-col justify-between">
          <div>
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-4 border-b border-gray-100">
              <div>
                <h3 className="text-base font-bold text-gray-900 flex items-center gap-2">
                  <DollarSign className="h-5 w-5 text-emerald-600" />
                  Preços para: {selectedList ? selectedList.name : 'Selecione uma tabela'}
                </h3>
                <p className="text-xs text-gray-500 mt-0.5">
                  Moeda: {selectedList?.currency || 'BRL'} | Margem sugerida: {selectedList?.discountOrMarkupPct || 0}%
                </p>
              </div>
            </div>

            {products.length === 0 ? (
              <div className="p-12 text-center text-gray-400 text-sm">
                Nenhum produto cadastrado no catálogo para precificação.
              </div>
            ) : (
              <div className="mt-4 divide-y divide-gray-100 max-h-[600px] overflow-y-auto">
                {loadingPrices ? (
                  <div className="p-8 text-center text-xs text-gray-400">Atualizando preços da tabela...</div>
                ) : products.map((p) => {
                  const currentPrice = pricesMap[p.id];
                  const cost = Number(p.averageCost || p.lastAcquisitionCost || 0);
                  const suggested = (
                    cost * (1 + Number(selectedList?.discountOrMarkupPct || 0) / 100)
                  ).toFixed(2);

                  return (
                    <div key={p.id} className="py-3 flex items-center justify-between hover:bg-gray-50/60 px-2 rounded-lg transition">
                      <div>
                        <div className="flex items-center gap-2">
                          <span className="font-semibold text-gray-900 text-sm">{p.name}</span>
                          {currentPrice ? (
                            <span className="inline-flex items-center px-1.5 py-0.5 rounded text-[10px] font-semibold bg-emerald-100 text-emerald-800">
                              Preço Ativo
                            </span>
                          ) : (
                            <span className="inline-flex items-center px-1.5 py-0.5 rounded text-[10px] font-medium bg-amber-50 text-amber-700 border border-amber-200">
                              Não precificado
                            </span>
                          )}
                        </div>
                        <div className="flex items-center gap-2 text-xs text-gray-500 mt-0.5 font-mono">
                          <span className="bg-gray-100 px-1.5 py-0.5 rounded text-[11px] font-bold text-gray-700">{p.sku}</span>
                          <span>Custo CMP: R$ {cost.toFixed(2)}</span>
                          <span className="text-emerald-700 font-semibold">Sugerido: R$ {suggested}</span>
                        </div>
                      </div>

                      <div className="flex items-center gap-4">
                        {currentPrice ? (
                          <div className="text-right">
                            <div className="text-sm font-bold text-emerald-700 font-mono">
                              {selectedList?.currency || 'R$'} {Number(currentPrice.effectivePrice ?? currentPrice.salesPrice).toFixed(2)}
                            </div>
                            {currentPrice.isPromotionalActive ? (
                              <div className="text-[10px] text-amber-600 font-semibold flex items-center justify-end gap-1">
                                <span className="line-through text-gray-400">
                                  R$ {Number(currentPrice.salesPrice).toFixed(2)}
                                </span>
                                <span>Promoção</span>
                              </div>
                            ) : (
                              <div className="text-[10px] text-gray-400">Preço Regular</div>
                            )}
                          </div>
                        ) : (
                          <div className="text-right">
                            <div className="text-xs text-gray-400 font-mono italic">Sem preço fixo</div>
                          </div>
                        )}

                        <button
                          onClick={() => openSetPriceModal(p)}
                          className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition cursor-pointer ${
                            currentPrice
                              ? 'border border-gray-300 text-gray-700 hover:bg-gray-100'
                              : 'border border-emerald-600 text-emerald-700 hover:bg-emerald-50'
                          }`}
                        >
                          {currentPrice ? 'Alterar Preço' : 'Configurar Preço'}
                        </button>
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Modal New Price List */}
      {modalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 backdrop-blur-sm p-4">
          <div className="bg-white rounded-2xl max-w-md w-full p-6 shadow-xl border border-gray-100">
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-lg font-bold text-gray-900">Nova Tabela de Preços</h2>
              <button onClick={() => setModalOpen(false)} className="text-gray-400 hover:text-gray-600 p-1">
                <X className="h-5 w-5" />
              </button>
            </div>

            {errorMsg && (
              <div className="mb-4 p-3 bg-red-50 border border-red-200 text-red-700 text-xs rounded-lg">
                {errorMsg}
              </div>
            )}

            <form onSubmit={handleCreatePriceList} className="space-y-4">
              <div>
                <div className="flex items-center justify-between mb-1">
                  <label className="text-xs font-semibold text-gray-700">Código da Tabela *</label>
                  <button
                    type="button"
                    onClick={() => setCode(generatePriceListCode())}
                    className="text-xs text-emerald-600 hover:text-emerald-700 font-medium flex items-center gap-1 cursor-pointer"
                    title="Gerar novo código"
                  >
                    <Sparkles className="h-3 w-3" />
                    Gerar outro
                  </button>
                </div>
                <input
                  type="text"
                  required
                  placeholder="EX: TAB-01"
                  value={code}
                  onChange={(e) => setCode(e.target.value.toUpperCase())}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm font-mono focus:ring-2 focus:ring-emerald-500 focus:outline-none"
                />
                <span className="text-[11px] text-gray-400 mt-0.5 block">Gerado automaticamente. Você pode alterar se desejar.</span>
              </div>

              <div>
                <label className="block text-xs font-semibold text-gray-700 mb-1">Nome da Tabela *</label>
                <input
                  type="text"
                  required
                  placeholder="EX: Tabela Varejo Consumidor Final"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-emerald-500 focus:outline-none"
                />
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-semibold text-gray-700 mb-1">Moeda *</label>
                  <select
                    value={currency}
                    onChange={(e) => setCurrency(e.target.value.toUpperCase())}
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-emerald-500 focus:outline-none"
                  >
                    <option value="BRL">BRL (R$)</option>
                    <option value="USD">USD ($)</option>
                    <option value="EUR">EUR (€)</option>
                  </select>
                </div>
                <div>
                  <label className="block text-xs font-semibold text-gray-700 mb-1">Margem / Markup Base (%)</label>
                  <input
                    type="number"
                    step="0.01"
                    value={markupPct}
                    onChange={(e) => setMarkupPct(e.target.value)}
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-emerald-500 focus:outline-none"
                  />
                </div>
              </div>

              <div className="flex items-center gap-2 pt-2">
                <input
                  type="checkbox"
                  id="defaultList"
                  checked={isDefault}
                  onChange={(e) => setIsDefault(e.target.checked)}
                  className="rounded text-emerald-600 focus:ring-emerald-500 h-4 w-4"
                />
                <label htmlFor="defaultList" className="text-sm font-medium text-gray-700">
                  Definir como Tabela Comercial Padrão
                </label>
              </div>

              <div className="flex items-center justify-end gap-3 pt-4 border-t border-gray-100">
                <button
                  type="button"
                  onClick={() => setModalOpen(false)}
                  className="px-4 py-2 border border-gray-300 text-gray-700 rounded-lg text-sm font-medium hover:bg-gray-50"
                >
                  Cancelar
                </button>
                <button
                  type="submit"
                  className="px-4 py-2 bg-emerald-600 hover:bg-emerald-700 text-white rounded-lg text-sm font-medium shadow-sm"
                >
                  Salvar Tabela
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Modal Set Product Price */}
      {priceModalOpen && selectedProduct && selectedList && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 backdrop-blur-sm p-4">
          <div className="bg-white rounded-2xl max-w-md w-full p-6 shadow-xl border border-gray-100">
            <div className="flex items-center justify-between mb-4">
              <div>
                <h2 className="text-lg font-bold text-gray-900">Preço do Produto</h2>
                <p className="text-xs text-gray-500">{selectedProduct.name} ({selectedProduct.sku})</p>
              </div>
              <button onClick={() => setPriceModalOpen(false)} className="text-gray-400 hover:text-gray-600 p-1">
                <X className="h-5 w-5" />
              </button>
            </div>

            {priceError && (
              <div className="mb-4 p-3 bg-red-50 border border-red-200 text-red-700 text-xs rounded-lg">
                {priceError}
              </div>
            )}

            <form onSubmit={handleSaveProductPrice} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-gray-700 mb-1">
                  Preço de Venda Regular ({selectedList.currency}) *
                </label>
                <input
                  type="number"
                  step="0.01"
                  required
                  value={salesPrice}
                  onChange={(e) => setSalesPrice(e.target.value)}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm font-bold text-gray-900 focus:ring-2 focus:ring-emerald-500 focus:outline-none"
                />
              </div>

              <div className="p-3 bg-emerald-50 rounded-xl border border-emerald-100 space-y-3">
                <h4 className="text-xs font-bold text-emerald-800 uppercase tracking-wider flex items-center gap-1.5">
                  <Calendar className="h-3.5 w-3.5" />
                  Campanha Promocional Agendada (Opcional)
                </h4>

                <div>
                  <label className="block text-[11px] font-semibold text-gray-700 mb-1">Preço Promocional (R$)</label>
                  <input
                    type="number"
                    step="0.01"
                    placeholder="Deixar em branco se não houver promoção"
                    value={promotionalPrice}
                    onChange={(e) => setPromotionalPrice(e.target.value)}
                    className="w-full px-3 py-1.5 border border-gray-300 rounded-lg text-xs bg-white"
                  />
                </div>

                <div className="grid grid-cols-2 gap-2">
                  <div>
                    <label className="block text-[10px] font-semibold text-gray-600 mb-0.5">Início da Promoção</label>
                    <input
                      type="date"
                      value={promoStart}
                      onChange={(e) => setPromoStart(e.target.value)}
                      className="w-full px-2 py-1 border border-gray-300 rounded-lg text-xs bg-white"
                    />
                  </div>
                  <div>
                    <label className="block text-[10px] font-semibold text-gray-600 mb-0.5">Fim da Promoção</label>
                    <input
                      type="date"
                      value={promoEnd}
                      onChange={(e) => setPromoEnd(e.target.value)}
                      className="w-full px-2 py-1 border border-gray-300 rounded-lg text-xs bg-white"
                    />
                  </div>
                </div>
              </div>

              <div className="flex items-center justify-end gap-3 pt-4 border-t border-gray-100">
                <button
                  type="button"
                  onClick={() => setPriceModalOpen(false)}
                  className="px-4 py-2 border border-gray-300 text-gray-700 rounded-lg text-sm font-medium hover:bg-gray-50"
                >
                  Cancelar
                </button>
                <button
                  type="submit"
                  className="px-4 py-2 bg-emerald-600 hover:bg-emerald-700 text-white rounded-lg text-sm font-medium shadow-sm"
                >
                  Salvar Preço
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
