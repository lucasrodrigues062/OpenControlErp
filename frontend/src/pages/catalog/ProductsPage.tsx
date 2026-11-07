import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Product, ProductCategory, SpedItemType, ProductTrackingMode } from '../../types/catalog';
import { catalogApi } from '../../services/catalogApi';
import { ProductFormModal } from '../../components/catalog/ProductFormModal';
import {
  Package,
  Plus,
  Search,
  Barcode,
  FolderTree,
  Edit2,
  Trash2,
  ChevronLeft,
  ChevronRight,
  Layers,
  CheckCircle2,
  XCircle,
} from 'lucide-react';

export const ProductsPage: React.FC = () => {
  const navigate = useNavigate();

  const [products, setProducts] = useState<Product[]>([]);
  const [categories, setCategories] = useState<ProductCategory[]>([]);
  const [loading, setLoading] = useState(false);

  // Filters
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedCategory, setSelectedCategory] = useState('');
  const [selectedTracking, setSelectedTracking] = useState<string>('');

  // Pagination
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [totalElements, setTotalElements] = useState(0);
  const pageSize = 15;

  // Modal
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [productToEdit, setProductToEdit] = useState<Product | null>(null);

  const fetchCategories = async () => {
    try {
      const data = await catalogApi.listCategories({ activeOnly: true });
      setCategories(data);
    } catch (err) {
      console.error('Error fetching categories:', err);
    }
  };

  const fetchProducts = async () => {
    setLoading(true);
    try {
      const response = await catalogApi.searchProducts({
        query: searchQuery || undefined,
        categoryId: selectedCategory || undefined,
        trackingMode: selectedTracking || undefined,
        page,
        size: pageSize,
      });
      setProducts(response.items);
      setTotalPages(response.totalPages);
      setTotalElements(response.totalElements);
    } catch (err) {
      console.error('Error fetching products:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchCategories();
  }, []);

  useEffect(() => {
    fetchProducts();
  }, [page, selectedCategory, selectedTracking]);

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setPage(0);
    fetchProducts();
  };

  const openCreateModal = () => {
    setProductToEdit(null);
    setIsModalOpen(true);
  };

  const openEditModal = (product: Product) => {
    setProductToEdit(product);
    setIsModalOpen(true);
  };

  const handleDelete = async (id: string, name: string) => {
    if (window.confirm(`Deseja desativar o produto "${name}"?`)) {
      try {
        await catalogApi.deleteProduct(id);
        fetchProducts();
      } catch (err: any) {
        alert(err.response?.data?.detail || 'Erro ao desativar produto');
      }
    }
  };

  const formatSpedBadge = (itemType: SpedItemType) => {
    switch (itemType) {
      case 'REV_00':
        return <span className="px-2 py-0.5 rounded text-[11px] font-semibold bg-blue-50 text-blue-700">00 Revenda</span>;
      case 'MP_01':
        return <span className="px-2 py-0.5 rounded text-[11px] font-semibold bg-amber-50 text-amber-700">01 Matéria-Prima</span>;
      case 'PA_04':
        return <span className="px-2 py-0.5 rounded text-[11px] font-semibold bg-emerald-50 text-emerald-700">04 Acabado</span>;
      case 'SRV_09':
        return <span className="px-2 py-0.5 rounded text-[11px] font-semibold bg-purple-50 text-purple-700">09 Serviço</span>;
      default:
        return <span className="px-2 py-0.5 rounded text-[11px] font-semibold bg-gray-100 text-gray-700">{itemType}</span>;
    }
  };

  const formatTrackingBadge = (mode: ProductTrackingMode) => {
    if (mode === 'BATCH_LOT') {
      return (
        <span className="inline-flex items-center gap-1 text-[11px] font-medium text-amber-700 bg-amber-50 px-2 py-0.5 rounded-full border border-amber-200">
          Lote / Validade
        </span>
      );
    }
    if (mode === 'SERIAL_NUMBER') {
      return (
        <span className="inline-flex items-center gap-1 text-[11px] font-medium text-indigo-700 bg-indigo-50 px-2 py-0.5 rounded-full border border-indigo-200">
          Nº de Série
        </span>
      );
    }
    return null;
  };

  return (
    <div className="p-6 space-y-6">
      {/* Top Header */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 flex items-center gap-2">
            <Package className="h-7 w-7 text-emerald-600" />
            Catálogo de Produtos & Serviços
          </h1>
          <p className="text-sm text-gray-500 mt-1">
            Itens comerciais e industriais, parametrização fiscal brasileira (NCM/CEST/SPED 0200) e estoque
          </p>
        </div>
        <div className="flex items-center gap-3">
          <button
            onClick={() => navigate('/catalog/categories')}
            className="inline-flex items-center gap-2 px-3.5 py-2 border border-gray-300 hover:bg-gray-50 text-gray-700 text-sm font-medium rounded-lg transition"
          >
            <FolderTree className="h-4 w-4 text-gray-500" />
            Categorias
          </button>
          <button
            onClick={openCreateModal}
            className="inline-flex items-center gap-2 px-4 py-2 bg-emerald-600 hover:bg-emerald-700 text-white font-medium rounded-lg shadow-sm transition"
          >
            <Plus className="h-4 w-4" />
            Novo Produto
          </button>
        </div>
      </div>

      {/* Filter & Barcode Search Bar */}
      <div className="bg-white p-4 rounded-xl border border-gray-200 shadow-sm space-y-3">
        <form onSubmit={handleSearchSubmit} className="flex flex-col md:flex-row gap-3">
          <div className="flex-1 flex items-center gap-2 px-3 py-2 bg-gray-50 border border-gray-200 rounded-lg">
            <Search className="h-4 w-4 text-gray-400" />
            <input
              type="text"
              placeholder="Buscar por nome, SKU ou escanear código de barras (EAN)..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="flex-1 bg-transparent border-none outline-none text-sm text-gray-800 placeholder-gray-400"
            />
            <Barcode className="h-5 w-5 text-gray-400" />
          </div>

          <div className="flex items-center gap-3">
            <select
              value={selectedCategory}
              onChange={(e) => {
                setSelectedCategory(e.target.value);
                setPage(0);
              }}
              className="px-3 py-2 border border-gray-300 rounded-lg text-sm text-gray-700 focus:outline-none focus:ring-2 focus:ring-emerald-500 bg-white"
            >
              <option value="">Todas as Categorias</option>
              {categories.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.code} - {c.name}
                </option>
              ))}
            </select>

            <select
              value={selectedTracking}
              onChange={(e) => {
                setSelectedTracking(e.target.value);
                setPage(0);
              }}
              className="px-3 py-2 border border-gray-300 rounded-lg text-sm text-gray-700 focus:outline-none focus:ring-2 focus:ring-emerald-500 bg-white"
            >
              <option value="">Rastreabilidade (Todas)</option>
              <option value="NONE">Estoque Padrão</option>
              <option value="BATCH_LOT">Lote / Validade (FEFO)</option>
              <option value="SERIAL_NUMBER">Número de Série</option>
            </select>

            <button
              type="submit"
              className="px-4 py-2 bg-gray-800 hover:bg-gray-900 text-white rounded-lg text-sm font-medium transition"
            >
              Filtrar
            </button>
          </div>
        </form>
      </div>

      {/* Products Table */}
      <div className="bg-white rounded-xl border border-gray-200 shadow-sm overflow-hidden">
        {loading ? (
          <div className="p-12 text-center text-gray-500">Carregando catálogo...</div>
        ) : products.length === 0 ? (
          <div className="p-12 text-center">
            <Layers className="h-12 w-12 text-gray-300 mx-auto mb-3" />
            <h3 className="text-base font-semibold text-gray-700">Nenhum produto cadastrado</h3>
            <p className="text-sm text-gray-500 mt-1">Cadastre seus produtos para gerenciar estoque, preços e vendas.</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="bg-gray-50 text-gray-600 font-semibold border-b border-gray-200 text-xs uppercase tracking-wider">
                <tr>
                  <th className="py-3 px-4">Produto & SKU</th>
                  <th className="py-3 px-4">Categoria</th>
                  <th className="py-3 px-4">Tipo SPED / Fiscal</th>
                  <th className="py-3 px-4">NCM / CEST</th>
                  <th className="py-3 px-4">Custo Médio</th>
                  <th className="py-3 px-4">Rastreamento</th>
                  <th className="py-3 px-4">Status</th>
                  <th className="py-3 px-4 text-right">Ações</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {products.map((p) => (
                  <tr key={p.id} className="hover:bg-gray-50/80 transition">
                    <td className="py-3.5 px-4">
                      <div className="font-bold text-gray-900">{p.name}</div>
                      <div className="text-xs text-gray-500 flex items-center gap-2 mt-0.5">
                        <span className="font-mono bg-gray-100 px-1.5 py-0.5 rounded text-[11px] text-gray-700 font-semibold">
                          {p.sku}
                        </span>
                        {p.barcode && (
                          <span className="font-mono text-gray-400 text-[11px] flex items-center gap-0.5">
                            <Barcode className="h-3 w-3" /> {p.barcode}
                          </span>
                        )}
                        <span className="text-gray-400 font-medium">({p.unitOfMeasure})</span>
                      </div>
                    </td>
                    <td className="py-3.5 px-4 text-xs font-medium text-gray-700">
                      {p.categoryName || '—'}
                    </td>
                    <td className="py-3.5 px-4">{formatSpedBadge(p.itemType)}</td>
                    <td className="py-3.5 px-4 text-xs">
                      <div className="font-mono font-medium text-gray-800">
                        {p.ncm ? `${p.ncm.slice(0, 4)}.${p.ncm.slice(4, 6)}.${p.ncm.slice(6, 8)}` : '—'}
                      </div>
                      {p.cest && (
                        <div className="text-[11px] font-mono text-gray-400">CEST: {p.cest}</div>
                      )}
                    </td>
                    <td className="py-3.5 px-4 text-xs font-semibold text-gray-900">
                      {p.averageCost != null
                        ? `R$ ${Number(p.averageCost).toFixed(2)}`
                        : `R$ ${Number(p.lastAcquisitionCost).toFixed(2)}`}
                    </td>
                    <td className="py-3.5 px-4">{formatTrackingBadge(p.trackingMode) || <span className="text-xs text-gray-400">—</span>}</td>
                    <td className="py-3.5 px-4">
                      {p.isActive ? (
                        <span className="inline-flex items-center gap-1 text-xs text-emerald-600 font-medium">
                          <CheckCircle2 className="h-3.5 w-3.5" /> Ativo
                        </span>
                      ) : (
                        <span className="inline-flex items-center gap-1 text-xs text-gray-400 font-medium">
                          <XCircle className="h-3.5 w-3.5" /> Inativo
                        </span>
                      )}
                    </td>
                    <td className="py-3.5 px-4 text-right">
                      <div className="flex items-center justify-end gap-1">
                        <button
                          onClick={() => openEditModal(p)}
                          className="p-1.5 text-gray-500 hover:text-emerald-600 hover:bg-emerald-50 rounded-lg transition"
                          title="Editar Produto"
                        >
                          <Edit2 className="h-4 w-4" />
                        </button>
                        {p.isActive && (
                          <button
                            onClick={() => handleDelete(p.id, p.name)}
                            className="p-1.5 text-gray-400 hover:text-red-600 hover:bg-red-50 rounded-lg transition"
                            title="Desativar Produto"
                          >
                            <Trash2 className="h-4 w-4" />
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {/* Pagination Bar */}
        {totalPages > 1 && (
          <div className="flex items-center justify-between p-4 border-t border-gray-100 bg-gray-50/50">
            <span className="text-xs text-gray-500">
              Total de <strong>{totalElements}</strong> produtos cadastrados
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

      {/* Product Form Modal */}
      <ProductFormModal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        onSuccess={fetchProducts}
        productToEdit={productToEdit}
        categories={categories}
      />
    </div>
  );
};
