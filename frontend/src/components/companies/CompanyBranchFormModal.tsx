import React, { useState, useEffect } from 'react';
import {
  AccountantDto,
  CompanyBranchDetail,
  CompanyBranchSummary,
  CreateCompanyBranchRequest,
  EstablishmentType,
  FiscalSettingsDto,
  UpdateCompanyBranchRequest,
} from '../../types/companyBranches';
import { useI18n } from '../../context/I18nContext';
import { FiscalSettingsTab } from './FiscalSettingsTab';
import { AccountantTab } from './AccountantTab';
import { CertificateTab } from './CertificateTab';
import { CustomFieldFormSection } from '../customFields/CustomFieldFormSection';
import { companyBranchesApi } from '../../api/companyBranchesApi';
import { customFieldsApi } from '../../api/customFieldsApi';
import { Building2, Upload, X, Sparkles } from 'lucide-react';
import { generateBranchCode } from '../../utils/codeGenerator';

interface CompanyBranchFormModalProps {
  isOpen: boolean;
  onClose: () => void;
  branchToEdit?: CompanyBranchDetail | null;
  defaultType?: EstablishmentType;
  headquartersList: CompanyBranchSummary[];
  onSave: (payload: CreateCompanyBranchRequest | UpdateCompanyBranchRequest) => Promise<void>;
  onRefreshDetail?: (id: string) => Promise<CompanyBranchDetail>;
}

