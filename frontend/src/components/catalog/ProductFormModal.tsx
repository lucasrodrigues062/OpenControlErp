import React, { useState, useEffect } from 'react';
import {
  Product,
  ProductCategory,
  CreateProductRequest,
  UpdateProductRequest,
  SpedItemType,
  ProductTrackingMode,
} from '../../types/catalog';
import { catalogApi } from '../../services/catalogApi';
import {
  X,
  Package,
  FileText,
  Truck,
  DollarSign,
  AlertCircle,
  Plus,
  Trash2,
  Sparkles,
} from 'lucide-react';
import { generateProductSku } from '../../utils/codeGenerator';

interface ProductFormModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSuccess: () => void;
  productToEdit?: Product | null;
  categories: ProductCategory[];
}

export const ProductFormModal: React.FC<ProductFormModalProps> = ({
  isOpen,
  onClose,
  onSuccess,
  productToEdit,
  categories,
}) => {
  const [activeTab, setActiveTab] = useState<'general' | 'fiscal' | 'logistics' | 'costs'>('general');
  const [loading, setLoading] = useState(false);
  const [errorMsg, setErrorMsg] = useState('');

  // General fields
  const [sku, setSku] = useState('');
  const [barcode, setBarcode] = useState('');
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [categoryId, setCategoryId] = useState('');
  const [brand, setBrand] = useState('');
  const [model, setModel] = useState('');
  const [unitOfMeasure, setUnitOfMeasure] = useState('UN');
  const [trackingMode, setTrackingMode] = useState<ProductTrackingMode>('NONE');
  const [isActive, setIsActive] = useState(true);

  // Fiscal fields
  const [itemType, setItemType] = useState<SpedItemType>('REV_00');
  const [ncm, setNcm] = useState('');
  const [ncmException, setNcmException] = useState('');
  const [cest, setCest] = useState('');
  const [origin, setOrigin] = useState<number>(0);

  // Logistics / Dimensions
  const [grossWeight, setGrossWeight] = useState<string>('');
  const [netWeight, setNetWeight] = useState<string>('');
  const [width, setWidth] = useState<string>('');
  const [height, setHeight] = useState<string>('');
  const [depth, setDepth] = useState<string>('');
  const [volume, setVolume] = useState<string>('');
  const [minStock, setMinStock] = useState<string>('0');
  const [maxStock, setMaxStock] = useState<string>('');
  const [reorderPoint, setReorderPoint] = useState<string>('');

  // Cost baseline
  const [lastAcquisitionCost, setLastAcquisitionCost] = useState<string>('0.00');

  // Conversions
  const [conversions, setConversions] = useState<{ unitFrom: string; unitTo: string; factor: string }[]>([]);
  const [newUnitFrom, setNewUnitFrom] = useState('');
  const [newFactor, setNewFactor] = useState('');

  useEffect(() => {
    if (productToEdit) {
      setSku(productToEdit.sku);
      setBarcode(productToEdit.barcode || '');
      setName(productToEdit.name);
      setDescription(productToEdit.description || '');
      setCategoryId(productToEdit.categoryId);
      setBrand(productToEdit.brand || '');
      setModel(productToEdit.model || '');
      setUnitOfMeasure(productToEdit.unitOfMeasure);
      setTrackingMode(productToEdit.trackingMode);
      setIsActive(productToEdit.isActive);

      setItemType(productToEdit.itemType);
      setNcm(productToEdit.ncm);
      setNcmException(productToEdit.ncmException || '');
      setCest(productToEdit.cest || '');
      setOrigin(productToEdit.origin);

      setGrossWeight(productToEdit.grossWeight?.toString() || '');
      setNetWeight(productToEdit.netWeight?.toString() || '');
      setWidth(productToEdit.width?.toString() || '');
      setHeight(productToEdit.height?.toString() || '');
      setDepth(productToEdit.depth?.toString() || '');
      setVolume(productToEdit.volume?.toString() || '');
      setMinStock(productToEdit.minStock?.toString() || '0');
      setMaxStock(productToEdit.maxStock?.toString() || '');
      setReorderPoint(productToEdit.reorderPoint?.toString() || '');
      setLastAcquisitionCost(productToEdit.lastAcquisitionCost?.toString() || '0.00');

      if (productToEdit.unitConversions) {
        setConversions(
          productToEdit.unitConversions.map((c) => ({
            unitFrom: c.unitFrom,
            unitTo: c.unitTo,
            factor: c.conversionFactor.toString(),
          }))
        );
      }
    } else {
      // Defaults
      setSku(generateProductSku());
      setBarcode('');
      setName('');
      setDescription('');
      setCategoryId(categories[0]?.id || '');
      setBrand('');
      setModel('');
      setUnitOfMeasure('UN');
      setTrackingMode('NONE');
      setIsActive(true);

      setItemType('REV_00');
      setNcm('');
      setNcmException('');
      setCest('');
      setOrigin(0);

      setGrossWeight('');
      setNetWeight('');
      setWidth('');
      setHeight('');
      setDepth('');
      setVolume('');
      setMinStock('0');
      setMaxStock('');
      setReorderPoint('');
      setLastAcquisitionCost('0.00');
      setConversions([]);
    }
    setErrorMsg('');
    setActiveTab('general');
  }, [productToEdit, isOpen, categories]);

  if (!isOpen) return null;

  const handleAddConversion = () => {
    if (!newUnitFrom.trim() || !newFactor || Number(newFactor) <= 0) return;
    setConversions([
      ...conversions,
      {
        unitFrom: newUnitFrom.trim().toUpperCase(),
        unitTo: unitOfMeasure.toUpperCase(),
        factor: newFactor,
      },
    ]);
    setNewUnitFrom('');
    setNewFactor('');
  };

  const handleRemoveConversion = (index: number) => {
    setConversions(conversions.filter((_, i) => i !== index));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMsg('');

    // Pre-flight validation
    const cleanNcm = ncm.replace(/\D/g, '');
    if (cleanNcm.length !== 8) {
      setErrorMsg('O NCM deve possuir exatamente 8 dígitos numéricos.');
      setActiveTab('fiscal');
      return;
    }

    const cleanCest = cest.replace(/\D/g, '');
    if (cleanCest && cleanCest.length !== 7) {
      setErrorMsg('O CEST deve possuir exatamente 7 dígitos numéricos.');
      setActiveTab('fiscal');
      return;
    }

    setLoading(true);
    try {
      if (productToEdit) {
        const payload: UpdateProductRequest = {
          sku: sku.trim().toUpperCase(),
          barcode: barcode.trim() || null,
          name: name.trim(),
          description: description.trim() || null,
          itemType,
          unitOfMeasure: unitOfMeasure.trim().toUpperCase(),
          categoryId,
          brand: brand.trim() || null,
          model: model.trim() || null,
          ncm: cleanNcm,
          ncmException: ncmException.trim() || null,
          cest: cleanCest || null,
          origin,
          trackingMode,
          grossWeight: grossWeight ? Number(grossWeight) : null,
          netWeight: netWeight ? Number(netWeight) : null,
          width: width ? Number(width) : null,
          height: height ? Number(height) : null,
          depth: depth ? Number(depth) : null,
          volume: volume ? Number(volume) : null,
          minStock: Number(minStock || '0'),
          maxStock: maxStock ? Number(maxStock) : null,
          reorderPoint: reorderPoint ? Number(reorderPoint) : null,
          lastAcquisitionCost: Number(lastAcquisitionCost || '0'),
          isActive,
        };
        await catalogApi.updateProduct(productToEdit.id, payload);

        // Save conversions
        for (const conv of conversions) {
          await catalogApi.addUnitConversion(productToEdit.id, {
            unitFrom: conv.unitFrom,
            unitTo: conv.unitTo,
            conversionFactor: Number(conv.factor),
          });
        }
      } else {
        const payload: CreateProductRequest = {
          sku: (sku.trim() || generateProductSku()).toUpperCase(),
          barcode: barcode.trim() || null,
          name: name.trim(),
          description: description.trim() || null,
          itemType,
          unitOfMeasure: unitOfMeasure.trim().toUpperCase(),
          categoryId,
          brand: brand.trim() || null,
          model: model.trim() || null,
          ncm: cleanNcm,
          ncmException: ncmException.trim() || null,
          cest: cleanCest || null,
          origin,
          trackingMode,
          grossWeight: grossWeight ? Number(grossWeight) : null,
          netWeight: netWeight ? Number(netWeight) : null,
          width: width ? Number(width) : null,
          height: height ? Number(height) : null,
          depth: depth ? Number(depth) : null,
          volume: volume ? Number(volume) : null,
          minStock: Number(minStock || '0'),
          maxStock: maxStock ? Number(maxStock) : null,
          reorderPoint: reorderPoint ? Number(reorderPoint) : null,
          lastAcquisitionCost: Number(lastAcquisitionCost || '0'),
        };
        const created = await catalogApi.createProduct(payload);

        // Save conversions
        for (const conv of conversions) {
          await catalogApi.addUnitConversion(created.id, {
            unitFrom: conv.unitFrom,
            unitTo: conv.unitTo,
            conversionFactor: Number(conv.factor),
          });
        }
      }

      onSuccess();
      onClose();
    } catch (err: any) {
      setErrorMsg(err.response?.data?.detail || err.message || 'Erro ao salvar produto');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 backdrop-blur-sm p-4 overflow-y-auto">
      <div className="bg-white rounded-2xl max-w-3xl w-full shadow-2xl border border-gray-100 flex flex-col max-h-[90vh]">
        {/* Header */}
        <div className="flex items-center justify-between p-6 border-b border-gray-100">
          <div className="flex items-center gap-2">
            <Package className="h-6 w-6 text-emerald-600" />
            <h2 className="text-xl font-bold text-gray-900">
              {productToEdit ? 'Editar Produto' : 'Novo Produto'}
            </h2>
          </div>
          <button onClick={onClose} className="text-gray-400 hover:text-gray-600 p-1.5 rounded-lg">
            <X className="h-5 w-5" />
          </button>
        </div>

        {/* Tab Navigation */}
        <div className="flex border-b border-gray-200 px-6 bg-gray-50/50">
          <button
            type="button"
            onClick={() => setActiveTab('general')}
            className={`flex items-center gap-2 py-3 px-4 text-sm font-semibold border-b-2 transition ${
              activeTab === 'general'
                ? 'border-emerald-600 text-emerald-600'
                : 'border-transparent text-gray-500 hover:text-gray-700'
            }`}
          >
            <Package className="h-4 w-4" />
            Geral
          </button>
          <button
            type="button"
            onClick={() => setActiveTab('fiscal')}
            className={`flex items-center gap-2 py-3 px-4 text-sm font-semibold border-b-2 transition ${
              activeTab === 'fiscal'
                ? 'border-emerald-600 text-emerald-600'
                : 'border-transparent text-gray-500 hover:text-gray-700'
            }`}
          >
            <FileText className="h-4 w-4" />
            Fiscal & Tributação
          </button>
          <button
            type="button"
            onClick={() => setActiveTab('logistics')}
            className={`flex items-center gap-2 py-3 px-4 text-sm font-semibold border-b-2 transition ${
              activeTab === 'logistics'
                ? 'border-emerald-600 text-emerald-600'
                : 'border-transparent text-gray-500 hover:text-gray-700'
            }`}
          >
            <Truck className="h-4 w-4" />
            Dimensões & Estoque
          </button>
          <button
            type="button"
            onClick={() => setActiveTab('costs')}
            className={`flex items-center gap-2 py-3 px-4 text-sm font-semibold border-b-2 transition ${
              activeTab === 'costs'
                ? 'border-emerald-600 text-emerald-600'
                : 'border-transparent text-gray-500 hover:text-gray-700'
            }`}
          >
            <DollarSign className="h-4 w-4" />
            Custos & Conversão
          </button>
        </div>

        {/* Error message */}
        {errorMsg && (
          <div className="mx-6 mt-4 p-3 bg-red-50 border border-red-200 text-red-700 text-sm rounded-lg flex items-center gap-2">
            <AlertCircle className="h-4 w-4 flex-shrink-0" />
            <span>{errorMsg}</span>
          </div>
        )}

        {/* Content Body */}
        <form onSubmit={handleSubmit} className="p-6 overflow-y-auto space-y-4 flex-1">
          {/* TAB: GENERAL */}
          {activeTab === 'general' && (
            <div className="space-y-4">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div>
                  <div className="flex items-center justify-between mb-1">
                    <label className="text-xs font-semibold text-gray-700">SKU / Código Interno *</label>
                    {!productToEdit && (
                      <button
                        type="button"
                        onClick={() => setSku(generateProductSku())}
                        className="text-xs text-emerald-600 hover:text-emerald-700 font-medium flex items-center gap-1 cursor-pointer"
                        title="Gerar novo código"
                      >
                        <Sparkles className="h-3 w-3" />
                        Gerar outro
                      </button>
                    )}
                  </div>
                  <input
                    type="text"
                    required
                    placeholder="EX: PRD-12345"
                    value={sku}
                    onChange={(e) => setSku(e.target.value.toUpperCase())}
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-emerald-500 focus:outline-none"
                  />
                  <span className="text-[11px] text-gray-400 mt-0.5 block">Gerado automaticamente. Você pode alterar se preferir.</span>
                </div>
                <div>
                  <label className="block text-xs font-semibold text-gray-700 mb-1">Código de Barras (EAN-13 / GTIN)</label>
                  <input
                    type="text"
                    placeholder="7891000315507"
                    value={barcode}
                    onChange={(e) => setBarcode(e.target.value.trim())}
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-emerald-500 focus:outline-none"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-semibold text-gray-700 mb-1">Nome Comercial do Produto *</label>
                <input
                  type="text"
                  required
                  placeholder="EX: Notebook Dell XPS 15 32GB RAM"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-emerald-500 focus:outline-none"
                />
              </div>

              <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                <div>
                  <label className="block text-xs font-semibold text-gray-700 mb-1">Categoria Principal *</label>
                  <select
                    required
                    value={categoryId}
                    onChange={(e) => setCategoryId(e.target.value)}
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-emerald-500 focus:outline-none"
                  >
                    <option value="">Selecione uma categoria...</option>
                    {categories.map((c) => (
                      <option key={c.id} value={c.id}>
                        {c.code} - {c.name}
                      </option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className="block text-xs font-semibold text-gray-700 mb-1">Unidade Comercial *</label>
                  <select
                    value={unitOfMeasure}
                    onChange={(e) => setUnitOfMeasure(e.target.value.toUpperCase())}
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-emerald-500 focus:outline-none"
                  >
                    <option value="UN">UN - Unidade</option>
                    <option value="CX">CX - Caixa</option>
                    <option value="PC">PC - Peça</option>
                    <option value="KG">KG - Quilograma</option>
                    <option value="LT">LT - Litro</option>
                    <option value="MT">MT - Metro</option>
                    <option value="FD">FD - Fardo</option>
                    <option value="PR">PR - Par</option>
                  </select>
                </div>
                <div>
                  <label className="block text-xs font-semibold text-gray-700 mb-1">Rastreabilidade no Estoque</label>
                  <select
                    value={trackingMode}
                    onChange={(e) => setTrackingMode(e.target.value as ProductTrackingMode)}
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-emerald-500 focus:outline-none"
                  >
                    <option value="NONE">Sem Rastreamento Específico</option>
                    <option value="BATCH_LOT">Controle por Lote e Validade (FEFO)</option>
                    <option value="SERIAL_NUMBER">Controle por Número de Série Unitário</option>
                  </select>
                </div>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-semibold text-gray-700 mb-1">Marca</label>
                  <input
                    type="text"
                    placeholder="EX: Dell, Apple, Samsung"
                    value={brand}
                    onChange={(e) => setBrand(e.target.value)}
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-emerald-500 focus:outline-none"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-gray-700 mb-1">Modelo</label>
                  <input
                    type="text"
                    placeholder="EX: XPS 9520"
                    value={model}
                    onChange={(e) => setModel(e.target.value)}
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-emerald-500 focus:outline-none"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-semibold text-gray-700 mb-1">Descrição Detalhada / Ficha Técnica</label>
                <textarea
                  rows={3}
                  placeholder="Informações técnicas, aplicações e especificações do produto..."
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-emerald-500 focus:outline-none"
                />
              </div>

              {productToEdit && (
                <div className="flex items-center gap-2 pt-2">
                  <input
                    type="checkbox"
                    id="productActive"
                    checked={isActive}
                    onChange={(e) => setIsActive(e.target.checked)}
                    className="rounded text-emerald-600 focus:ring-emerald-500 h-4 w-4"
                  />
                  <label htmlFor="productActive" className="text-sm font-medium text-gray-700">
                    Produto Ativo no Catálogo
                  </label>
                </div>
              )}
            </div>
          )}

          {/* TAB: FISCAL */}
          {activeTab === 'fiscal' && (
            <div className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-gray-700 mb-1">Tipo de Item SPED Fiscal (Tabela 0200) *</label>
                <select
                  value={itemType}
                  onChange={(e) => setItemType(e.target.value as SpedItemType)}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-emerald-500 focus:outline-none"
                >
                  <option value="REV_00">00 - Mercadoria para Revenda</option>
                  <option value="MP_01">01 - Matéria-Prima</option>
                  <option value="EMB_02">02 - Embalagem</option>
                  <option value="PP_03">03 - Produto em Processo</option>
                  <option value="PA_04">04 - Produto Acabado</option>
                  <option value="SP_05">05 - Subproduto</option>
                  <option value="PI_06">06 - Produto Intermediário</option>
                  <option value="UC_07">07 - Material de Uso e Consumo</option>
                  <option value="AI_08">08 - Ativo Imobilizado</option>
                  <option value="SRV_09">09 - Serviços</option>
                  <option value="OUT_10">10 - Outros Insumos</option>
                  <option value="OUT_99">99 - Outras</option>
                </select>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                <div>
                  <label className="block text-xs font-semibold text-gray-700 mb-1">Código NCM (8 Dígitos) *</label>
                  <input
                    type="text"
                    required
                    maxLength={10}
                    placeholder="8471.30.12"
                    value={ncm}
                    onChange={(e) => setNcm(e.target.value)}
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-emerald-500 focus:outline-none"
                  />
                  <span className="text-[11px] text-gray-400">Classificação fiscal obrigatória para NF-e</span>
                </div>
                <div>
                  <label className="block text-xs font-semibold text-gray-700 mb-1">Exceção NCM (EX TIPI)</label>
                  <input
                    type="text"
                    maxLength={3}
                    placeholder="01"
                    value={ncmException}
                    onChange={(e) => setNcmException(e.target.value)}
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-emerald-500 focus:outline-none"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-gray-700 mb-1">Código CEST (7 Dígitos)</label>
                  <input
                    type="text"
                    maxLength={9}
                    placeholder="01.001.00"
                    value={cest}
                    onChange={(e) => setCest(e.target.value)}
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-emerald-500 focus:outline-none"
                  />
                  <span className="text-[11px] text-gray-400">Obrigatório para mercadorias com ICMS-ST</span>
                </div>
              </div>

              <div>
                <label className="block text-xs font-semibold text-gray-700 mb-1">Origem da Mercadoria (Tabela Oficial) *</label>
                <select
                  value={origin}
                  onChange={(e) => setOrigin(Number(e.target.value))}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-emerald-500 focus:outline-none"
                >
                  <option value={0}>0 - Nacional, exceto as indicadas nos códigos 3, 4, 5 e 8</option>
                  <option value={1}>1 - Estrangeira - Importação direta, exceto a indicada no código 6</option>
                  <option value={2}>2 - Estrangeira - Adquirida no mercado interno, exceto a indicada no código 7</option>
                  <option value={3}>3 - Nacional, mercadoria ou bem com Conteúdo de Importação superior a 40% e inferior ou igual a 70%</option>
                  <option value={4}>4 - Nacional, cuja produção tenha sido feita em conformidade com os processos produtivos básicos (PPB)</option>
                  <option value={5}>5 - Nacional, mercadoria ou bem com Conteúdo de Importação inferior ou igual a 40%</option>
                  <option value={6}>6 - Estrangeira - Importação direta, sem similar nacional (CAMEX)</option>
                  <option value={7}>7 - Estrangeira - Adquirida no mercado interno, sem similar nacional (CAMEX)</option>
                  <option value={8}>8 - Nacional, mercadoria ou bem com Conteúdo de Importação superior a 70%</option>
                </select>
              </div>
            </div>
          )}

          {/* TAB: LOGISTICS */}
          {activeTab === 'logistics' && (
            <div className="space-y-4">
              <h4 className="text-xs font-bold text-gray-500 uppercase tracking-wider">Pesos e Volumes (Cálculo de Frete)</h4>
              <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                <div>
                  <label className="block text-xs font-semibold text-gray-700 mb-1">Peso Bruto (kg)</label>
                  <input
                    type="number"
                    step="0.0001"
                    placeholder="0.0000"
                    value={grossWeight}
                    onChange={(e) => setGrossWeight(e.target.value)}
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-emerald-500 focus:outline-none"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-gray-700 mb-1">Peso Líquido (kg)</label>
                  <input
                    type="number"
                    step="0.0001"
                    placeholder="0.0000"
                    value={netWeight}
                    onChange={(e) => setNetWeight(e.target.value)}
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-emerald-500 focus:outline-none"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-gray-700 mb-1">Largura (cm)</label>
                  <input
                    type="number"
                    step="0.01"
                    placeholder="0.00"
                    value={width}
                    onChange={(e) => setWidth(e.target.value)}
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-emerald-500 focus:outline-none"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-gray-700 mb-1">Altura (cm)</label>
                  <input
                    type="number"
                    step="0.01"
                    placeholder="0.00"
                    value={height}
                    onChange={(e) => setHeight(e.target.value)}
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-emerald-500 focus:outline-none"
                  />
                </div>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-semibold text-gray-700 mb-1">Profundidade / Comprimento (cm)</label>
                  <input
                    type="number"
                    step="0.01"
                    placeholder="0.00"
                    value={depth}
                    onChange={(e) => setDepth(e.target.value)}
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-emerald-500 focus:outline-none"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-gray-700 mb-1">Volume Cúbico (m³)</label>
                  <input
                    type="number"
                    step="0.0001"
                    placeholder="0.0000"
                    value={volume}
                    onChange={(e) => setVolume(e.target.value)}
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-emerald-500 focus:outline-none"
                  />
                </div>
              </div>

              <h4 className="text-xs font-bold text-gray-500 uppercase tracking-wider pt-3 border-t border-gray-100">
                Limiares de Segurança e Reposição
              </h4>
              <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                <div>
                  <label className="block text-xs font-semibold text-gray-700 mb-1">Estoque Mínimo</label>
                  <input
                    type="number"
                    step="0.0001"
                    value={minStock}
                    onChange={(e) => setMinStock(e.target.value)}
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-emerald-500 focus:outline-none"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-gray-700 mb-1">Ponto de Pedido</label>
                  <input
                    type="number"
                    step="0.0001"
                    placeholder="Alerta de compra"
                    value={reorderPoint}
                    onChange={(e) => setReorderPoint(e.target.value)}
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-emerald-500 focus:outline-none"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-gray-700 mb-1">Estoque Máximo</label>
                  <input
                    type="number"
                    step="0.0001"
                    placeholder="Capacidade máxima"
                    value={maxStock}
                    onChange={(e) => setMaxStock(e.target.value)}
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-emerald-500 focus:outline-none"
                  />
                </div>
              </div>
            </div>
          )}

          {/* TAB: COSTS & CONVERSION */}
          {activeTab === 'costs' && (
            <div className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-gray-700 mb-1">
                  Custo de Aquisição Inicial (R$) *
                </label>
                <input
                  type="number"
                  step="0.01"
                  required
                  value={lastAcquisitionCost}
                  onChange={(e) => setLastAcquisitionCost(e.target.value)}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm font-semibold focus:ring-2 focus:ring-emerald-500 focus:outline-none"
                />
                <span className="text-[11px] text-gray-400">
                  Base inicial para o Custo Médio Ponderado (CMP) no estoque
                </span>
              </div>

              <div className="pt-3 border-t border-gray-100">
                <h4 className="text-xs font-bold text-gray-500 uppercase tracking-wider mb-2">
                  Conversões de Embalagem para Compra / Armazenamento
                </h4>
                <p className="text-xs text-gray-500 mb-3">
                  Permite comprar em fardo ou caixa e dar entrada automática na unidade principal ({unitOfMeasure}).
                </p>

                <div className="flex items-center gap-2 mb-3">
                  <div className="w-28">
                    <input
                      type="text"
                      placeholder="Unidade (CX)"
                      value={newUnitFrom}
                      onChange={(e) => setNewUnitFrom(e.target.value.toUpperCase())}
                      className="w-full px-3 py-1.5 border border-gray-300 rounded-lg text-xs"
                    />
                  </div>
                  <span className="text-xs text-gray-400 font-bold">=</span>
                  <div className="w-28">
                    <input
                      type="number"
                      step="0.000001"
                      placeholder="Fator (ex: 12)"
                      value={newFactor}
                      onChange={(e) => setNewFactor(e.target.value)}
                      className="w-full px-3 py-1.5 border border-gray-300 rounded-lg text-xs"
                    />
                  </div>
                  <span className="text-xs text-gray-600 font-semibold">{unitOfMeasure}</span>
                  <button
                    type="button"
                    onClick={handleAddConversion}
                    className="p-1.5 bg-emerald-100 text-emerald-700 hover:bg-emerald-200 rounded-lg transition"
                  >
                    <Plus className="h-4 w-4" />
                  </button>
                </div>

                {conversions.length > 0 && (
                  <div className="space-y-1.5">
                    {conversions.map((c, i) => (
                      <div
                        key={i}
                        className="flex items-center justify-between p-2 bg-gray-50 rounded-lg border border-gray-200 text-xs"
                      >
                        <span className="font-medium text-gray-700">
                          1 {c.unitFrom} = {c.factor} {c.unitTo}
                        </span>
                        <button
                          type="button"
                          onClick={() => handleRemoveConversion(i)}
                          className="text-red-500 hover:text-red-700"
                        >
                          <Trash2 className="h-3.5 w-3.5" />
                        </button>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </div>
          )}

          {/* Footer Actions */}
          <div className="flex items-center justify-end gap-3 pt-6 border-t border-gray-100 mt-6">
            <button
              type="button"
              onClick={onClose}
              disabled={loading}
              className="px-4 py-2 border border-gray-300 text-gray-700 rounded-lg text-sm font-medium hover:bg-gray-50 transition"
            >
              Cancelar
            </button>
            <button
              type="submit"
              disabled={loading}
              className="px-5 py-2 bg-emerald-600 hover:bg-emerald-700 text-white rounded-lg text-sm font-medium shadow-sm transition disabled:opacity-50"
            >
              {loading ? 'Salvando...' : 'Salvar Produto'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
