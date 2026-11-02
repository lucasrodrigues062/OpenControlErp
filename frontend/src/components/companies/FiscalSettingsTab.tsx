import React, { useState } from 'react';
import { DanfeLayout, FiscalSettingsDto, NfeEnvironment, PisCofinsRegime, TaxRegime } from '../../types/companyBranches';
import { useI18n } from '../../context/I18nContext';
import { Save, ShieldCheck } from 'lucide-react';

interface FiscalSettingsTabProps {
  initialSettings?: FiscalSettingsDto;
  onSave: (settings: FiscalSettingsDto) => Promise<void>;
}

export const FiscalSettingsTab: React.FC<FiscalSettingsTabProps> = ({ initialSettings, onSave }) => {
  const { t } = useI18n();
  const [loading, setLoading] = useState(false);
  const [success, setSuccess] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [taxRegime, setTaxRegime] = useState<TaxRegime>(initialSettings?.taxRegime || 'SIMPLES_NACIONAL');
  const [pisCofinsRegime, setPisCofinsRegime] = useState<PisCofinsRegime>(initialSettings?.pisCofinsRegime || 'CUMULATIVO');
  const [isTaxSubstitute, setIsTaxSubstitute] = useState(initialSettings?.isTaxSubstitute || false);
  const [isIpiTaxpayer, setIsIpiTaxpayer] = useState(initialSettings?.isIpiTaxpayer || false);
  const [isCulturalPromoter, setIsCulturalPromoter] = useState(initialSettings?.isCulturalPromoter || false);
  const [calculateFcp, setCalculateFcp] = useState(initialSettings?.calculateFcp || false);
  const [deductIcmsFromBasis, setDeductIcmsFromBasis] = useState(initialSettings?.deductIcmsFromPiscofinsBasis ?? true);

  const [icmsSnCreditRate, setIcmsSnCreditRate] = useState<number>(initialSettings?.icmsSnCreditRate || 0);
  const [issRate, setIssRate] = useState<number>(initialSettings?.issRate || 0);
  const [cbsRate, setCbsRate] = useState<number>(initialSettings?.cbsRate || 0);

  const [nfeEnvironment, setNfeEnvironment] = useState<NfeEnvironment>(initialSettings?.nfeEnvironment || 'HOMOLOGATION');
  const [nfeSeries, setNfeSeries] = useState(initialSettings?.nfeSeries || '1');
  const [nfeModel, setNfeModel] = useState(initialSettings?.nfeModel || '55');
  const [nfseSeries, setNfseSeries] = useState(initialSettings?.nfseSeries || '');
  const [mdfeSeries, setMdfeSeries] = useState(initialSettings?.mdfeSeries || '');
  const [danfeLayout, setDanfeLayout] = useState<DanfeLayout>(initialSettings?.danfeLayout || 'PORTRAIT');
  const [defaultInvoiceMessage, setDefaultInvoiceMessage] = useState(initialSettings?.defaultInvoiceMessage || '');

  const [nfseUser, setNfseUser] = useState(initialSettings?.nfseUser || '');
  const [nfsePassword, setNfsePassword] = useState('');
  const [nfseAccessKey, setNfseAccessKey] = useState(initialSettings?.nfseAccessKey || '');

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError(null);
    setSuccess(false);

    try {
      const payload: FiscalSettingsDto = {
        taxRegime,
        pisCofinsRegime,
        isTaxSubstitute,
        isIpiTaxpayer,
        isCulturalPromoter,
        calculateFcp,
        deductIcmsFromPiscofinsBasis: deductIcmsFromBasis,
        icmsSnCreditRate,
        issRate,
        cbsRate,
        nfeEnvironment,
        nfeSeries,
        nfeModel,
        nfseSeries: nfseSeries.trim() || undefined,
        mdfeSeries: mdfeSeries.trim() || undefined,
        danfeLayout,
        defaultInvoiceMessage: defaultInvoiceMessage.trim() || undefined,
        nfseUser: nfseUser.trim() || undefined,
        nfsePassword: nfsePassword.trim() || undefined,
        nfseAccessKey: nfseAccessKey.trim() || undefined,
      };

      await onSave(payload);
      setSuccess(true);
      setTimeout(() => setSuccess(false), 4000);
    } catch (err: any) {
      setError(err?.message || 'Error saving fiscal settings');
    } finally {
      setLoading(false);
    }
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-6">
      {error && (
        <div className="p-3 bg-red-50 dark:bg-red-950/40 text-red-700 dark:text-red-300 rounded-lg text-xs border border-red-200 dark:border-red-800">
          {error}
        </div>
      )}
      {success && (
        <div className="p-3 bg-emerald-50 dark:bg-emerald-950/40 text-emerald-700 dark:text-emerald-300 rounded-lg text-xs border border-emerald-200 dark:border-emerald-800 flex items-center gap-2">
          <ShieldCheck size={16} />
          {t('companies.fiscal.saveSuccess')}
        </div>
      )}

      {/* Regimes & Tax Framework */}
      <div className="space-y-4">
        <h4 className="text-xs font-semibold uppercase tracking-wider text-slate-500 dark:text-slate-400">
          Regimes de Tributação
        </h4>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <label className="block text-xs font-medium text-slate-700 dark:text-slate-300 mb-1">
              {t('companies.fiscal.taxRegime')} *
            </label>
            <select
              value={taxRegime}
              onChange={(e) => setTaxRegime(e.target.value as TaxRegime)}
              className="w-full bg-white dark:bg-slate-950 border border-slate-300 dark:border-slate-800 rounded-lg px-3 py-2 text-sm text-slate-900 dark:text-slate-100 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary"
            >
              <option value="SIMPLES_NACIONAL">{t('companies.fiscal.regimes.SIMPLES_NACIONAL')}</option>
              <option value="SIMPLES_EXCESSO_SUBLIMITE">{t('companies.fiscal.regimes.SIMPLES_EXCESSO_SUBLIMITE')}</option>
              <option value="REGIME_NORMAL">{t('companies.fiscal.regimes.REGIME_NORMAL')}</option>
            </select>
          </div>

          <div>
            <label className="block text-xs font-medium text-slate-700 dark:text-slate-300 mb-1">
              {t('companies.fiscal.pisCofinsRegime')} *
            </label>
            <select
              value={pisCofinsRegime}
              onChange={(e) => setPisCofinsRegime(e.target.value as PisCofinsRegime)}
              className="w-full bg-white dark:bg-slate-950 border border-slate-300 dark:border-slate-800 rounded-lg px-3 py-2 text-sm text-slate-900 dark:text-slate-100 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary"
            >
              <option value="CUMULATIVO">{t('companies.fiscal.pisCofins.CUMULATIVO')}</option>
              <option value="NAO_CUMULATIVO">{t('companies.fiscal.pisCofins.NAO_CUMULATIVO')}</option>
            </select>
          </div>
        </div>

        {/* Checkbox Flags */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-3 pt-2">
          <label className="flex items-center gap-2 cursor-pointer text-xs text-slate-700 dark:text-slate-300">
            <input
              type="checkbox"
              checked={isTaxSubstitute}
              onChange={(e) => setIsTaxSubstitute(e.target.checked)}
              className="rounded border-slate-300 text-primary focus:ring-primary"
            />
            {t('companies.fiscal.isTaxSubstitute')}
          </label>

          <label className="flex items-center gap-2 cursor-pointer text-xs text-slate-700 dark:text-slate-300">
            <input
              type="checkbox"
              checked={isIpiTaxpayer}
              onChange={(e) => setIsIpiTaxpayer(e.target.checked)}
              className="rounded border-slate-300 text-primary focus:ring-primary"
            />
            {t('companies.fiscal.isIpiTaxpayer')}
          </label>

          <label className="flex items-center gap-2 cursor-pointer text-xs text-slate-700 dark:text-slate-300">
            <input
              type="checkbox"
              checked={isCulturalPromoter}
              onChange={(e) => setIsCulturalPromoter(e.target.checked)}
              className="rounded border-slate-300 text-primary focus:ring-primary"
            />
            {t('companies.fiscal.isCulturalPromoter')}
          </label>

          <label className="flex items-center gap-2 cursor-pointer text-xs text-slate-700 dark:text-slate-300">
            <input
              type="checkbox"
              checked={calculateFcp}
              onChange={(e) => setCalculateFcp(e.target.checked)}
              className="rounded border-slate-300 text-primary focus:ring-primary"
            />
            {t('companies.fiscal.calculateFcp')}
          </label>

          <label className="flex items-center gap-2 cursor-pointer text-xs text-slate-700 dark:text-slate-300 md:col-span-2">
            <input
              type="checkbox"
              checked={deductIcmsFromBasis}
              onChange={(e) => setDeductIcmsFromBasis(e.target.checked)}
              className="rounded border-slate-300 text-primary focus:ring-primary"
            />
            {t('companies.fiscal.deductIcmsFromBasis')}
          </label>
        </div>
      </div>

      {/* Aliquots */}
      <div className="space-y-4 pt-3 border-t border-slate-200 dark:border-slate-800">
        <h4 className="text-xs font-semibold uppercase tracking-wider text-slate-500 dark:text-slate-400">
          Alíquotas Padrão
        </h4>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <div>
            <label className="block text-xs font-medium text-slate-700 dark:text-slate-300 mb-1">
              {t('companies.fiscal.icmsCreditRate')}
            </label>
            <input
              type="number"
              step="0.001"
              value={icmsSnCreditRate}
              onChange={(e) => setIcmsSnCreditRate(parseFloat(e.target.value) || 0)}
              className="w-full bg-white dark:bg-slate-950 border border-slate-300 dark:border-slate-800 rounded-lg px-3 py-2 text-sm text-slate-900 dark:text-slate-100 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary"
            />
          </div>

          <div>
            <label className="block text-xs font-medium text-slate-700 dark:text-slate-300 mb-1">
              {t('companies.fiscal.issRate')}
            </label>
            <input
              type="number"
              step="0.001"
              value={issRate}
              onChange={(e) => setIssRate(parseFloat(e.target.value) || 0)}
              className="w-full bg-white dark:bg-slate-950 border border-slate-300 dark:border-slate-800 rounded-lg px-3 py-2 text-sm text-slate-900 dark:text-slate-100 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary"
            />
          </div>

          <div>
            <label className="block text-xs font-medium text-slate-700 dark:text-slate-300 mb-1">
              {t('companies.fiscal.cbsRate')}
            </label>
            <input
              type="number"
              step="0.001"
              value={cbsRate}
              onChange={(e) => setCbsRate(parseFloat(e.target.value) || 0)}
              className="w-full bg-white dark:bg-slate-950 border border-slate-300 dark:border-slate-800 rounded-lg px-3 py-2 text-sm text-slate-900 dark:text-slate-100 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary"
            />
          </div>
        </div>
      </div>

      {/* Electronic Documents (SEFAZ / NF-e / NFS-e / MDF-e) */}
      <div className="space-y-4 pt-3 border-t border-slate-200 dark:border-slate-800">
        <h4 className="text-xs font-semibold uppercase tracking-wider text-slate-500 dark:text-slate-400">
          Documentos Eletrônicos (NF-e, NFS-e, MDF-e)
        </h4>

        <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
          <div>
            <label className="block text-xs font-medium text-slate-700 dark:text-slate-300 mb-1">
              {t('companies.fiscal.nfeEnvironment')}
            </label>
            <select
              value={nfeEnvironment}
              onChange={(e) => setNfeEnvironment(e.target.value as NfeEnvironment)}
              className="w-full bg-white dark:bg-slate-950 border border-slate-300 dark:border-slate-800 rounded-lg px-3 py-2 text-sm text-slate-900 dark:text-slate-100 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary"
            >
              <option value="HOMOLOGATION">{t('companies.fiscal.environments.HOMOLOGATION')}</option>
              <option value="PRODUCTION">{t('companies.fiscal.environments.PRODUCTION')}</option>
            </select>
          </div>

          <div>
            <label className="block text-xs font-medium text-slate-700 dark:text-slate-300 mb-1">
              {t('companies.fiscal.nfeSeries')}
            </label>
            <input
              type="text"
              value={nfeSeries}
              onChange={(e) => setNfeSeries(e.target.value)}
              className="w-full bg-white dark:bg-slate-950 border border-slate-300 dark:border-slate-800 rounded-lg px-3 py-2 text-sm text-slate-900 dark:text-slate-100 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary"
            />
          </div>

          <div>
            <label className="block text-xs font-medium text-slate-700 dark:text-slate-300 mb-1">
              {t('companies.fiscal.nfeModel')}
            </label>
            <input
              type="text"
              value={nfeModel}
              onChange={(e) => setNfeModel(e.target.value)}
              className="w-full bg-white dark:bg-slate-950 border border-slate-300 dark:border-slate-800 rounded-lg px-3 py-2 text-sm text-slate-900 dark:text-slate-100 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary"
            />
          </div>

          <div>
            <label className="block text-xs font-medium text-slate-700 dark:text-slate-300 mb-1">
              {t('companies.fiscal.danfeLayout')}
            </label>
            <select
              value={danfeLayout}
              onChange={(e) => setDanfeLayout(e.target.value as DanfeLayout)}
              className="w-full bg-white dark:bg-slate-950 border border-slate-300 dark:border-slate-800 rounded-lg px-3 py-2 text-sm text-slate-900 dark:text-slate-100 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary"
            >
              <option value="PORTRAIT">{t('companies.fiscal.layouts.PORTRAIT')}</option>
              <option value="LANDSCAPE">{t('companies.fiscal.layouts.LANDSCAPE')}</option>
            </select>
          </div>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <label className="block text-xs font-medium text-slate-700 dark:text-slate-300 mb-1">
              {t('companies.fiscal.nfseSeries')}
            </label>
            <input
              type="text"
              value={nfseSeries}
              onChange={(e) => setNfseSeries(e.target.value)}
              placeholder="ex: 1"
              className="w-full bg-white dark:bg-slate-950 border border-slate-300 dark:border-slate-800 rounded-lg px-3 py-2 text-sm text-slate-900 dark:text-slate-100 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary"
            />
          </div>

          <div>
            <label className="block text-xs font-medium text-slate-700 dark:text-slate-300 mb-1">
              {t('companies.fiscal.mdfeSeries')}
            </label>
            <input
              type="text"
              value={mdfeSeries}
              onChange={(e) => setMdfeSeries(e.target.value)}
              placeholder="ex: 1"
              className="w-full bg-white dark:bg-slate-950 border border-slate-300 dark:border-slate-800 rounded-lg px-3 py-2 text-sm text-slate-900 dark:text-slate-100 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary"
            />
          </div>
        </div>

        {/* NFS-e Municipal Credentials */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4 pt-2">
          <div>
            <label className="block text-xs font-medium text-slate-700 dark:text-slate-300 mb-1">
              {t('companies.fiscal.nfseUser')}
            </label>
            <input
              type="text"
              value={nfseUser}
              onChange={(e) => setNfseUser(e.target.value)}
              placeholder="Usuário prefeitura"
              className="w-full bg-white dark:bg-slate-950 border border-slate-300 dark:border-slate-800 rounded-lg px-3 py-2 text-sm text-slate-900 dark:text-slate-100 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary"
            />
          </div>

          <div>
            <label className="block text-xs font-medium text-slate-700 dark:text-slate-300 mb-1">
              {t('companies.fiscal.nfsePassword')}
            </label>
            <input
              type="password"
              value={nfsePassword}
              onChange={(e) => setNfsePassword(e.target.value)}
              placeholder="••••••••"
              className="w-full bg-white dark:bg-slate-950 border border-slate-300 dark:border-slate-800 rounded-lg px-3 py-2 text-sm text-slate-900 dark:text-slate-100 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary"
            />
          </div>

          <div>
            <label className="block text-xs font-medium text-slate-700 dark:text-slate-300 mb-1">
              {t('companies.fiscal.nfseAccessKey')}
            </label>
            <input
              type="text"
              value={nfseAccessKey}
              onChange={(e) => setNfseAccessKey(e.target.value)}
              placeholder="Chave/Frase secreta"
              className="w-full bg-white dark:bg-slate-950 border border-slate-300 dark:border-slate-800 rounded-lg px-3 py-2 text-sm text-slate-900 dark:text-slate-100 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary"
            />
          </div>
        </div>

        <div>
          <label className="block text-xs font-medium text-slate-700 dark:text-slate-300 mb-1">
            {t('companies.fiscal.defaultMessage')}
          </label>
          <textarea
            rows={2}
            value={defaultInvoiceMessage}
            onChange={(e) => setDefaultInvoiceMessage(e.target.value)}
            placeholder="Informações adicionais de interesse do contribuinte..."
            className="w-full bg-white dark:bg-slate-950 border border-slate-300 dark:border-slate-800 rounded-lg px-3 py-2 text-sm text-slate-900 dark:text-slate-100 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary resize-none"
          />
        </div>
      </div>

      <div className="flex justify-end pt-3">
        <button
          type="submit"
          disabled={loading}
          className="inline-flex items-center gap-1.5 px-4 py-2 bg-primary hover:bg-primary/90 text-primary-foreground rounded-lg text-sm font-medium transition shadow-sm disabled:opacity-50"
        >
          <Save size={16} />
          {loading ? t('common.loading') : t('common.save')}
        </button>
      </div>
    </form>
  );
};