export const CompanyBranchFormModal: React.FC<CompanyBranchFormModalProps> = ({
  isOpen,
  onClose,
  branchToEdit,
  defaultType = 'HEADQUARTERS',
  headquartersList,
  onSave,
  onRefreshDetail,
}) => {
  const { t } = useI18n();
  const [activeTab, setActiveTab] = useState<'general' | 'fiscal' | 'certificate' | 'accountant' | 'customFields'>('general');

  const [type, setType] = useState<EstablishmentType>(defaultType);
  const [parentId, setParentId] = useState<string>('');
  const [code, setCode] = useState('');
  const [tradeName, setTradeName] = useState('');
  const [legalName, setLegalName] = useState('');
  const [cnpj, setCnpj] = useState('');
  const [stateRegistration, setStateRegistration] = useState('');
  const [isStateRegistrationExempt, setIsStateRegistrationExempt] = useState(false);
  const [municipalRegistration, setMunicipalRegistration] = useState('');
  const [mainCnae, setMainCnae] = useState('');
  const [nfseCnae, setNfseCnae] = useState('');
  const [phone1, setPhone1] = useState('');
  const [phone2, setPhone2] = useState('');
  const [email, setEmail] = useState('');
  const [responsiblePerson, setResponsiblePerson] = useState('');
  const [website, setWebsite] = useState('');
  const [logoUrl, setLogoUrl] = useState('');

  // Address
  const [postalCode, setPostalCode] = useState('');
  const [street, setStreet] = useState('');
  const [number, setNumber] = useState('');
  const [complement, setComplement] = useState('');
  const [neighborhood, setNeighborhood] = useState('');
  const [cityName, setCityName] = useState('');
  const [cityIbgeCode, setCityIbgeCode] = useState<number | undefined>(undefined);
  const [stateCode, setStateCode] = useState('');

  // Custom field values
  const [customFieldValues, setCustomFieldValues] = useState<Record<string, any>>({});

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (branchToEdit) {
      setType(branchToEdit.type);
      setParentId(branchToEdit.parentId || '');
      setCode(branchToEdit.code);
      setTradeName(branchToEdit.tradeName);
      setLegalName(branchToEdit.legalName);
      setCnpj(branchToEdit.cnpj);
      setStateRegistration(branchToEdit.stateRegistration || '');
      setIsStateRegistrationExempt(branchToEdit.isStateRegistrationExempt ?? (branchToEdit as any).stateRegistrationExempt ?? false);
      setMunicipalRegistration(branchToEdit.municipalRegistration || '');
      setMainCnae(branchToEdit.mainCnae || '');
      setNfseCnae(branchToEdit.nfseCnae || '');
      setPhone1(branchToEdit.phone1 || '');
      setPhone2(branchToEdit.phone2 || '');
      setEmail(branchToEdit.email || '');
      setResponsiblePerson(branchToEdit.responsiblePerson || '');
      setWebsite(branchToEdit.website || '');
      setLogoUrl(branchToEdit.logoUrl || '');

      setPostalCode(branchToEdit.address.postalCode || '');
      setStreet(branchToEdit.address.street || '');
      setNumber(branchToEdit.address.number || '');
      setComplement(branchToEdit.address.complement || '');
      setNeighborhood(branchToEdit.address.neighborhood || '');
      setCityName(branchToEdit.address.cityName);
      setCityIbgeCode(branchToEdit.address.cityIbgeCode);
      setStateCode(branchToEdit.address.stateCode);

      // Load custom field values
      customFieldsApi.getEntityCustomValues('COMPANY', branchToEdit.id).then((res: any) => {
        setCustomFieldValues(res.values || {});
      }).catch(() => {});
    } else {
      setType(defaultType);
      setParentId(headquartersList[0]?.id || '');
      setCode(generateBranchCode());
      setTradeName('');
      setLegalName('');
      setCnpj('');
      setStateRegistration('');
      setIsStateRegistrationExempt(false);
      setMunicipalRegistration('');
      setMainCnae('');
      setNfseCnae('');
      setPhone1('');
      setPhone2('');
      setEmail('');
      setResponsiblePerson('');
      setWebsite('');
      setLogoUrl('');
      setPostalCode('');
      setStreet('');
      setNumber('');
      setComplement('');
      setNeighborhood('');
      setCityName('');
      setCityIbgeCode(undefined);
      setStateCode('');
      setCustomFieldValues({});
      setActiveTab('general');
    }
    setError(null);
  }, [branchToEdit, defaultType, headquartersList, isOpen]);

  // Mask & Auto-format CNPJ
  const handleCnpjChange = (val: string) => {
    const raw = val.replace(/\D/g, '').slice(0, 14);
    if (raw.length <= 2) setCnpj(raw);
    else if (raw.length <= 5) setCnpj(`${raw.slice(0, 2)}.${raw.slice(2)}`);
    else if (raw.length <= 8) setCnpj(`${raw.slice(0, 2)}.${raw.slice(2, 5)}.${raw.slice(5)}`);
    else if (raw.length <= 12) setCnpj(`${raw.slice(0, 2)}.${raw.slice(2, 5)}.${raw.slice(5, 8)}/${raw.slice(8)}`);
    else setCnpj(`${raw.slice(0, 2)}.${raw.slice(2, 5)}.${raw.slice(5, 8)}/${raw.slice(8, 12)}-${raw.slice(12, 14)}`);
  };

  // CEP Auto-complete via ViaCEP
  const handleCepChange = async (val: string) => {
    const digits = val.replace(/\D/g, '').slice(0, 8);
    setPostalCode(digits.length > 5 ? `${digits.slice(0, 5)}-${digits.slice(5)}` : digits);

    if (digits.length === 8) {
      try {
        const res = await fetch(`https://viacep.com.br/ws/${digits}/json/`);
        const data = await res.json();
        if (!data.erro) {
          if (data.logradouro) setStreet(data.logradouro);
          if (data.bairro) setNeighborhood(data.bairro);
          if (data.localidade) setCityName(data.localidade);
          if (data.uf) setStateCode(data.uf);
          if (data.ibge) setCityIbgeCode(parseInt(data.ibge, 10));
        }
      } catch {
        // Fallback silently if offline
      }
    }
  };

  const handleGeneralSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!tradeName.trim() || !legalName.trim() || !cityName.trim() || !stateCode.trim()) {
      setError('Por favor preencha todos os campos obrigatórios (*)');
      return;
    }

    if (!branchToEdit && !code.trim()) {
      setError('Código do estabelecimento é obrigatório');
      return;
    }

    if (!branchToEdit && !cnpj.trim()) {
      setError('CNPJ é obrigatório');
      return;
    }

    if (type === 'BRANCH' && !branchToEdit && !parentId) {
      setError('Selecione a empresa Matriz vinculada');
      return;
    }

    setLoading(true);
    setError(null);

    try {
      const address = {
        postalCode: postalCode.replace(/\D/g, '') || undefined,
        street: street.trim() || undefined,
        number: number.trim() || undefined,
        complement: complement.trim() || undefined,
        neighborhood: neighborhood.trim() || undefined,
        cityName: cityName.trim(),
        cityIbgeCode: cityIbgeCode || undefined,
        stateCode: stateCode.trim().toUpperCase(),
        countryCode: 'BRA',
      };

      if (branchToEdit) {
        const updateReq: UpdateCompanyBranchRequest = {
          tradeName: tradeName.trim(),
          legalName: legalName.trim(),
          stateRegistration: isStateRegistrationExempt ? undefined : stateRegistration.trim() || undefined,
          isStateRegistrationExempt,
          municipalRegistration: municipalRegistration.trim() || undefined,
          mainCnae: mainCnae.trim() || undefined,
          nfseCnae: nfseCnae.trim() || undefined,
          phone1: phone1.trim() || undefined,
          phone2: phone2.trim() || undefined,
          email: email.trim() || undefined,
          responsiblePerson: responsiblePerson.trim() || undefined,
          website: website.trim() || undefined,
          logoUrl: logoUrl.trim() || undefined,
          address,
        };
        await onSave(updateReq);

        // Save custom field values
        if (Object.keys(customFieldValues).length > 0) {
          await customFieldsApi.saveEntityCustomValues('COMPANY', branchToEdit.id, {
            values: customFieldValues,
          });
        }
      } else {
        const createReq: CreateCompanyBranchRequest = {
          type,
          parentId: type === 'BRANCH' ? parentId : undefined,
          code: (code.trim() || generateBranchCode()),
          tradeName: tradeName.trim(),
          legalName: legalName.trim(),
          cnpj: cnpj.replace(/\D/g, ''),
          stateRegistration: isStateRegistrationExempt ? undefined : stateRegistration.trim() || undefined,
          isStateRegistrationExempt,
          municipalRegistration: municipalRegistration.trim() || undefined,
          mainCnae: mainCnae.trim() || undefined,
          nfseCnae: nfseCnae.trim() || undefined,
          phone1: phone1.trim() || undefined,
          phone2: phone2.trim() || undefined,
          email: email.trim() || undefined,
          responsiblePerson: responsiblePerson.trim() || undefined,
          website: website.trim() || undefined,
          address,
        };
        await onSave(createReq);
      }
      onClose();
    } catch (err: any) {
      setError(err?.message || 'Error saving company');
    } finally {
      setLoading(false);
    }
  };

  const handleSaveFiscal = async (settings: FiscalSettingsDto) => {
    if (!branchToEdit) return;
    await companyBranchesApi.updateFiscalSettings(branchToEdit.id, settings);
    if (onRefreshDetail) await onRefreshDetail(branchToEdit.id);
  };

  const handleSaveAccountant = async (accountant: AccountantDto) => {
    if (!branchToEdit) return;
    await companyBranchesApi.updateAccountant(branchToEdit.id, accountant);
    if (onRefreshDetail) await onRefreshDetail(branchToEdit.id);
  };

  const handleUploadCert = async (file: File, pass: string) => {
    if (!branchToEdit) return;
    await companyBranchesApi.uploadCertificate(branchToEdit.id, file, pass);
    if (onRefreshDetail) await onRefreshDetail(branchToEdit.id);
  };

  const handleDeleteCert = async () => {
    if (!branchToEdit) return;
    await companyBranchesApi.deleteCertificate(branchToEdit.id);
    if (onRefreshDetail) await onRefreshDetail(branchToEdit.id);
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/50 backdrop-blur-sm overflow-y-auto">
      <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl shadow-2xl w-full max-w-4xl max-h-[90vh] flex flex-col overflow-hidden my-6">
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-slate-200 dark:border-slate-800 bg-slate-50 dark:bg-slate-950/50">
          <div className="flex items-center gap-3">
            <div className="p-2 rounded-lg bg-primary/10 text-primary">
              <Building2 size={20} />
            </div>
            <div>
              <h3 className="text-base font-semibold text-slate-900 dark:text-white">
                {branchToEdit
                  ? t('companies.editEstablishment')
                  : type === 'HEADQUARTERS'
                  ? t('companies.newHeadquarters')
                  : t('companies.newBranch')}
              </h3>
              <p className="text-xs text-slate-500 dark:text-slate-400">
                {t('companies.description')}
              </p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-1.5 text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 rounded-lg hover:bg-slate-100 dark:hover:bg-slate-800 transition"
          >
            <X size={18} />
          </button>
        </div>

        {/* Tab Navigation (only when editing) */}
        {branchToEdit && (
          <div className="flex border-b border-slate-200 dark:border-slate-800 px-6 gap-2 bg-slate-50/50 dark:bg-slate-950/20 text-xs font-medium">
            <button
              onClick={() => setActiveTab('general')}
              className={`py-3 px-3 border-b-2 transition ${
                activeTab === 'general'
                  ? 'border-primary text-primary font-semibold'
                  : 'border-transparent text-slate-500 hover:text-slate-800 dark:hover:text-slate-200'
              }`}
            >
              {t('companies.tabs.general')}
            </button>
            <button
              onClick={() => setActiveTab('fiscal')}
              className={`py-3 px-3 border-b-2 transition ${
                activeTab === 'fiscal'
                  ? 'border-primary text-primary font-semibold'
                  : 'border-transparent text-slate-500 hover:text-slate-800 dark:hover:text-slate-200'
              }`}
            >
              {t('companies.tabs.fiscal')}
            </button>
            <button
              onClick={() => setActiveTab('certificate')}
              className={`py-3 px-3 border-b-2 transition ${
                activeTab === 'certificate'
                  ? 'border-primary text-primary font-semibold'
                  : 'border-transparent text-slate-500 hover:text-slate-800 dark:hover:text-slate-200'
              }`}
            >
              {t('companies.tabs.certificate')}
            </button>
            <button
              onClick={() => setActiveTab('accountant')}
              className={`py-3 px-3 border-b-2 transition ${
                activeTab === 'accountant'
                  ? 'border-primary text-primary font-semibold'
                  : 'border-transparent text-slate-500 hover:text-slate-800 dark:hover:text-slate-200'
              }`}
            >
              {t('companies.tabs.accountant')}
            </button>
            <button
              onClick={() => setActiveTab('customFields')}
              className={`py-3 px-3 border-b-2 transition ${
                activeTab === 'customFields'
                  ? 'border-primary text-primary font-semibold'
                  : 'border-transparent text-slate-500 hover:text-slate-800 dark:hover:text-slate-200'
              }`}
            >
              {t('companies.tabs.customFields')}
            </button>
          </div>
        )}

        {/* Content Body */}
        <div className="flex-1 overflow-y-auto p-6">
          {error && (
            <div className="p-3 mb-4 bg-red-50 dark:bg-red-950/40 text-red-700 dark:text-red-300 rounded-lg text-xs border border-red-200 dark:border-red-800">
              {error}
            </div>
          )}

          {activeTab === 'general' && (
            <form onSubmit={handleGeneralSubmit} className="space-y-6">
              {/* Type & Parent Link (when creating) */}
              {!branchToEdit && (
                <div className="grid grid-cols-1 md:grid-cols-3 gap-4 p-4 rounded-xl bg-slate-50 dark:bg-slate-950/40 border border-slate-200 dark:border-slate-800">
                  <div>
                    <label className="block text-xs font-medium text-slate-700 dark:text-slate-300 mb-1">
                      {t('companies.type')} *
                    </label>
                    <select
                      value={type}
                      onChange={(e) => setType(e.target.value as EstablishmentType)}
                      className="w-full bg-white dark:bg-slate-900 border border-slate-300 dark:border-slate-800 rounded-lg px-3 py-2 text-sm text-slate-900 dark:text-slate-100 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary"
                    >
                      <option value="HEADQUARTERS">{t('companies.types.HEADQUARTERS')}</option>
                      <option value="BRANCH">{t('companies.types.BRANCH')}</option>
                    </select>
                  </div>

                  {type === 'BRANCH' && (
                    <div className="md:col-span-2">
                      <label className="block text-xs font-medium text-slate-700 dark:text-slate-300 mb-1">
                        {t('companies.parentHeadquarters')} *
                      </label>
                      <select
                        value={parentId}
                        onChange={(e) => setParentId(e.target.value)}
                        className="w-full bg-white dark:bg-slate-900 border border-slate-300 dark:border-slate-800 rounded-lg px-3 py-2 text-sm text-slate-900 dark:text-slate-100 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary"
                        required
                      >
                        <option value="">{t('companies.selectParentHeadquarters')}</option>
                        {headquartersList.map((hq) => (
                          <option key={hq.id} value={hq.id}>
                            {hq.tradeName} ({hq.cnpj})
                          </option>
                        ))}
                      </select>
                    </div>
                  )}
                </div>
              )}

              {/* Code, Trade Name, Legal Name */}
              <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
                <div>
                  <div className="flex items-center justify-between mb-1">
                    <label className="text-xs font-medium text-slate-700 dark:text-slate-300">
                      {t('companies.code')} *
                    </label>
                    {!branchToEdit && (
                      <button
                        type="button"
                        onClick={() => setCode(generateBranchCode())}
                        className="text-xs text-primary hover:text-primary/80 font-medium flex items-center gap-1 cursor-pointer"
                        title="Gerar novo código"
                      >
                        <Sparkles className="h-3 w-3" />
                        Gerar outro
                      </button>
                    )}
                  </div>
                  <input
                    type="text"
                    disabled={!!branchToEdit}
                    value={code}
                    onChange={(e) => setCode(e.target.value)}
                    placeholder="ex: 01"
                    className="w-full bg-white dark:bg-slate-950 border border-slate-300 dark:border-slate-800 rounded-lg px-3 py-2 text-sm text-slate-900 dark:text-slate-100 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary disabled:opacity-60"
                    required
                  />
                  {!branchToEdit ? (
                    <span className="text-[11px] text-slate-400 mt-0.5 block">Gerado automaticamente. Você pode alterar se desejar.</span>
                  ) : (
                    <span className="text-[11px] text-slate-400 mt-0.5 block">{t('companies.codeHelp')}</span>
                  )}
                </div>

                <div className="md:col-span-3">
                  <label className="block text-xs font-medium text-slate-700 dark:text-slate-300 mb-1">
                    {t('companies.tradeName')} *
                  </label>
                  <input
                    type="text"
                    value={tradeName}
                    onChange={(e) => setTradeName(e.target.value)}
                    placeholder={t('companies.tradeNamePlaceholder')}
                    className="w-full bg-white dark:bg-slate-950 border border-slate-300 dark:border-slate-800 rounded-lg px-3 py-2 text-sm text-slate-900 dark:text-slate-100 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary"
                    required
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-medium text-slate-700 dark:text-slate-300 mb-1">
                  {t('companies.legalName')} *
                </label>
                <input
                  type="text"
                  value={legalName}
                  onChange={(e) => setLegalName(e.target.value)}
                  placeholder={t('companies.legalNamePlaceholder')}
                  className="w-full bg-white dark:bg-slate-950 border border-slate-300 dark:border-slate-800 rounded-lg px-3 py-2 text-sm text-slate-900 dark:text-slate-100 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary"
                  required
                />
              </div>

              {/* Tax Identifiers (CNPJ, IE, IM, CNAEs) */}
              <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                <div>
                  <label className="block text-xs font-medium text-slate-700 dark:text-slate-300 mb-1">
                    {t('companies.cnpj')} *
                  </label>
                  <input
                    type="text"
                    disabled={!!branchToEdit}
                    value={cnpj}
                    onChange={(e) => handleCnpjChange(e.target.value)}
                    placeholder="00.000.000/0000-00"
                    className="w-full bg-white dark:bg-slate-950 border border-slate-300 dark:border-slate-800 rounded-lg px-3 py-2 text-sm text-slate-900 dark:text-slate-100 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary disabled:opacity-60"
                    required
                  />
                  <span className="text-[11px] text-slate-400 mt-0.5 block">{t('companies.cnpjHelp')}</span>
                </div>

                <div>
                  <div className="flex items-center justify-between mb-1">
                    <label className="block text-xs font-medium text-slate-700 dark:text-slate-300">
                      {t('companies.stateRegistration')}
                    </label>
                    <label className="flex items-center gap-1 text-[11px] text-slate-500 cursor-pointer">
                      <input
                        type="checkbox"
                        checked={isStateRegistrationExempt}
                        onChange={(e) => setIsStateRegistrationExempt(e.target.checked)}
                        className="rounded border-slate-300 text-primary"
                      />
                      Isento
                    </label>
                  </div>
                  <input
                    type="text"
                    disabled={isStateRegistrationExempt}
                    value={stateRegistration}
                    onChange={(e) => setStateRegistration(e.target.value)}
                    placeholder="Inscrição Estadual"
                    className="w-full bg-white dark:bg-slate-950 border border-slate-300 dark:border-slate-800 rounded-lg px-3 py-2 text-sm text-slate-900 dark:text-slate-100 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary disabled:opacity-50"
                  />
                </div>

                <div>
                  <label className="block text-xs font-medium text-slate-700 dark:text-slate-300 mb-1">
                    {t('companies.municipalRegistration')}
                  </label>
                  <input
                    type="text"
                    value={municipalRegistration}
                    onChange={(e) => setMunicipalRegistration(e.target.value)}
                    placeholder="Inscrição Municipal"
                    className="w-full bg-white dark:bg-slate-950 border border-slate-300 dark:border-slate-800 rounded-lg px-3 py-2 text-sm text-slate-900 dark:text-slate-100 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary"
                  />
                </div>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-medium text-slate-700 dark:text-slate-300 mb-1">
                    {t('companies.mainCnae')}
                  </label>
                  <input
                    type="text"
                    value={mainCnae}
                    onChange={(e) => setMainCnae(e.target.value)}
                    placeholder="ex: 6202-3/00"
                    className="w-full bg-white dark:bg-slate-950 border border-slate-300 dark:border-slate-800 rounded-lg px-3 py-2 text-sm text-slate-900 dark:text-slate-100 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary"
                  />
                </div>

                <div>
                  <label className="block text-xs font-medium text-slate-700 dark:text-slate-300 mb-1">
                    {t('companies.nfseCnae')}
                  </label>
                  <input
                    type="text"
                    value={nfseCnae}
                    onChange={(e) => setNfseCnae(e.target.value)}
                    placeholder="ex: 1.05"
                    className="w-full bg-white dark:bg-slate-950 border border-slate-300 dark:border-slate-800 rounded-lg px-3 py-2 text-sm text-slate-900 dark:text-slate-100 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary"
                  />
                </div>
              </div>


              {/* Logo & Website Card */}
              <div className="p-4 rounded-xl bg-slate-50 dark:bg-slate-900/60 border border-slate-200 dark:border-slate-800">
                <div className="flex flex-col sm:flex-row items-start sm:items-center gap-4">
                  <div className="relative group flex-shrink-0">
                    <div className="w-16 h-16 rounded-xl border-2 border-dashed border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-950 flex items-center justify-center overflow-hidden shadow-xs">
                      {logoUrl ? (
                        <img src={logoUrl} alt="Logo" className="w-full h-full object-contain p-1" />
                      ) : (
                        <Building2 className="w-8 h-8 text-slate-400" />
                      )}
                    </div>
                  </div>

                  <div className="flex-1 space-y-1">
                    <label className="text-xs font-medium text-slate-700 dark:text-slate-300 block">
                      {t('companies.logo')}
                    </label>
                    <p className="text-[11px] text-slate-500 dark:text-slate-400">
                      {t('companies.logoHelp')}
                    </p>
                    <div className="flex items-center gap-2 pt-1">
                      <label className="cursor-pointer inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium bg-white dark:bg-slate-800 border border-slate-300 dark:border-slate-700 text-slate-700 dark:text-slate-200 hover:bg-slate-50 dark:hover:bg-slate-700 transition-colors shadow-xs">
                        <Upload className="w-3.5 h-3.5" />
                        <span>{logoUrl ? t('companies.changeLogo') : t('companies.uploadLogo')}</span>
                        <input
                          type="file"
                          accept="image/png,image/jpeg,image/webp,image/svg+xml"
                          className="hidden"
                          onChange={(e) => {
                            const file = e.target.files?.[0];
                            if (file) {
                              if (file.size > 2 * 1024 * 1024) {
                                setError('O arquivo de imagem excede o tamanho máximo de 2MB');
                                return;
                              }
                              const reader = new FileReader();
                              reader.onload = (event) => {
                                setLogoUrl(event.target?.result as string);
                              };
                              reader.readAsDataURL(file);
                            }
                          }}
                        />
                      </label>
                      {logoUrl && (
                        <button
                          type="button"
                          onClick={() => setLogoUrl('')}
                          className="px-2.5 py-1.5 rounded-lg text-xs font-medium text-red-600 hover:bg-red-50 dark:hover:bg-red-950/40 transition-colors"
                        >
                          {t('companies.removeLogo')}
                        </button>
                      )}
                    </div>
                  </div>

                  <div className="w-full sm:w-64">
                    <label className="block text-xs font-medium text-slate-700 dark:text-slate-300 mb-1">
                      {t('companies.website')}
                    </label>
                    <input
                      type="url"
                      value={website}
                      onChange={(e) => setWebsite(e.target.value)}
                      placeholder="https://empresa.com.br"
                      className="w-full bg-white dark:bg-slate-950 border border-slate-300 dark:border-slate-800 rounded-lg px-3 py-2 text-sm text-slate-900 dark:text-slate-100 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary"
                    />
                  </div>
                </div>
              </div>

              {/* Contact Information */}
              <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                <div>
                  <label className="block text-xs font-medium text-slate-700 dark:text-slate-300 mb-1">
                    {t('companies.phone1')}
                  </label>
                  <input
                    type="text"
                    value={phone1}
                    onChange={(e) => setPhone1(e.target.value)}
                    placeholder="(11) 3333-0000"
                    className="w-full bg-white dark:bg-slate-950 border border-slate-300 dark:border-slate-800 rounded-lg px-3 py-2 text-sm text-slate-900 dark:text-slate-100 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary"
                  />
                </div>

                <div>
                  <label className="block text-xs font-medium text-slate-700 dark:text-slate-300 mb-1">
                    {t('companies.email')}
                  </label>
                  <input
                    type="email"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    placeholder="contato@empresa.com.br"
                    className="w-full bg-white dark:bg-slate-950 border border-slate-300 dark:border-slate-800 rounded-lg px-3 py-2 text-sm text-slate-900 dark:text-slate-100 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary"
                  />
                </div>

                <div>
                  <label className="block text-xs font-medium text-slate-700 dark:text-slate-300 mb-1">
                    {t('companies.responsiblePerson')}
                  </label>
                  <input
                    type="text"
                    value={responsiblePerson}
                    onChange={(e) => setResponsiblePerson(e.target.value)}
                    placeholder="Nome do Administrador"
                    className="w-full bg-white dark:bg-slate-950 border border-slate-300 dark:border-slate-800 rounded-lg px-3 py-2 text-sm text-slate-900 dark:text-slate-100 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary"
                  />
                </div>
              </div>

              {/* Statutory Address Section */}
              <div className="space-y-4 pt-3 border-t border-slate-200 dark:border-slate-800">
                <h4 className="text-xs font-semibold uppercase tracking-wider text-slate-500 dark:text-slate-400">
                  {t('companies.addressSection')}
                </h4>

                <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
                  <div>
                    <label className="block text-xs font-medium text-slate-700 dark:text-slate-300 mb-1">
                      {t('companies.postalCode')}
                    </label>
                    <input
                      type="text"
                      value={postalCode}
                      onChange={(e) => handleCepChange(e.target.value)}
                      placeholder="00000-000"
                      className="w-full bg-white dark:bg-slate-950 border border-slate-300 dark:border-slate-800 rounded-lg px-3 py-2 text-sm text-slate-900 dark:text-slate-100 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary"
                    />
                  </div>

                  <div className="md:col-span-2">
                    <label className="block text-xs font-medium text-slate-700 dark:text-slate-300 mb-1">
                      {t('companies.street')}
                    </label>
                    <input
                      type="text"
                      value={street}
                      onChange={(e) => setStreet(e.target.value)}
                      className="w-full bg-white dark:bg-slate-950 border border-slate-300 dark:border-slate-800 rounded-lg px-3 py-2 text-sm text-slate-900 dark:text-slate-100 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary"
                    />
                  </div>

                  <div>
                    <label className="block text-xs font-medium text-slate-700 dark:text-slate-300 mb-1">
                      {t('companies.number')}
                    </label>
                    <input
                      type="text"
                      value={number}
                      onChange={(e) => setNumber(e.target.value)}
                      className="w-full bg-white dark:bg-slate-950 border border-slate-300 dark:border-slate-800 rounded-lg px-3 py-2 text-sm text-slate-900 dark:text-slate-100 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary"
                    />
                  </div>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
                  <div>
                    <label className="block text-xs font-medium text-slate-700 dark:text-slate-300 mb-1">
                      {t('companies.complement')}
                    </label>
                    <input
                      type="text"
                      value={complement}
                      onChange={(e) => setComplement(e.target.value)}
                      placeholder="Sala, Andar..."
                      className="w-full bg-white dark:bg-slate-950 border border-slate-300 dark:border-slate-800 rounded-lg px-3 py-2 text-sm text-slate-900 dark:text-slate-100 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary"
                    />
                  </div>

                  <div>
                    <label className="block text-xs font-medium text-slate-700 dark:text-slate-300 mb-1">
                      {t('companies.neighborhood')}
                    </label>
                    <input
                      type="text"
                      value={neighborhood}
                      onChange={(e) => setNeighborhood(e.target.value)}
                      className="w-full bg-white dark:bg-slate-950 border border-slate-300 dark:border-slate-800 rounded-lg px-3 py-2 text-sm text-slate-900 dark:text-slate-100 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary"
                    />
                  </div>

                  <div>
                    <label className="block text-xs font-medium text-slate-700 dark:text-slate-300 mb-1">
                      {t('companies.city')} *
                    </label>
                    <input
                      type="text"
                      value={cityName}
                      onChange={(e) => setCityName(e.target.value)}
                      className="w-full bg-white dark:bg-slate-950 border border-slate-300 dark:border-slate-800 rounded-lg px-3 py-2 text-sm text-slate-900 dark:text-slate-100 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary"
                      required
                    />
                  </div>

                  <div>
                    <label className="block text-xs font-medium text-slate-700 dark:text-slate-300 mb-1">
                      {t('companies.state')} *
                    </label>
                    <input
                      type="text"
                      maxLength={2}
                      value={stateCode}
                      onChange={(e) => setStateCode(e.target.value.toUpperCase())}
                      placeholder="SP"
                      className="w-full bg-white dark:bg-slate-950 border border-slate-300 dark:border-slate-800 rounded-lg px-3 py-2 text-sm text-slate-900 dark:text-slate-100 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary"
                      required
                    />
                  </div>
                </div>
              </div>

              {/* Modal Footer */}
              <div className="flex items-center justify-end gap-3 pt-4 border-t border-slate-200 dark:border-slate-800">
                <button
                  type="button"
                  onClick={onClose}
                  className="px-4 py-2 border border-slate-200 dark:border-slate-800 hover:bg-slate-100 dark:hover:bg-slate-800 text-slate-700 dark:text-slate-300 rounded-lg text-sm font-medium transition"
                >
                  {t('common.cancel')}
                </button>
                <button
                  type="submit"
                  disabled={loading}
                  className="px-4 py-2 bg-primary hover:bg-primary/90 text-primary-foreground rounded-lg text-sm font-medium transition shadow-sm disabled:opacity-50"
                >
                  {loading ? t('common.loading') : t('common.save')}
                </button>
              </div>
            </form>
          )}

          {activeTab === 'fiscal' && (
            <FiscalSettingsTab
              initialSettings={branchToEdit?.fiscalSettings}
              onSave={handleSaveFiscal}
            />
          )}

          {activeTab === 'certificate' && (
            <CertificateTab
              certificate={branchToEdit?.certificate}
              onUpload={handleUploadCert}
              onDelete={handleDeleteCert}
            />
          )}

          {activeTab === 'accountant' && (
            <AccountantTab
              initialAccountant={branchToEdit?.accountant}
              onSave={handleSaveAccountant}
            />
          )}

          {activeTab === 'customFields' && (
            <div className="space-y-4">
              <CustomFieldFormSection
                targetEntity="COMPANY"
                values={customFieldValues}
                onChange={(key, val) => setCustomFieldValues((prev) => ({ ...prev, [key]: val }))}
              />
              <div className="flex justify-end pt-3">
                <button
                  type="button"
                  onClick={async () => {
                    if (!branchToEdit) return;
                    setLoading(true);
                    try {
                      await customFieldsApi.saveEntityCustomValues('COMPANY', branchToEdit.id, {
                        values: customFieldValues,
                      });
                      alert(t('companies.saveSuccess'));
                    } catch (err: any) {
                      setError(err.message);
                    } finally {
                      setLoading(false);
                    }
                  }}
                  disabled={loading}
                  className="px-4 py-2 bg-primary hover:bg-primary/90 text-primary-foreground rounded-lg text-sm font-medium transition shadow-sm disabled:opacity-50"
                >
                  {t('common.save')}
                </button>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
