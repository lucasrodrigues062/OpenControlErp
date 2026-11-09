import React, { useState } from 'react';
import { X, Calculator, Plus, Trash2, CheckCircle2, AlertTriangle, HelpCircle } from 'lucide-react';
import { inboundApi } from '../../services/inboundApi';

interface LandedCostSimulatorModalProps {
  isOpen: boolean;
  onClose: () => void;
  initialFormula?: string;
  initialCustomVariables?: Record<string, { name: string; label: string; defaultValue: number }>;
}

export const LandedCostSimulatorModal: React.FC<LandedCostSimulatorModalProps> = ({
  isOpen,
  onClose,
  initialFormula = '= (INVOICE_UNIT_PRICE * (1 + FATOR_QUEBRA)) + (FREIGHT_ALLOCATED / QUANTITY) - ICMS_CREDIT - PIS_CREDIT - COFINS_CREDIT',
  initialCustomVariables = {
    FATOR_QUEBRA: { name: 'FATOR_QUEBRA', label: 'Fator de Quebra/Perda', defaultValue: 0.05 },
  },
}) => {
  const [formula, setFormula] = useState(initialFormula);
  const [variables, setVariables] = useState<Record<string, number>>({
    INVOICE_UNIT_PRICE: 100.0,
    QUANTITY: 10,
    FREIGHT_ALLOCATED: 50.0,
    INSURANCE_ALLOCATED: 10.0,
    OTHER_EXPENSES_ALLOCATED: 5.0,
    ICMS_CREDIT: 12.0,
    PIS_CREDIT: 1.65,
    COFINS_CREDIT: 7.6,
    IPI_COST: 5.0,
    ICMS_ST_COST: 0.0,
    ...Object.fromEntries(
      Object.entries(initialCustomVariables).map(([k, v]) => [k, v.defaultValue])
    ),
  });

  const [customVarList, setCustomVarList] = useState<Array<{ name: string; label: string; val: number }>>(
    Object.entries(initialCustomVariables).map(([k, v]) => ({
      name: k,
      label: v.label,
      val: v.defaultValue,
    }))
  );

  const [newVarName, setNewVarName] = useState('');
  const [newVarLabel, setNewVarLabel] = useState('');
  const [newVarValue, setNewVarValue] = useState('0.00');

  const [evaluating, setEvaluating] = useState(false);
  const [result, setResult] = useState<number | null>(null);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  if (!isOpen) return null;

  const handleSimulate = async () => {
    setEvaluating(true);
    setErrorMsg(null);
    setResult(null);

    try {
      const resp = await inboundApi.evaluateFormulaTest({
        formulaExpression: formula,
        variables,
      });

      if (resp.success) {
        setResult(resp.result);
      } else {
        setErrorMsg(resp.errorMessage || 'Erro na avaliação da fórmula.');
      }
    } catch (err: any) {
      setErrorMsg(err.response?.data?.message || err.message || 'Falha ao conectar com o motor de cálculo.');
    } finally {
      setEvaluating(false);
    }
  };

  const handleAddCustomVar = () => {
    const cleanName = newVarName.trim().toUpperCase().replace(/[^A-Z0-9_]/g, '_');
    if (!cleanName) return;

    const val = parseFloat(newVarValue) || 0;
    const label = newVarLabel.trim() || cleanName;

    setCustomVarList((prev) => [...prev, { name: cleanName, label, val }]);
    setVariables((prev) => ({ ...prev, [cleanName]: val }));

    setNewVarName('');
    setNewVarLabel('');
    setNewVarValue('0.00');
  };

  const handleRemoveCustomVar = (name: string) => {
    setCustomVarList((prev) => prev.filter((v) => v.name !== name));
    setVariables((prev) => {
      const copy = { ...prev };
      delete copy[name];
      return copy;
    });
  };

  const handleInsertToken = (token: string) => {
    setFormula((prev) => `${prev} ${token}`);
  };

  return (
    <div className="fixed inset-0 z-50 overflow-y-auto bg-black bg-opacity-50 flex items-center justify-center p-4">
      <div className="bg-white rounded-xl shadow-2xl max-w-4xl w-full p-6 max-h-[90vh] flex flex-col">
        {/* Header */}
        <div className="flex justify-between items-center pb-4 border-b">
          <div className="flex items-center space-x-2">
            <Calculator className="w-6 h-6 text-primary-600" />
            <div>
              <h2 className="text-xl font-bold text-gray-800">Simulador de Motor de Custos (Estilo Excel)</h2>
              <p className="text-xs text-gray-500">
                Calcule o Custo de Entrada / Landed Cost com suporte a fórmulas dinâmicas e variáveis personalizadas.
              </p>
            </div>
          </div>
          <button onClick={onClose} className="text-gray-400 hover:text-gray-600 transition">
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Content body */}
        <div className="flex-1 overflow-y-auto py-4 space-y-6">
          {/* Formula Editor */}
          <div>
            <label className="block text-sm font-semibold text-gray-700 mb-1">
              Fórmula de Custo (inicie com =)
            </label>
            <div className="relative">
              <input
                type="text"
                value={formula}
                onChange={(e) => setFormula(e.target.value)}
                placeholder="Ex: = (INVOICE_UNIT_PRICE * (1 + FATOR_QUEBRA)) + (FREIGHT_ALLOCATED / QUANTITY)"
                className="w-full font-mono text-sm px-4 py-2 border rounded-lg focus:ring-2 focus:ring-primary-500 focus:outline-none bg-gray-50 text-gray-900 border-gray-300"
              />
            </div>

            {/* Quick Token Pills */}
            <div className="mt-2 flex flex-wrap gap-1.5 items-center">
              <span className="text-xs text-gray-500 mr-1 flex items-center">
                <HelpCircle className="w-3.5 h-3.5 mr-0.5" /> Tokens:
              </span>
              {[
                'INVOICE_UNIT_PRICE',
                'QUANTITY',
                'FREIGHT_ALLOCATED',
                'INSURANCE_ALLOCATED',
                'OTHER_EXPENSES_ALLOCATED',
                'ICMS_CREDIT',
                'PIS_CREDIT',
                'COFINS_CREDIT',
                'IPI_COST',
                'ICMS_ST_COST',
                'ROUND(x, 2)',
                'IF(cond, v1, v2)',
                'MAX(a, b)',
                'MIN(a, b)',
              ].map((token) => (
                <button
                  key={token}
                  type="button"
                  onClick={() => handleInsertToken(token)}
                  className="px-2 py-0.5 text-xs bg-gray-100 hover:bg-gray-200 text-gray-700 rounded border border-gray-300 font-mono transition"
                >
                  +{token}
                </button>
              ))}
            </div>
          </div>

          {/* Standard Variables Grid */}
          <div>
            <h3 className="text-xs font-semibold text-gray-500 uppercase tracking-wider mb-2">
              Variáveis Nativas da Nota Fiscal / Pedido
            </h3>
            <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-3 bg-gray-50 p-3 rounded-lg border">
              {[
                { key: 'INVOICE_UNIT_PRICE', label: 'Preço Unitário (NF)' },
                { key: 'QUANTITY', label: 'Quantidade Faturada' },
                { key: 'FREIGHT_ALLOCATED', label: 'Frete Rateado (R$)' },
                { key: 'INSURANCE_ALLOCATED', label: 'Seguro Rateado (R$)' },
                { key: 'OTHER_EXPENSES_ALLOCATED', label: 'Desp. Acessórias (R$)' },
                { key: 'ICMS_CREDIT', label: 'Crédito ICMS (Unitário)' },
                { key: 'PIS_CREDIT', label: 'Crédito PIS (Unitário)' },
                { key: 'COFINS_CREDIT', label: 'Crédito COFINS (Unitário)' },
                { key: 'IPI_COST', label: 'Custo IPI (Unitário)' },
                { key: 'ICMS_ST_COST', label: 'Custo ICMS ST (Unitário)' },
              ].map(({ key, label }) => (
                <div key={key}>
                  <label className="block text-[11px] text-gray-600 truncate font-medium" title={label}>
                    {label}
                  </label>
                  <input
                    type="number"
                    step="0.01"
                    value={variables[key] ?? 0}
                    onChange={(e) =>
                      setVariables((prev) => ({
                        ...prev,
                        [key]: parseFloat(e.target.value) || 0,
                      }))
                    }
                    className="w-full text-xs px-2 py-1 border rounded bg-white font-mono"
                  />
                </div>
              ))}
            </div>
          </div>

          {/* User Custom Variables */}
          <div>
            <div className="flex justify-between items-center mb-2">
              <h3 className="text-xs font-semibold text-gray-500 uppercase tracking-wider">
                Variáveis Personalizadas da Empresa
              </h3>
            </div>

            {/* Custom Variables List */}
            {customVarList.length > 0 ? (
              <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-3 mb-3">
                {customVarList.map((cv) => (
                  <div
                    key={cv.name}
                    className="flex items-center justify-between p-2.5 bg-blue-50/60 border border-blue-200 rounded-lg"
                  >
                    <div className="flex-1 mr-2">
                      <span className="text-xs font-bold text-blue-900 block font-mono">{cv.name}</span>
                      <span className="text-[11px] text-blue-700 block truncate">{cv.label}</span>
                    </div>
                    <div className="flex items-center space-x-1">
                      <input
                        type="number"
                        step="0.001"
                        value={variables[cv.name] ?? cv.val}
                        onChange={(e) => {
                          const val = parseFloat(e.target.value) || 0;
                          setVariables((prev) => ({ ...prev, [cv.name]: val }));
                        }}
                        className="w-20 text-xs px-2 py-1 border border-blue-300 rounded bg-white font-mono text-right"
                      />
                      <button
                        type="button"
                        onClick={() => handleRemoveCustomVar(cv.name)}
                        className="text-red-400 hover:text-red-600 p-1"
                        title="Remover variável"
                      >
                        <Trash2 className="w-3.5 h-3.5" />
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            ) : (
              <p className="text-xs text-gray-400 italic mb-3">
                Nenhuma variável personalizada definida ainda. Crie variáveis como FATOR_QUEBRA, TAXA_DESCARGA, etc.
              </p>
            )}

            {/* Add Custom Variable Form */}
            <div className="flex flex-wrap gap-2 items-end bg-gray-50 p-3 rounded-lg border border-dashed border-gray-300">
              <div className="flex-1 min-w-[120px]">
                <label className="block text-[11px] text-gray-600 font-medium">Nome (Ex: FATOR_QUEBRA)</label>
                <input
                  type="text"
                  value={newVarName}
                  onChange={(e) => setNewVarName(e.target.value)}
                  placeholder="NOME_VARIAVEL"
                  className="w-full text-xs px-2 py-1 border rounded bg-white font-mono uppercase"
                />
              </div>
              <div className="flex-1 min-w-[160px]">
                <label className="block text-[11px] text-gray-600 font-medium">Rótulo / Descrição</label>
                <input
                  type="text"
                  value={newVarLabel}
                  onChange={(e) => setNewVarLabel(e.target.value)}
                  placeholder="Ex: Fator de Quebra na Descarga"
                  className="w-full text-xs px-2 py-1 border rounded bg-white"
                />
              </div>
              <div className="w-24">
                <label className="block text-[11px] text-gray-600 font-medium">Valor Padrão</label>
                <input
                  type="number"
                  step="0.001"
                  value={newVarValue}
                  onChange={(e) => setNewVarValue(e.target.value)}
                  className="w-full text-xs px-2 py-1 border rounded bg-white font-mono"
                />
              </div>
              <button
                type="button"
                onClick={handleAddCustomVar}
                disabled={!newVarName.trim()}
                className="px-3 py-1 bg-primary-600 text-white rounded text-xs hover:bg-primary-700 disabled:opacity-50 flex items-center h-8"
              >
                <Plus className="w-3.5 h-3.5 mr-1" /> Adicionar
              </button>
            </div>
          </div>

          {/* Results / Error Area */}
          {result !== null && (
            <div className="p-4 bg-emerald-50 border border-emerald-200 rounded-xl flex items-center justify-between">
              <div className="flex items-center space-x-3">
                <CheckCircle2 className="w-6 h-6 text-emerald-600" />
                <div>
                  <span className="text-xs font-semibold text-emerald-800 uppercase tracking-wider block">
                    Custo Unitário Final Calculado (Landed Cost)
                  </span>
                  <span className="text-2xl font-bold font-mono text-emerald-900">
                    {new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL', minimumFractionDigits: 4 }).format(result)}
                  </span>
                </div>
              </div>
              <span className="text-xs text-emerald-700 bg-emerald-100 px-3 py-1 rounded-full font-medium">
                Cálculo Executado com Sucesso
              </span>
            </div>
          )}

          {errorMsg && (
            <div className="p-4 bg-red-50 border border-red-200 rounded-xl flex items-start space-x-3 text-red-800">
              <AlertTriangle className="w-5 h-5 text-red-600 flex-shrink-0 mt-0.5" />
              <div>
                <span className="text-xs font-semibold uppercase tracking-wider block text-red-900">
                  Erro de Validação da Fórmula
                </span>
                <p className="text-sm font-mono mt-1 text-red-700">{errorMsg}</p>
              </div>
            </div>
          )}
        </div>

        {/* Footer actions */}
        <div className="pt-4 border-t flex justify-end space-x-3">
          <button
            type="button"
            onClick={onClose}
            className="px-4 py-2 border rounded-lg text-gray-700 hover:bg-gray-50 text-sm font-medium transition"
          >
            Fechar
          </button>
          <button
            type="button"
            onClick={handleSimulate}
            disabled={evaluating || !formula.trim()}
            className="px-5 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 text-sm font-medium flex items-center shadow disabled:opacity-50 transition"
          >
            <Calculator className="w-4 h-4 mr-2" />
            {evaluating ? 'Calculando...' : 'Executar Simulação'}
          </button>
        </div>
      </div>
    </div>
  );
};
