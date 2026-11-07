import React, { useState, useEffect } from 'react';
import axios from 'axios';
import {
  Warehouse,
  CreateWarehouseRequest,
  UpdateWarehouseRequest,
  StorageLocation,
  CreateStorageLocationRequest,
  WarehouseType,
} from '../../types/inventory';
import { inventoryApi } from '../../services/inventoryApi';
import {
  Warehouse as WarehouseIcon,
  Plus,
  Search,
  MapPin,
  CheckCircle2,
  XCircle,
  Building2,
  Edit2,
  Trash2,
  Layers,
  X,
  Compass,
} from 'lucide-react';

interface CompanyBranchSummary {
  id: string;
  corporateName: string;
  tradeName?: string;
  cnpj: string;
  branchNumber: string;
  establishmentType: string;
}

export const WarehousesPage: React.FC = () => {
  const [warehouses, setWarehouses] = useState<Warehouse[]>([]);
  const [branches, setBranches] = useState<CompanyBranchSummary[]>([]);
  const [selectedBranch, setSelectedBranch] = useState<string>('');
  const [loading, setLoading] = useState(false);
  const [search, setSearch] = useState('');

  // Warehouse Modal
  const [modalOpen, setModalOpen] = useState(false);
  const [editingWarehouse, setEditingWarehouse] = useState<Warehouse | null>(null);
  const [code, setCode] = useState('');
  const [name, setName] = useState('');
  const [branchId, setBranchId] = useState('');
  const [warehouseType, setWarehouseType] = useState<WarehouseType>('COMMERCIAL');
  const [isDefault, setIsDefault] = useState(false);
  const [isActive, setIsActive] = useState(true);
  const [errorMsg, setErrorMsg] = useState('');

  // Storage Location Modal (Bin Addressing)
  const [locationModalOpen, setLocationModalOpen] = useState(false);
  const [activeWarehouse, setActiveWarehouse] = useState<Warehouse | null>(null);
  const [locations, setLocations] = useState<StorageLocation[]>([]);
  const [aisle, setAisle] = useState('');
  const [rack, setRack] = useState('');
  const [shelf, setShelf] = useState('');
  const [bin, setBin] = useState('');
  const [locationError, setLocationError] = useState('');

  const fetchBranches = async () => {
    try {
      const token = localStorage.getItem('opencontrol_token');
      const res = await axios.get<CompanyBranchSummary[]>('/api/v1/companies', {
        headers: token ? { Authorization: `Bearer ${token}` } : {},
      });
      setBranches(res.data);
      if (res.data.length > 0 && !selectedBranch) {
        setBranchId(res.data[0].id);
      }
    } catch (err) {
      console.error('Error fetching branches:', err);
    }
  };

  const fetchWarehouses = async () => {
    setLoading(true);
    try {
      const data = await inventoryApi.listWarehouses(selectedBranch || undefined);
      setWarehouses(data);
    } catch (err) {
      console.error('Error fetching warehouses:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchBranches();
  }, []);

  useEffect(() => {
    fetchWarehouses();
  }, [selectedBranch]);

  const openCreateModal = () => {
    setEditingWarehouse(null);
    setCode('');
    setName('');
    setBranchId(selectedBranch || branches[0]?.id || '');
    setWarehouseType('COMMERCIAL');
    setIsDefault(false);
    setIsActive(true);
    setErrorMsg('');
    setModalOpen(true);
  };

  const openEditModal = (w: Warehouse) => {
    setEditingWarehouse(w);
    setCode(w.code);
    setName(w.name);
    setBranchId(w.branchId);
    setWarehouseType(w.warehouseType);
    setIsDefault(w.isDefault);
    setIsActive(w.isActive);
    setErrorMsg('');
    setModalOpen(true);
  };

  const handleSaveWarehouse = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMsg('');
    try {
      if (editingWarehouse) {
        const payload: UpdateWarehouseRequest = {
          branchId,
          name,
          warehouseType,
          isDefault,
          isActive,
        };
        await inventoryApi.updateWarehouse(editingWarehouse.id, payload);
      } else {
        const payload: CreateWarehouseRequest = {
          branchId,
          code,
          name,
          warehouseType,
          isDefault,
        };
        await inventoryApi.createWarehouse(payload);
      }
      setModalOpen(false);
      fetchWarehouses();
    } catch (err: any) {
      setErrorMsg(err.response?.data?.detail || err.message || 'Erro ao salvar depósito');
    }
  };

  const handleDeleteWarehouse = async (id: string, wName: string) => {
    if (window.confirm(`Deseja desativar o depósito "${wName}"?`)) {
      try {
        await inventoryApi.deleteWarehouse(id);
        fetchWarehouses();
      } catch (err: any) {
        alert(err.response?.data?.detail || 'Erro ao desativar depósito');
      }
    }
  };

  // Bin locations
  const openLocationsModal = async (w: Warehouse) => {
    setActiveWarehouse(w);
    setAisle('');
    setRack('');
    setShelf('');
    setBin('');
    setLocationError('');
    setLocationModalOpen(true);
    try {
      const locs = await inventoryApi.listLocations(w.id);
      setLocations(locs);
    } catch (err) {
      console.error('Error fetching locations:', err);
    }
  };

  const handleAddLocation = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!activeWarehouse) return;
    setLocationError('');
    try {
      const payload: CreateStorageLocationRequest = {
        aisle: aisle.trim().toUpperCase(),
        rack: rack.trim().toUpperCase(),
        shelf: shelf.trim().toUpperCase(),
        bin: bin.trim().toUpperCase(),
      };
      await inventoryApi.createLocation(activeWarehouse.id, payload);
      const locs = await inventoryApi.listLocations(activeWarehouse.id);
      setLocations(locs);
      setAisle('');
      setRack('');
      setShelf('');
      setBin('');
    } catch (err: any) {
      setLocationError(err.response?.data?.detail || err.message || 'Erro ao criar endereço logístico');
    }
  };

  const filteredWarehouses = warehouses.filter((w) =>
    w.name.toLowerCase().includes(search.toLowerCase()) ||
    w.code.toLowerCase().includes(search.toLowerCase())
  );

  const getBranchName = (bId: string) => {
    const branch = branches.find((b) => b.id === bId);
    return branch ? `${branch.tradeName || branch.corporateName} (#${branch.branchNumber})` : 'Filial';
  };

  const formatWarehouseType = (type: WarehouseType) => {
    switch (type) {
      case 'COMMERCIAL':
        return <span className="px-2 py-0.5 rounded text-[11px] font-semibold bg-blue-50 text-blue-700">Comercial / Vendas</span>;
      case 'STORAGE':
        return <span className="px-2 py-0.5 rounded text-[11px] font-semibold bg-emerald-50 text-emerald-700">Almoxarifado Geral</span>;
      case 'QUARANTINE':
        return <span className="px-2 py-0.5 rounded text-[11px] font-semibold bg-amber-50 text-amber-700">Quarentena / Avarias</span>;
      case 'TRANSIT':
        return <span className="px-2 py-0.5 rounded text-[11px] font-semibold bg-purple-50 text-purple-700">Trânsito / Transferência</span>;
    }
  };

  return (
    <div className="p-6 space-y-6">
      {/* Header */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 flex items-center gap-2">
            <WarehouseIcon className="h-7 w-7 text-indigo-600" />
            Depósitos & Endereçamento Físico
          </h1>
          <p className="text-sm text-gray-500 mt-1">
            Gestão multi-depósito particionada por filial e estruturação de endereços logísticos (Rua-Estante-Nível-Box)
          </p>
        </div>
        <button
          onClick={openCreateModal}
          className="inline-flex items-center gap-2 px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white font-medium rounded-lg shadow-sm transition"
        >
          <Plus className="h-4 w-4" />
          Novo Depósito
        </button>
      </div>

      {/* Filter Toolbar */}
      <div className="bg-white p-4 rounded-xl border border-gray-200 shadow-sm flex flex-col md:flex-row gap-3 items-center">
        <div className="flex-1 flex items-center gap-2 px-3 py-2 bg-gray-50 border border-gray-200 rounded-lg w-full">
          <Search className="h-4 w-4 text-gray-400" />
          <input
            type="text"
            placeholder="Buscar por código ou nome do depósito..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="flex-1 bg-transparent border-none outline-none text-sm text-gray-800 placeholder-gray-400"
          />
        </div>

        <div className="flex items-center gap-2 w-full md:w-auto">
          <Building2 className="h-4 w-4 text-gray-500" />
          <select
            value={selectedBranch}
            onChange={(e) => setSelectedBranch(e.target.value)}
            className="px-3 py-2 border border-gray-300 rounded-lg text-sm text-gray-700 focus:outline-none focus:ring-2 focus:ring-indigo-500 bg-white"
          >
            <option value="">Todas as Filiais</option>
            {branches.map((b) => (
              <option key={b.id} value={b.id}>
                {b.tradeName || b.corporateName} (#{b.branchNumber})
              </option>
            ))}
          </select>
        </div>
      </div>

      {/* Warehouses Grid */}
      {loading ? (
        <div className="p-12 text-center text-gray-500">Carregando depósitos...</div>
      ) : filteredWarehouses.length === 0 ? (
        <div className="bg-white p-12 text-center rounded-xl border border-gray-200 shadow-sm">
          <Layers className="h-12 w-12 text-gray-300 mx-auto mb-3" />
          <h3 className="text-base font-semibold text-gray-700">Nenhum depósito cadastrado</h3>
          <p className="text-sm text-gray-500 mt-1">
            Cadastre os depósitos físicos ou almoxarifados da sua empresa para controlar os saldos de estoque.
          </p>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
          {filteredWarehouses.map((w) => (
            <div
              key={w.id}
              className="bg-white rounded-xl border border-gray-200 p-5 shadow-sm hover:shadow-md transition flex flex-col justify-between"
            >
              <div>
                <div className="flex items-center justify-between mb-3">
                  <span className="font-mono text-xs font-bold px-2.5 py-1 bg-indigo-50 text-indigo-700 rounded-md border border-indigo-100">
                    {w.code}
                  </span>
                  <div className="flex items-center gap-1.5">
                    {w.isDefault && (
                      <span className="text-[11px] font-semibold bg-emerald-50 text-emerald-700 border border-emerald-200 px-2 py-0.5 rounded-full">
                        Padrão
                      </span>
                    )}
                    {w.isActive ? (
                      <span className="inline-flex items-center gap-1 text-xs text-emerald-600 font-medium">
                        <CheckCircle2 className="h-3.5 w-3.5" /> Ativo
                      </span>
                    ) : (
                      <span className="inline-flex items-center gap-1 text-xs text-gray-400 font-medium">
                        <XCircle className="h-3.5 w-3.5" /> Inativo
                      </span>
                    )}
                  </div>
                </div>

                <h3 className="text-base font-bold text-gray-900">{w.name}</h3>
                <div className="text-xs text-gray-500 mt-1 flex items-center gap-1">
                  <Building2 className="h-3.5 w-3.5 text-gray-400" />
                  <span>{getBranchName(w.branchId)}</span>
                </div>
                <div className="mt-2.5">{formatWarehouseType(w.warehouseType)}</div>
              </div>

              <div className="mt-5 pt-3 border-t border-gray-100 flex items-center justify-between">
                <button
                  onClick={() => openLocationsModal(w)}
                  className="inline-flex items-center gap-1.5 text-xs font-semibold text-indigo-600 hover:text-indigo-800 hover:bg-indigo-50 px-2.5 py-1.5 rounded-lg transition"
                >
                  <MapPin className="h-3.5 w-3.5" />
                  Endereços Físicos (Boxes)
                </button>

                <div className="flex items-center gap-1">
                  <button
                    onClick={() => openEditModal(w)}
                    className="p-1.5 text-gray-500 hover:text-indigo-600 hover:bg-indigo-50 rounded-lg transition"
                    title="Editar Depósito"
                  >
                    <Edit2 className="h-4 w-4" />
                  </button>
                  {w.isActive && (
                    <button
                      onClick={() => handleDeleteWarehouse(w.id, w.name)}
                      className="p-1.5 text-gray-400 hover:text-red-600 hover:bg-red-50 rounded-lg transition"
                      title="Desativar Depósito"
                    >
                      <Trash2 className="h-4 w-4" />
                    </button>
                  )}
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Warehouse Modal */}
      {modalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 backdrop-blur-sm p-4">
          <div className="bg-white rounded-2xl max-w-md w-full p-6 shadow-xl border border-gray-100">
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-lg font-bold text-gray-900">
                {editingWarehouse ? 'Editar Depósito' : 'Novo Depósito'}
              </h2>
              <button onClick={() => setModalOpen(false)} className="text-gray-400 hover:text-gray-600 p-1">
                <X className="h-5 w-5" />
              </button>
            </div>

            {errorMsg && (
              <div className="mb-4 p-3 bg-red-50 border border-red-200 text-red-700 text-sm rounded-lg">
                {errorMsg}
              </div>
            )}

            <form onSubmit={handleSaveWarehouse} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-gray-700 mb-1">Filial Responsável *</label>
                <select
                  required
                  value={branchId}
                  onChange={(e) => setBranchId(e.target.value)}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-indigo-500 focus:outline-none"
                >
                  <option value="">Selecione a Filial...</option>
                  {branches.map((b) => (
                    <option key={b.id} value={b.id}>
                      {b.tradeName || b.corporateName} (#{b.branchNumber})
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-xs font-semibold text-gray-700 mb-1">Código do Depósito *</label>
                <input
                  type="text"
                  required
                  disabled={!!editingWarehouse}
                  placeholder="EX: ALMOX-01, MATRIZ-COM"
                  value={code}
                  onChange={(e) => setCode(e.target.value.toUpperCase())}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-indigo-500 focus:outline-none disabled:bg-gray-100 font-mono"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-gray-700 mb-1">Nome do Depósito *</label>
                <input
                  type="text"
                  required
                  placeholder="EX: Almoxarifado Central"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-indigo-500 focus:outline-none"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-gray-700 mb-1">Finalidade / Tipo *</label>
                <select
                  value={warehouseType}
                  onChange={(e) => setWarehouseType(e.target.value as WarehouseType)}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-indigo-500 focus:outline-none"
                >
                  <option value="COMMERCIAL">Comercial / Vendas</option>
                  <option value="STORAGE">Almoxarifado / Matérias-Primas</option>
                  <option value="QUARANTINE">Quarentena / Avarias / Devoluções</option>
                  <option value="TRANSIT">Trânsito / Transferência</option>
                </select>
              </div>

              <div className="flex items-center gap-2 pt-2">
                <input
                  type="checkbox"
                  id="isDefault"
                  checked={isDefault}
                  onChange={(e) => setIsDefault(e.target.checked)}
                  className="rounded text-indigo-600 focus:ring-indigo-500 h-4 w-4"
                />
                <label htmlFor="isDefault" className="text-sm font-medium text-gray-700">
                  Definir como Depósito Padrão da Filial
                </label>
              </div>

              {editingWarehouse && (
                <div className="flex items-center gap-2">
                  <input
                    type="checkbox"
                    id="isActive"
                    checked={isActive}
                    onChange={(e) => setIsActive(e.target.checked)}
                    className="rounded text-indigo-600 focus:ring-indigo-500 h-4 w-4"
                  />
                  <label htmlFor="isActive" className="text-sm font-medium text-gray-700">
                    Depósito Ativo
                  </label>
                </div>
              )}

              <div className="flex items-center justify-end gap-3 pt-4 border-t border-gray-100">
                <button
                  type="button"
                  onClick={() => setModalOpen(false)}
                  className="px-4 py-2 border border-gray-300 text-gray-700 rounded-lg text-sm font-medium hover:bg-gray-50 transition"
                >
                  Cancelar
                </button>
                <button
                  type="submit"
                  className="px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg text-sm font-medium shadow-sm transition"
                >
                  Salvar
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Storage Location Drawer / Modal */}
      {locationModalOpen && activeWarehouse && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 backdrop-blur-sm p-4">
          <div className="bg-white rounded-2xl max-w-2xl w-full p-6 shadow-xl border border-gray-100 flex flex-col max-h-[85vh]">
            <div className="flex items-center justify-between pb-4 border-b border-gray-100">
              <div>
                <h2 className="text-lg font-bold text-gray-900 flex items-center gap-2">
                  <Compass className="h-5 w-5 text-indigo-600" />
                  Endereçamento Logístico — {activeWarehouse.name}
                </h2>
                <p className="text-xs text-gray-500 font-mono mt-0.5">Depósito: {activeWarehouse.code}</p>
              </div>
              <button
                onClick={() => setLocationModalOpen(false)}
                className="text-gray-400 hover:text-gray-600 p-1"
              >
                <X className="h-5 w-5" />
              </button>
            </div>

            {locationError && (
              <div className="mt-3 p-3 bg-red-50 border border-red-200 text-red-700 text-xs rounded-lg">
                {locationError}
              </div>
            )}

            {/* Quick Add Form */}
            <form onSubmit={handleAddLocation} className="mt-4 p-4 bg-gray-50 rounded-xl border border-gray-200">
              <h4 className="text-xs font-bold text-gray-700 uppercase tracking-wider mb-2">
                Cadastrar Novo Endereço (Rua-Estante-Nível-Box)
              </h4>
              <div className="grid grid-cols-2 md:grid-cols-4 gap-2">
                <div>
                  <label className="block text-[11px] font-semibold text-gray-600 mb-0.5">Rua / Corredor *</label>
                  <input
                    type="text"
                    required
                    placeholder="Ex: A"
                    value={aisle}
                    onChange={(e) => setAisle(e.target.value.toUpperCase())}
                    className="w-full px-2.5 py-1.5 border border-gray-300 rounded-lg text-xs font-mono"
                  />
                </div>
                <div>
                  <label className="block text-[11px] font-semibold text-gray-600 mb-0.5">Estante / Rack *</label>
                  <input
                    type="text"
                    required
                    placeholder="Ex: 01"
                    value={rack}
                    onChange={(e) => setRack(e.target.value.toUpperCase())}
                    className="w-full px-2.5 py-1.5 border border-gray-300 rounded-lg text-xs font-mono"
                  />
                </div>
                <div>
                  <label className="block text-[11px] font-semibold text-gray-600 mb-0.5">Nível / Prateleira *</label>
                  <input
                    type="text"
                    required
                    placeholder="Ex: 02"
                    value={shelf}
                    onChange={(e) => setShelf(e.target.value.toUpperCase())}
                    className="w-full px-2.5 py-1.5 border border-gray-300 rounded-lg text-xs font-mono"
                  />
                </div>
                <div>
                  <label className="block text-[11px] font-semibold text-gray-600 mb-0.5">Vão / Box *</label>
                  <input
                    type="text"
                    required
                    placeholder="Ex: 01"
                    value={bin}
                    onChange={(e) => setBin(e.target.value.toUpperCase())}
                    className="w-full px-2.5 py-1.5 border border-gray-300 rounded-lg text-xs font-mono"
                  />
                </div>
              </div>

              <div className="flex items-center justify-between mt-3 pt-2 border-t border-gray-200">
                <span className="text-xs text-gray-500">
                  Endereço Formatado:{' '}
                  <strong className="font-mono text-indigo-700">
                    {aisle && rack && shelf && bin ? `${aisle}-${rack}-${shelf}-${bin}` : '—'}
                  </strong>
                </span>
                <button
                  type="submit"
                  className="px-3.5 py-1.5 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg text-xs font-medium transition"
                >
                  Adicionar Endereço
                </button>
              </div>
            </form>

            {/* List of existing locations */}
            <div className="mt-4 flex-1 overflow-y-auto">
              <h4 className="text-xs font-bold text-gray-500 uppercase tracking-wider mb-2">
                Endereços Cadastrados ({locations.length})
              </h4>
              {locations.length === 0 ? (
                <p className="text-xs text-gray-400 py-4 text-center">Nenhum endereço logístico configurado neste depósito.</p>
              ) : (
                <div className="grid grid-cols-2 md:grid-cols-3 gap-2">
                  {locations.map((loc) => (
                    <div
                      key={loc.id}
                      className="p-2.5 bg-white border border-gray-200 rounded-lg shadow-2xs flex items-center justify-between"
                    >
                      <span className="font-mono text-xs font-bold text-gray-800">
                        {loc.formattedAddress}
                      </span>
                      <MapPin className="h-3.5 w-3.5 text-indigo-500" />
                    </div>
                  ))}
                </div>
              )}
            </div>

            <div className="mt-4 pt-3 border-t border-gray-100 flex justify-end">
              <button
                type="button"
                onClick={() => setLocationModalOpen(false)}
                className="px-4 py-1.5 border border-gray-300 text-gray-700 rounded-lg text-xs font-medium hover:bg-gray-50"
              >
                Fechar
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
