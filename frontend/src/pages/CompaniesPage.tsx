import React, { useState, useEffect } from 'react';
import {
  CompanyBranchDetail,
  CompanyBranchSummary,
  CreateCompanyBranchRequest,
  EstablishmentType,
  UpdateCompanyBranchRequest,
} from '../types/companyBranches';
import { companyBranchesApi } from '../api/companyBranchesApi';
import { useI18n } from '../context/I18nContext';
import { CompanyBranchFormModal } from '../components/companies/CompanyBranchFormModal';
import {
  Award,
  Building2,
  CheckCircle2,
  CornerDownRight,
  Edit2,
  Plus,
  Power,
  Search,
  XCircle,
} from 'lucide-react';

export const CompaniesPage: React.FC = () => {
  const { t, formatDate } = useI18n();
  const [companies, setCompanies] = useState<CompanyBranchSummary[]>([]);
  const [loading, setLoading] = useState(false);
  const [searchTerm, setSearchTerm] = useState('');
  const [typeFilter, setTypeFilter] = useState<'ALL' | EstablishmentType>('ALL');

  const [modalOpen, setModalOpen] = useState(false);
  const [editingBranch, setEditingBranch] = useState<CompanyBranchDetail | null>(null);
  const [modalDefaultType, setModalDefaultType] = useState<EstablishmentType>('HEADQUARTERS');

  const [notification, setNotification] = useState<{ type: 'success' | 'error'; message: string } | null>(null);

  const showNotification = (type: 'success' | 'error', message: string) => {
    setNotification({ type, message });
    setTimeout(() => setNotification(null), 4000);
  };

  const loadCompanies = async () => {
    setLoading(true);
    try {
      const data = await companyBranchesApi.list();
      setCompanies(data);
    } catch (err: any) {
      showNotification('error', err.message || 'Error loading companies');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadCompanies();
  }, []);

  const handleOpenCreateHq = () => {
    setEditingBranch(null);
    setModalDefaultType('HEADQUARTERS');
    setModalOpen(true);
  };

  const handleOpenCreateBranch = () => {
    setEditingBranch(null);
    setModalDefaultType('BRANCH');
    setModalOpen(true);
  };

  const handleOpenEdit = async (id: string) => {
    try {
      const detail = await companyBranchesApi.getById(id);
      setEditingBranch(detail);
      setModalOpen(true);
    } catch (err: any) {
      showNotification('error', err.message || 'Error loading company details');
    }
  };

  const handleSave = async (payload: CreateCompanyBranchRequest | UpdateCompanyBranchRequest) => {
    if (editingBranch) {
      await companyBranchesApi.update(editingBranch.id, payload as UpdateCompanyBranchRequest);
      showNotification('success', t('companies.saveSuccess'));
    } else {
      await companyBranchesApi.create(payload as CreateCompanyBranchRequest);
      showNotification('success', t('companies.saveSuccess'));
    }
    await loadCompanies();
  };

  const handleToggleStatus = async (id: string) => {
    try {
      await companyBranchesApi.toggleStatus(id);
      showNotification('success', t('companies.toggleSuccess'));
      await loadCompanies();
    } catch (err: any) {
      showNotification('error', err.message || 'Error toggling status');
    }
  };

  const handleRefreshDetail = async (id: string): Promise<CompanyBranchDetail> => {
    const detail = await companyBranchesApi.getById(id);
    setEditingBranch(detail);
    await loadCompanies();
    return detail;
  };

  // Filter list
  const filtered = companies.filter((c) => {
    const matchesType = typeFilter === 'ALL' || c.type === typeFilter;
    const matchesSearch =
      searchTerm === '' ||
      c.tradeName.toLowerCase().includes(searchTerm.toLowerCase()) ||
      c.legalName.toLowerCase().includes(searchTerm.toLowerCase()) ||
      c.cnpj.includes(searchTerm) ||
      c.code.toLowerCase().includes(searchTerm.toLowerCase());
    return matchesType && matchesSearch;
  });

  const isBranchActive = (branch: { isActive?: boolean; active?: boolean }) => branch.isActive ?? branch.active ?? true;

  const headquarters = companies.filter((c) => c.type === 'HEADQUARTERS');

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-xl font-bold tracking-tight text-slate-900 dark:text-white flex items-center gap-2">
            <Building2 className="text-primary" size={24} />
            {t('companies.title')}
          </h1>
          <p className="text-xs text-slate-500 dark:text-slate-400 mt-1">
            {t('companies.description')}
          </p>
        </div>

        <div className="flex items-center gap-2">
          <button
            onClick={handleOpenCreateHq}
            className="inline-flex items-center gap-1.5 px-3.5 py-2 bg-primary hover:bg-primary/90 text-primary-foreground rounded-lg text-xs font-semibold transition shadow-sm"
          >
            <Plus size={15} />
            {t('companies.newHeadquarters')}
          </button>
          <button
            onClick={handleOpenCreateBranch}
            disabled={headquarters.length === 0}
            className="inline-flex items-center gap-1.5 px-3.5 py-2 border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-900 hover:bg-slate-50 dark:hover:bg-slate-800 text-slate-800 dark:text-slate-200 rounded-lg text-xs font-semibold transition shadow-sm disabled:opacity-50"
          >
            <Plus size={15} />
            {t('companies.newBranch')}
          </button>
        </div>
      </div>

      {/* Notifications */}
      {notification && (
        <div
          className={`p-3.5 rounded-xl text-xs font-medium border flex items-center justify-between transition ${
            notification.type === 'success'
              ? 'bg-emerald-50 dark:bg-emerald-950/40 text-emerald-800 dark:text-emerald-300 border-emerald-200 dark:border-emerald-800'
              : 'bg-red-50 dark:bg-red-950/40 text-red-800 dark:text-red-300 border-red-200 dark:border-red-800'
          }`}
        >
          <span>{notification.message}</span>
        </div>
      )}

      {/* Search & Filters */}
      <div className="flex flex-col sm:flex-row items-center gap-3">
        <div className="relative flex-1 w-full">
          <Search size={16} className="absolute left-3 top-2.5 text-slate-400" />
          <input
            type="text"
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            placeholder={t('common.search')}
            className="w-full pl-9 pr-3 py-2 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-lg text-xs text-slate-900 dark:text-slate-100 placeholder:text-slate-400 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary shadow-sm"
          />
        </div>

        <div className="flex items-center gap-1 self-stretch sm:self-auto bg-slate-100 dark:bg-slate-800/60 p-1 rounded-lg border border-slate-200 dark:border-slate-700/60">
          <button
            onClick={() => setTypeFilter('ALL')}
            className={`px-3 py-1 text-xs rounded-md font-medium transition ${
              typeFilter === 'ALL'
                ? 'bg-white dark:bg-slate-900 text-slate-900 dark:text-white shadow-xs'
                : 'text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white'
            }`}
          >
            {t('common.all')}
          </button>
          <button
            onClick={() => setTypeFilter('HEADQUARTERS')}
            className={`px-3 py-1 text-xs rounded-md font-medium transition ${
              typeFilter === 'HEADQUARTERS'
                ? 'bg-white dark:bg-slate-900 text-slate-900 dark:text-white shadow-xs'
                : 'text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white'
            }`}
          >
            Matrizes
          </button>
          <button
            onClick={() => setTypeFilter('BRANCH')}
            className={`px-3 py-1 text-xs rounded-md font-medium transition ${
              typeFilter === 'BRANCH'
                ? 'bg-white dark:bg-slate-900 text-slate-900 dark:text-white shadow-xs'
                : 'text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white'
            }`}
          >
            Filiais
          </button>
        </div>
      </div>

      {/* List Table / Card View */}
      <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl overflow-hidden shadow-sm">
        {loading ? (
          <div className="p-12 text-center text-slate-500 text-xs">{t('common.loading')}</div>
        ) : filtered.length === 0 ? (
          <div className="p-12 text-center text-slate-500 text-xs space-y-3">
            <Building2 size={36} className="mx-auto text-slate-400 dark:text-slate-600" />
            <p>{t('companies.noCompanies')}</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="border-b border-slate-200 dark:border-slate-800 bg-slate-50/75 dark:bg-slate-950/60 text-xs font-semibold text-slate-500 dark:text-slate-400 uppercase tracking-wider">
                  <th className="py-3 px-4 w-16 text-center">Cód</th>
                  <th className="py-3 px-4">Estabelecimento</th>
                  <th className="py-3 px-4">CNPJ</th>
                  <th className="py-3 px-4">Cidade / UF</th>
                  <th className="py-3 px-4">Certificado A1</th>
                  <th className="py-3 px-4">{t('common.status')}</th>
                  <th className="py-3 px-4 text-right">{t('common.actions')}</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 dark:divide-slate-800 text-sm">
                {filtered.map((item) => (
                  <tr
                    key={item.id}
                    className={`hover:bg-slate-50/80 dark:hover:bg-slate-800/40 transition ${
                      !isBranchActive(item) ? 'opacity-60 bg-slate-50/40 dark:bg-slate-950/20' : ''
                    }`}
                  >
                    <td className="py-3.5 px-4 text-center font-mono text-xs font-semibold text-slate-500 dark:text-slate-400">
                      #{item.code}
                    </td>

                    <td className="py-3.5 px-4">
                      <div className="flex items-center gap-2">
                        {item.type === 'BRANCH' && (
                          <CornerDownRight size={14} className="text-slate-400 shrink-0 ml-1" />
                        )}
                        <div className="w-9 h-9 rounded-lg border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 flex items-center justify-center overflow-hidden shrink-0 shadow-xs">
                          {item.logoUrl ? (
                            <img src={item.logoUrl} alt={item.tradeName} className="w-full h-full object-contain p-0.5" />
                          ) : (
                            <Building2 className={"w-5 h-5 " + (item.type === 'HEADQUARTERS' ? 'text-primary' : 'text-purple-500')} />
                          )}
                        </div>
                        <div>
                          <div className="font-semibold text-slate-900 dark:text-white flex items-center gap-1.5">
                            {item.tradeName}
                            <span
                              className={`text-[10px] uppercase font-semibold px-1.5 py-0.5 rounded ${
                                item.type === 'HEADQUARTERS'
                                  ? 'bg-blue-100 text-blue-800 dark:bg-blue-950/60 dark:text-blue-300'
                                  : 'bg-purple-100 text-purple-800 dark:bg-purple-950/60 dark:text-purple-300'
                              }`}
                            >
                              {item.type === 'HEADQUARTERS' ? 'Matriz' : 'Filial'}
                            </span>
                          </div>
                          <div className="text-xs text-slate-500 dark:text-slate-400 truncate max-w-sm">
                            {item.legalName}
                          </div>
                        </div>
                      </div>
                    </td>

                    <td className="py-3.5 px-4">
                      <code className="text-xs font-mono text-slate-700 dark:text-slate-300 bg-slate-100 dark:bg-slate-800/60 px-1.5 py-0.5 rounded border border-slate-200 dark:border-slate-700/60">
                        {item.cnpj}
                      </code>
                    </td>

                    <td className="py-3.5 px-4 text-xs text-slate-600 dark:text-slate-400">
                      {item.cityName} / <span className="font-semibold">{item.stateCode}</span>
                    </td>

                    <td className="py-3.5 px-4">
                      {item.certificateValidUntil ? (
                        <span
                          className={`inline-flex items-center gap-1 text-[11px] font-medium px-2 py-0.5 rounded-full ${
                            item.isCertificateExpired
                              ? 'bg-red-50 text-red-700 border border-red-200 dark:bg-red-950/40 dark:text-red-300 dark:border-red-800'
                              : 'bg-emerald-50 text-emerald-700 border border-emerald-200 dark:bg-emerald-950/40 dark:text-emerald-300 dark:border-emerald-800'
                          }`}
                        >
                          <Award size={12} />
                          {item.isCertificateExpired ? 'Expirado' : `Até ${formatDate(item.certificateValidUntil)}`}
                        </span>
                      ) : (
                        <span className="text-[11px] text-slate-400">-</span>
                      )}
                    </td>

                    <td className="py-3.5 px-4">
                      <span
                        className={`inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[11px] font-medium border ${
                          isBranchActive(item)
                            ? 'bg-emerald-50 text-emerald-700 border-emerald-200 dark:bg-emerald-950/40 dark:text-emerald-300 dark:border-emerald-800/50'
                            : 'bg-slate-100 text-slate-600 border-slate-200 dark:bg-slate-800 dark:text-slate-400 dark:border-slate-700'
                        }`}
                      >
                        {isBranchActive(item) ? (
                          <>
                            <CheckCircle2 size={11} /> {t('common.active')}
                          </>
                        ) : (
                          <>
                            <XCircle size={11} /> {t('common.inactive')}
                          </>
                        )}
                      </span>
                    </td>

                    <td className="py-3.5 px-4 text-right">
                      <div className="flex items-center justify-end gap-1">
                        <button
                          onClick={() => handleOpenEdit(item.id)}
                          className="p-1.5 text-slate-500 hover:text-primary hover:bg-slate-100 dark:hover:bg-slate-800 rounded-lg transition"
                          title={t('common.edit')}
                        >
                          <Edit2 size={14} />
                        </button>
                        <button
                          onClick={() => handleToggleStatus(item.id)}
                          className={`p-1.5 rounded-lg transition ${
                            item.isActive
                              ? 'text-slate-500 hover:text-amber-600 hover:bg-amber-50 dark:hover:bg-amber-950/30'
                              : 'text-slate-400 hover:text-emerald-600 hover:bg-emerald-50 dark:hover:bg-emerald-950/30'
                          }`}
                          title={t('common.status')}
                        >
                          <Power size={14} />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Modal for Create/Edit */}
      <CompanyBranchFormModal
        isOpen={modalOpen}
        onClose={() => setModalOpen(false)}
        branchToEdit={editingBranch}
        defaultType={modalDefaultType}
        headquartersList={headquarters}
        onSave={handleSave}
        onRefreshDetail={handleRefreshDetail}
      />
    </div>
  );
};
