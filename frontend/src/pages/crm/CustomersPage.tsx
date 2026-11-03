import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Customer, CustomerCategory, EconomicGroup } from '../../types/crm';
import { crmApi } from '../../services/crmApi';
import { CustomerFormModal } from '../../components/crm/CustomerFormModal';
import { CustomerCategoryModal } from '../../components/crm/CustomerCategoryModal';
import { EconomicGroupModal } from '../../components/crm/EconomicGroupModal';
import {
  Users,
  Search,
  Plus,
  Tag,
  Network,
  Eye,
  Edit,
  Power,
  ChevronLeft,
  ChevronRight,
  Building2,
  User,
} from 'lucide-react';

export const CustomersPage: React.FC = () => {
  const navigate = useNavigate();

  const [customers, setCustomers] = useState<Customer[]>([]);
  const [categories, setCategories] = useState<CustomerCategory[]>([]);
  const [economicGroups, setEconomicGroups] = useState<EconomicGroup[]>([]);
  const [loading, setLoading] = useState(false);

  // Filters
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedCategory, setSelectedCategory] = useState('');
  const [selectedGroup, setSelectedGroup] = useState('');
  const [activeOnly, setActiveOnly] = useState(false);

  // Pagination
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [totalElements, setTotalElements] = useState(0);

  // Modals
  const [isCustomerModalOpen, setIsCustomerModalOpen] = useState(false);
  const [customerToEdit, setCustomerToEdit] = useState<Customer | null>(null);
  const [isCategoryModalOpen, setIsCategoryModalOpen] = useState(false);
  const [isGroupModalOpen, setIsGroupModalOpen] = useState(false);

  const fetchFiltersData = async () => {
    try {
      const [cats, grps] = await Promise.all([
        crmApi.listCustomerCategories(),
        crmApi.listEconomicGroups(),
      ]);
      setCategories(cats);
      setEconomicGroups(grps);
    } catch (err) {
      console.error('Error fetching categories or groups:', err);
    }
  };

  const fetchCustomers = async () => {
    try {
      setLoading(true);
      const res = await crmApi.listCustomers({
        query: searchQuery.trim() || undefined,
        categoryId: selectedCategory || undefined,
        economicGroupId: selectedGroup || undefined,
        activeOnly,
        page,
        size: 15,
      });
      setCustomers(res.content);
      setTotalPages(res.totalPages || 1);
      setTotalElements(res.totalElements || 0);
    } catch (err) {
      console.error('Error fetching customers:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchFiltersData();
  }, []);

  useEffect(() => {
    fetchCustomers();
  }, [searchQuery, selectedCategory, selectedGroup, activeOnly, page]);

  const handleToggleStatus = async (customer: Customer) => {
    try {
      await crmApi.toggleCustomerStatus(customer.id, !customer.isActive);
      await fetchCustomers();
    } catch (err) {
      console.error('Error toggling customer status:', err);
    }
  };

  const handleEdit = (customer: Customer) => {
    setCustomerToEdit(customer);
    setIsCustomerModalOpen(true);
  };

  const handleNewCustomer = () => {
    setCustomerToEdit(null);
    setIsCustomerModalOpen(true);
  };

  return (
    <div className="p-6 space-y-6">
      {/* Header */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <div className="flex items-center gap-2">
            <Users className="h-7 w-7 text-indigo-600 dark:text-indigo-400" />
            <h1 className="text-2xl font-bold text-gray-900 dark:text-white">
              Gestão de Clientes
            </h1>
          </div>
          <p className="text-sm text-gray-500 dark:text-gray-400">
            Cadastro mestre de compradores (Pessoa Física e Jurídica), segmentação e crédito
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-2">
          <button
            onClick={() => setIsCategoryModalOpen(true)}
            className="inline-flex items-center gap-1.5 rounded-lg border border-gray-300 bg-white px-3 py-2 text-xs font-semibold text-gray-700 shadow-sm hover:bg-gray-50 dark:border-gray-600 dark:bg-gray-800 dark:text-gray-200 dark:hover:bg-gray-700"
          >
            <Tag className="h-4 w-4 text-indigo-500" /> Categorias
          </button>
          <button
            onClick={() => setIsGroupModalOpen(true)}
            className="inline-flex items-center gap-1.5 rounded-lg border border-gray-300 bg-white px-3 py-2 text-xs font-semibold text-gray-700 shadow-sm hover:bg-gray-50 dark:border-gray-600 dark:bg-gray-800 dark:text-gray-200 dark:hover:bg-gray-700"
          >
            <Network className="h-4 w-4 text-purple-500" /> Grupos Econômicos
          </button>
          <button
            onClick={handleNewCustomer}
            className="inline-flex items-center gap-1.5 rounded-lg bg-indigo-600 px-4 py-2 text-xs font-semibold text-white shadow-sm hover:bg-indigo-700"
          >
            <Plus className="h-4 w-4" /> Novo Cliente
          </button>
        </div>
      </div>

      {/* Filter Bar */}
      <div className="flex flex-col md:flex-row items-center gap-3 rounded-xl border border-gray-200 bg-white p-4 shadow-sm dark:border-gray-700 dark:bg-gray-800">
        <div className="relative flex-1 w-full">
          <Search className="absolute left-3 top-2.5 h-4 w-4 text-gray-400" />
          <input
            type="text"
            placeholder="Buscar por razão social, nome fantasia ou documento (CPF/CNPJ)..."
            value={searchQuery}
            onChange={(e) => {
              setSearchQuery(e.target.value);
              setPage(0);
            }}
            className="w-full rounded-lg border border-gray-300 pl-9 pr-4 py-2 text-xs dark:border-gray-600 dark:bg-gray-700 dark:text-white"
          />
        </div>

        <select
          value={selectedCategory}
          onChange={(e) => {
            setSelectedCategory(e.target.value);
            setPage(0);
          }}
          className="rounded-lg border border-gray-300 px-3 py-2 text-xs dark:border-gray-600 dark:bg-gray-700 dark:text-white"
        >
          <option value="">Todas as Categorias</option>
          {categories.map((c) => (
            <option key={c.id} value={c.id}>
              {c.name}
            </option>
          ))}
        </select>

        <select
          value={selectedGroup}
          onChange={(e) => {
            setSelectedGroup(e.target.value);
            setPage(0);
          }}
          className="rounded-lg border border-gray-300 px-3 py-2 text-xs dark:border-gray-600 dark:bg-gray-700 dark:text-white"
        >
          <option value="">Todos os Grupos</option>
          {economicGroups.map((g) => (
            <option key={g.id} value={g.id}>
              {g.name}
            </option>
          ))}
        </select>

        <label className="flex items-center gap-1.5 text-xs text-gray-700 dark:text-gray-300 whitespace-nowrap cursor-pointer">
          <input
            type="checkbox"
            checked={activeOnly}
            onChange={(e) => {
              setActiveOnly(e.target.checked);
              setPage(0);
            }}
            className="rounded border-gray-300 text-indigo-600 focus:ring-indigo-500"
          />
          Apenas Ativos
        </label>
      </div>

      {/* Table */}
      <div className="overflow-hidden rounded-xl border border-gray-200 bg-white shadow-sm dark:border-gray-700 dark:bg-gray-800">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead className="border-b border-gray-200 bg-gray-50 uppercase tracking-wider text-gray-500 dark:border-gray-700 dark:bg-gray-850 dark:text-gray-400">
              <tr>
                <th className="px-4 py-3">Tipo</th>
                <th className="px-4 py-3">Razão Social / Nome</th>
                <th className="px-4 py-3">Documento</th>
                <th className="px-4 py-3">Categoria</th>
                <th className="px-4 py-3">Limite de Crédito</th>
                <th className="px-4 py-3">Status</th>
                <th className="px-4 py-3 text-right">Ações</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-200 dark:divide-gray-700">
              {customers.map((c) => (
                <tr
                  key={c.id}
                  className="hover:bg-gray-50 dark:hover:bg-gray-750 transition-colors"
                >
                  <td className="px-4 py-3 whitespace-nowrap">
                    {c.type === 'LEGAL_ENTITY' ? (
                      <span className="inline-flex items-center gap-1 rounded bg-blue-50 px-2 py-0.5 text-[10px] font-semibold text-blue-700 dark:bg-blue-900/40 dark:text-blue-300">
                        <Building2 className="h-3 w-3" /> PJ
                      </span>
                    ) : (
                      <span className="inline-flex items-center gap-1 rounded bg-amber-50 px-2 py-0.5 text-[10px] font-semibold text-amber-700 dark:bg-amber-900/40 dark:text-amber-300">
                        <User className="h-3 w-3" /> PF
                      </span>
                    )}
                  </td>
                  <td className="px-4 py-3">
                    <button
                      onClick={() => navigate(`/crm/customers/${c.id}`)}
                      className="font-semibold text-gray-900 hover:text-indigo-600 dark:text-white dark:hover:text-indigo-400 text-left"
                    >
                      {c.legalName}
                    </button>
                    {c.tradeName && (
                      <div className="text-[11px] text-gray-500 dark:text-gray-400">
                        {c.tradeName}
                      </div>
                    )}
                  </td>
                  <td className="px-4 py-3 font-mono text-gray-700 dark:text-gray-300 whitespace-nowrap">
                    {c.formattedDocument || c.document}
                  </td>
                  <td className="px-4 py-3 whitespace-nowrap">
                    {c.categoryName ? (
                      <span
                        className="inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-[11px] font-medium"
                        style={{
                          backgroundColor: c.categoryColorHex ? `${c.categoryColorHex}20` : '#E0E7FF',
                          color: c.categoryColorHex || '#3730A3',
                        }}
                      >
                        <span
                          className="h-1.5 w-1.5 rounded-full"
                          style={{ backgroundColor: c.categoryColorHex || '#4F46E5' }}
                        />
                        {c.categoryName}
                      </span>
                    ) : (
                      <span className="text-gray-400 italic text-[11px]">Sem categoria</span>
                    )}
                  </td>
                  <td className="px-4 py-3 font-mono font-medium text-emerald-600 dark:text-emerald-400 whitespace-nowrap">
                    R$ {Number(c.creditLimit || 0).toLocaleString('pt-BR', { minimumFractionDigits: 2 })}
                  </td>
                  <td className="px-4 py-3 whitespace-nowrap">
                    <span
                      className={`inline-block rounded-full px-2 py-0.5 text-[10px] font-semibold ${
                        c.isActive
                          ? 'bg-emerald-100 text-emerald-800 dark:bg-emerald-900/40 dark:text-emerald-300'
                          : 'bg-rose-100 text-rose-800 dark:bg-rose-900/40 dark:text-rose-300'
                      }`}
                    >
                      {c.isActive ? 'Ativo' : 'Inativo'}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-right whitespace-nowrap">
                    <div className="flex items-center justify-end gap-1">
                      <button
                        onClick={() => navigate(`/crm/customers/${c.id}`)}
                        className="rounded p-1 text-gray-500 hover:bg-gray-100 hover:text-indigo-600 dark:hover:bg-gray-700"
                        title="Ver Perfil 360"
                      >
                        <Eye className="h-4 w-4" />
                      </button>
                      <button
                        onClick={() => handleEdit(c)}
                        className="rounded p-1 text-gray-500 hover:bg-gray-100 hover:text-indigo-600 dark:hover:bg-gray-700"
                        title="Editar"
                      >
                        <Edit className="h-4 w-4" />
                      </button>
                      <button
                        onClick={() => handleToggleStatus(c)}
                        className={`rounded p-1 ${
                          c.isActive
                            ? 'text-rose-500 hover:bg-rose-50 dark:hover:bg-rose-900/20'
                            : 'text-emerald-500 hover:bg-emerald-50 dark:hover:bg-emerald-900/20'
                        }`}
                        title={c.isActive ? 'Desativar' : 'Ativar'}
                      >
                        <Power className="h-4 w-4" />
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
              {customers.length === 0 && !loading && (
                <tr>
                  <td colSpan={7} className="py-8 text-center text-gray-500 dark:text-gray-400">
                    Nenhum cliente localizado para os filtros informados.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination Footer */}
        <div className="flex items-center justify-between border-t border-gray-200 px-4 py-3 text-xs text-gray-500 dark:border-gray-700 dark:text-gray-400">
          <div>
            Total: <span className="font-semibold text-gray-900 dark:text-white">{totalElements}</span> clientes
          </div>
          <div className="flex items-center gap-2">
            <span>
              Página {page + 1} de {totalPages}
            </span>
            <div className="flex gap-1">
              <button
                disabled={page <= 0}
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                className="rounded border border-gray-300 p-1 hover:bg-gray-100 disabled:opacity-40 dark:border-gray-600 dark:hover:bg-gray-700"
              >
                <ChevronLeft className="h-4 w-4" />
              </button>
              <button
                disabled={page + 1 >= totalPages}
                onClick={() => setPage((p) => p + 1)}
                className="rounded border border-gray-300 p-1 hover:bg-gray-100 disabled:opacity-40 dark:border-gray-600 dark:hover:bg-gray-700"
              >
                <ChevronRight className="h-4 w-4" />
              </button>
            </div>
          </div>
        </div>
      </div>

      {/* Modals */}
      <CustomerFormModal
        isOpen={isCustomerModalOpen}
        onClose={() => setIsCustomerModalOpen(false)}
        customerToEdit={customerToEdit}
        onSaved={fetchCustomers}
      />

      <CustomerCategoryModal
        isOpen={isCategoryModalOpen}
        onClose={() => setIsCategoryModalOpen(false)}
        onUpdated={fetchFiltersData}
      />

      <EconomicGroupModal
        isOpen={isGroupModalOpen}
        onClose={() => setIsGroupModalOpen(false)}
        onUpdated={fetchFiltersData}
      />
    </div>
  );
};
