import React, { useEffect, useState } from 'react';
import {
  Calculator,
  Plus,
  Star,
  CheckCircle,
  Search,
  Play,
  Layers,
} from 'lucide-react';
import { inboundApi } from '../../services/inboundApi';
import { CostFormulaProfile, CreateCostFormulaProfileRequest } from '../../types/inbound';
import { LandedCostSimulatorModal } from '../../components/inbound/LandedCostSimulatorModal';

export const CostFormulasPage: React.FC = () => {
  const [profiles, setProfiles] = useState<CostFormulaProfile[]>([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');

  // Simulator Modal state
  const [isSimulatorOpen, setIsSimulatorOpen] = useState(false);
  const [simInitialFormula, setSimInitialFormula] = useState<string | undefined>();
  const [simInitialVars, setSimInitialVars] = useState<Record<string, { name: string; label: string; defaultValue: number }> | undefined>();

  // New Profile Form Modal state
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
  const [formData, setFormData] = useState<CreateCostFormulaProfileRequest>({
    code: `CST-${Math.floor(1000 + Math.random() * 9000)}`,
    name: '',
    formulaExpression: '= (INVOICE_UNIT_PRICE * (1 + FATOR_MARGEM)) + (FREIGHT_ALLOCATED / QUANTITY) - ICMS_CREDIT',
    freightProrationMethod: 'VALUE_PROPORTIONAL',
    customVariables: {
      FATOR_MARGEM: { name: 'FATOR_MARGEM', label: 'Margem Operacional Rateada', defaultValue: 0.05, scope: 'PROFILE_DEFAULT' },
    },
    isDefault: false,
  });

  const [newVarName, setNewVarName] = useState('');
  const [newVarLabel, setNewVarLabel] = useState('');
  const [newVarDefault, setNewVarDefault] = useState('0.00');

  const [saving, setSaving] = useState(false);
  const [actionMsg, setActionMsg] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  const loadProfiles = async () => {
    try {
      setLoading(true);
      const data = await inboundApi.listCostFormulaProfiles();
      setProfiles(data);
    } catch (err: any) {
      console.error('Falha ao carregar perfis de cálculo', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadProfiles();
  }, []);

  const handleSetDefault = async (profileId: string) => {
    try {
      await inboundApi.setDefaultCostFormulaProfile(profileId);
      setActionMsg({ type: 'success', text: 'Perfil definido como padrão com sucesso!' });
      loadProfiles();
    } catch (err: any) {
      setActionMsg({
        type: 'error',
        text: err.response?.data?.message || 'Falha ao definir perfil como padrão.',
      });
    }
  };

  const handleAddCustomVar = () => {
    const cleanName = newVarName.trim().toUpperCase().replace(/[^A-Z0-9_]/g, '_');
    if (!cleanName) return;

    setFormData((prev) => ({
      ...prev,
      customVariables: {
        ...(prev.customVariables || {}),
        [cleanName]: {
          name: cleanName,
          label: newVarLabel.trim() || cleanName,
          defaultValue: parseFloat(newVarDefault) || 0,
          scope: 'PROFILE_DEFAULT',
        },
      },
    }));

    setNewVarName('');
    setNewVarLabel('');
    setNewVarDefault('0.00');
  };

  const handleRemoveCustomVar = (varName: string) => {
    setFormData((prev) => {
      const copy = { ...(prev.customVariables || {}) };
      delete copy[varName];
      return { ...prev, customVariables: copy };
    });
  };

  const handleCreateProfile = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!formData.name.trim() || !formData.formulaExpression.trim()) return;

    setSaving(true);
    setActionMsg(null);

    try {
      await inboundApi.createCostFormulaProfile(formData);
      setActionMsg({ type: 'success', text: 'Perfil de fórmula de custo cadastrado com sucesso!' });
      setIsCreateModalOpen(false);
      // Reset form
      setFormData({
        code: `CST-${Math.floor(1000 + Math.random() * 9000)}`,
        name: '',
        formulaExpression: '= (INVOICE_UNIT_PRICE * (1 + FATOR_MARGEM)) + (FREIGHT_ALLOCATED / QUANTITY) - ICMS_CREDIT',
        freightProrationMethod: 'VALUE_PROPORTIONAL',
        customVariables: {},
        isDefault: false,
      });
      loadProfiles();
    } catch (err: any) {
      setActionMsg({
        type: 'error',
        text: err.response?.data?.message || 'Erro ao cadastrar perfil de fórmula.',
      });
    } finally {
      setSaving(false);
    }
  };

  const openSimulatorWithProfile = (profile: CostFormulaProfile) => {
    setSimInitialFormula(profile.formulaExpression);
    const convertedVars: Record<string, { name: string; label: string; defaultValue: number }> = {};
    if (profile.customVariables) {
      Object.entries(profile.customVariables).forEach(([k, v]) => {
        convertedVars[k] = { name: v.name, label: v.label, defaultValue: v.defaultValue };
      });
    }
    setSimInitialVars(convertedVars);
    setIsSimulatorOpen(true);
  };

  const filtered = profiles.filter(
    (p) =>
      p.code.toLowerCase().includes(search.toLowerCase()) ||
      p.name.toLowerCase().includes(search.toLowerCase())
  );

  return (
    <div className="p-6 max-w-7xl mx-auto space-y-6">
      {/* Page Header */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 bg-white p-6 rounded-xl shadow-sm border border-gray-100">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 flex items-center">
            <Calculator className="w-7 h-7 text-primary-600 mr-2" />
            Motor de Cálculo de Custos (Landed Cost)
          </h1>
          <p className="text-sm text-gray-500 mt-1">
            Configure fórmulas estilo planilha (Excel) e variáveis personalizadas para apropriação exata dos custos de aquisição.
          </p>
        </div>
        <div className="flex items-center space-x-3">
          <button
            onClick={() => {
              setSimInitialFormula(undefined);
              setSimInitialVars(undefined);
              setIsSimulatorOpen(true);
            }}
            className="flex items-center px-4 py-2 bg-indigo-50 text-indigo-700 hover:bg-indigo-100 rounded-lg text-sm font-medium border border-indigo-200 transition"
          >
            <Play className="w-4 h-4 mr-2" />
            Simulador Interativo
          </button>
          <button
            onClick={() => setIsCreateModalOpen(true)}
            className="flex items-center px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 text-sm font-medium shadow-sm transition"
          >
            <Plus className="w-4 h-4 mr-2" />
            Nova Fórmula de Custo
          </button>
        </div>
      </div>

      {actionMsg && (
        <div
          className={`p-4 rounded-xl border flex items-center justify-between text-sm ${
            actionMsg.type === 'success'
              ? 'bg-emerald-50 text-emerald-800 border-emerald-200'
              : 'bg-red-50 text-red-800 border-red-200'
          }`}
        >
          <span>{actionMsg.text}</span>
          <button onClick={() => setActionMsg(null)} className="text-gray-400 hover:text-gray-600">
            &times;
          </button>
        </div>
      )}

      {/* Search Bar */}
      <div className="relative">
        <Search className="w-5 h-5 absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
        <input
          type="text"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          placeholder="Buscar por código ou nome do perfil de custo..."
          className="w-full pl-10 pr-4 py-2.5 bg-white border border-gray-200 rounded-xl text-sm focus:ring-2 focus:ring-primary-500 focus:outline-none"
        />
      </div>

      {/* Profiles Cards Grid */}
      {loading ? (
        <div className="p-12 text-center text-gray-400">Carregando fórmulas de custo...</div>
      ) : filtered.length === 0 ? (
        <div className="p-12 text-center bg-white rounded-xl border border-dashed border-gray-200">
          <Layers className="w-10 h-10 text-gray-300 mx-auto mb-2" />
          <p className="text-sm text-gray-500">Nenhum perfil de fórmula encontrado.</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          {filtered.map((profile) => (
            <div
              key={profile.id}
              className={`bg-white rounded-xl shadow-sm border p-6 flex flex-col justify-between transition hover:shadow-md ${
                profile.isDefault ? 'border-primary-500 ring-1 ring-primary-500' : 'border-gray-200'
              }`}
            >
              <div>
                <div className="flex justify-between items-start mb-3">
                  <div className="flex items-center space-x-2">
                    <span className="font-mono text-xs font-semibold px-2.5 py-1 bg-gray-100 text-gray-700 rounded-md">
                      {profile.code}
                    </span>
                    {profile.isDefault && (
                      <span className="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-semibold bg-primary-50 text-primary-700 border border-primary-200">
                        <Star className="w-3 h-3 mr-1 fill-primary-600 text-primary-600" />
                        Padrão do Sistema
                      </span>
                    )}
                  </div>
                  <span className="text-xs text-gray-400">
                    Rateio: {profile.freightProrationMethod === 'VALUE_PROPORTIONAL' ? 'Por Valor' : 'Por Peso/Qtd'}
                  </span>
                </div>

                <h3 className="text-lg font-bold text-gray-900 mb-2">{profile.name}</h3>

                {/* Formula Expression Box */}
                <div className="mb-4 bg-gray-50 p-3 rounded-lg border font-mono text-xs text-gray-800 break-all">
                  <span className="text-gray-400 mr-1 block text-[10px] uppercase font-sans font-bold">
                    Expressão de Cálculo:
                  </span>
                  {profile.formulaExpression}
                </div>

                {/* Custom Variables Chips */}
                {profile.customVariables && Object.keys(profile.customVariables).length > 0 && (
                  <div className="mb-4">
                    <span className="text-xs font-medium text-gray-500 block mb-1">
                      Variáveis Personalizadas:
                    </span>
                    <div className="flex flex-wrap gap-1.5">
                      {Object.entries(profile.customVariables).map(([varKey, varDef]) => (
                        <span
                          key={varKey}
                          className="px-2 py-0.5 bg-blue-50 text-blue-700 border border-blue-200 rounded text-xs font-mono"
                          title={`${varDef.label} (Valor Padrão: ${varDef.defaultValue})`}
                        >
                          {varKey} ({varDef.defaultValue})
                        </span>
                      ))}
                    </div>
                  </div>
                )}
              </div>

              {/* Actions Footer */}
              <div className="pt-4 border-t flex items-center justify-between mt-4">
                <button
                  type="button"
                  onClick={() => openSimulatorWithProfile(profile)}
                  className="inline-flex items-center text-xs font-semibold text-primary-600 hover:text-primary-800 transition"
                >
                  <Play className="w-3.5 h-3.5 mr-1" />
                  Testar no Simulador
                </button>

                {!profile.isDefault && (
                  <button
                    type="button"
                    onClick={() => handleSetDefault(profile.id)}
                    className="inline-flex items-center text-xs text-gray-600 hover:text-gray-900 font-medium transition"
                  >
                    <CheckCircle className="w-3.5 h-3.5 mr-1 text-gray-400" />
                    Tornar Padrão
                  </button>
                )}
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Create Profile Modal */}
      {isCreateModalOpen && (
        <div className="fixed inset-0 z-50 overflow-y-auto bg-black bg-opacity-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-xl shadow-2xl max-w-2xl w-full p-6">
            <h2 className="text-lg font-bold text-gray-900 mb-4">Novo Perfil de Cálculo de Custos</h2>
            <form onSubmit={handleCreateProfile} className="space-y-4">
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-medium text-gray-700 mb-1">Código do Perfil</label>
                  <input
                    type="text"
                    required
                    value={formData.code}
                    onChange={(e) => setFormData({ ...formData, code: e.target.value.toUpperCase() })}
                    className="w-full text-sm px-3 py-2 border rounded-lg focus:ring-2 focus:ring-primary-500 font-mono"
                  />
                </div>
                <div>
                  <label className="block text-xs font-medium text-gray-700 mb-1">Nome / Identificação</label>
                  <input
                    type="text"
                    required
                    placeholder="Ex: Custo Landed Indústria SP"
                    value={formData.name}
                    onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                    className="w-full text-sm px-3 py-2 border rounded-lg focus:ring-2 focus:ring-primary-500"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-medium text-gray-700 mb-1">
                  Expressão da Fórmula (Estilo Excel)
                </label>
                <input
                  type="text"
                  required
                  value={formData.formulaExpression}
                  onChange={(e) => setFormData({ ...formData, formulaExpression: e.target.value })}
                  placeholder="= (INVOICE_UNIT_PRICE * (1 + FATOR)) + (FREIGHT_ALLOCATED / QUANTITY)"
                  className="w-full text-sm px-3 py-2 border rounded-lg focus:ring-2 focus:ring-primary-500 font-mono bg-gray-50"
                />
              </div>

              {/* Custom Variables Section */}
              <div className="border rounded-lg p-3 bg-gray-50 space-y-3">
                <span className="text-xs font-semibold text-gray-700 block">
                  Variáveis Personalizadas Deste Perfil
                </span>

                {formData.customVariables && Object.keys(formData.customVariables).length > 0 && (
                  <div className="flex flex-wrap gap-2">
                    {Object.entries(formData.customVariables).map(([k, v]) => (
                      <span
                        key={k}
                        className="inline-flex items-center px-2 py-1 bg-white border border-gray-300 rounded text-xs font-mono shadow-sm"
                      >
                        {k}: {v.defaultValue}
                        <button
                          type="button"
                          onClick={() => handleRemoveCustomVar(k)}
                          className="ml-1.5 text-red-500 hover:text-red-700"
                        >
                          &times;
                        </button>
                      </span>
                    ))}
                  </div>
                )}

                <div className="flex gap-2 items-center">
                  <input
                    type="text"
                    placeholder="VAR_NOME"
                    value={newVarName}
                    onChange={(e) => setNewVarName(e.target.value)}
                    className="w-1/3 text-xs px-2 py-1.5 border rounded uppercase font-mono"
                  />
                  <input
                    type="text"
                    placeholder="Descrição / Rótulo"
                    value={newVarLabel}
                    onChange={(e) => setNewVarLabel(e.target.value)}
                    className="flex-1 text-xs px-2 py-1.5 border rounded"
                  />
                  <input
                    type="number"
                    step="0.001"
                    placeholder="Valor"
                    value={newVarDefault}
                    onChange={(e) => setNewVarDefault(e.target.value)}
                    className="w-20 text-xs px-2 py-1.5 border rounded font-mono"
                  />
                  <button
                    type="button"
                    onClick={handleAddCustomVar}
                    disabled={!newVarName.trim()}
                    className="px-3 py-1.5 bg-gray-800 text-white text-xs rounded hover:bg-gray-900 disabled:opacity-50"
                  >
                    + Adicionar
                  </button>
                </div>
              </div>

              <div className="flex items-center space-x-2">
                <input
                  type="checkbox"
                  id="isDefault"
                  checked={formData.isDefault}
                  onChange={(e) => setFormData({ ...formData, isDefault: e.target.checked })}
                  className="rounded border-gray-300 text-primary-600 focus:ring-primary-500"
                />
                <label htmlFor="isDefault" className="text-xs text-gray-700 font-medium">
                  Definir como fórmula padrão para novas Notas Fiscais
                </label>
              </div>

              <div className="pt-4 border-t flex justify-end space-x-3">
                <button
                  type="button"
                  onClick={() => setIsCreateModalOpen(false)}
                  className="px-4 py-2 border rounded-lg text-sm text-gray-700 hover:bg-gray-50"
                >
                  Cancelar
                </button>
                <button
                  type="submit"
                  disabled={saving}
                  className="px-4 py-2 bg-primary-600 text-white rounded-lg text-sm font-medium hover:bg-primary-700 disabled:opacity-50"
                >
                  {saving ? 'Salvando...' : 'Salvar Perfil'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Simulator Modal */}
      <LandedCostSimulatorModal
        isOpen={isSimulatorOpen}
        onClose={() => setIsSimulatorOpen(false)}
        initialFormula={simInitialFormula}
        initialCustomVariables={simInitialVars}
      />
    </div>
  );
};
