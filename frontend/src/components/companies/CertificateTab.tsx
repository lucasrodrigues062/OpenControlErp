import React, { useState } from 'react';
import { CertificateMetadataDto } from '../../types/companyBranches';
import { useI18n } from '../../context/I18nContext';
import { Award, CheckCircle2, FileUp, Key, Trash2, XCircle } from 'lucide-react';

interface CertificateTabProps {
  certificate?: CertificateMetadataDto | null;
  onUpload: (file: File, password: string) => Promise<void>;
  onDelete: () => Promise<void>;
}

export const CertificateTab: React.FC<CertificateTabProps> = ({ certificate, onUpload, onDelete }) => {
  const { t, formatDate } = useI18n();
  const [file, setFile] = useState<File | null>(null);
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0]) {
      setFile(e.target.files[0]);
    }
  };

  const handleUploadSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!file) {
      setError(t('companies.certificate.selectFile'));
      return;
    }
    if (!password.trim()) {
      setError(t('companies.certificate.password'));
      return;
    }

    setLoading(true);
    setError(null);
    setSuccess(null);

    try {
      await onUpload(file, password);
      setSuccess(t('companies.certificate.uploadSuccess'));
      setFile(null);
      setPassword('');
      setTimeout(() => setSuccess(null), 4000);
    } catch (err: any) {
      setError(err?.message || 'Error uploading certificate');
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async () => {
    if (!window.confirm(t('companies.certificate.removeConfirm'))) return;

    setLoading(true);
    setError(null);
    try {
      await onDelete();
      setSuccess(t('companies.certificate.deleteSuccess'));
      setTimeout(() => setSuccess(null), 4000);
    } catch (err: any) {
      setError(err?.message || 'Error removing certificate');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="space-y-6">
      {error && (
        <div className="p-3 bg-red-50 dark:bg-red-950/40 text-red-700 dark:text-red-300 rounded-lg text-xs border border-red-200 dark:border-red-800">
          {error}
        </div>
      )}
      {success && (
        <div className="p-3 bg-emerald-50 dark:bg-emerald-950/40 text-emerald-700 dark:text-emerald-300 rounded-lg text-xs border border-emerald-200 dark:border-emerald-800">
          {success}
        </div>
      )}

      {/* Active Certificate Info */}
      {certificate ? (
        <div className="bg-slate-50 dark:bg-slate-950/50 border border-slate-200 dark:border-slate-800 rounded-xl p-4 space-y-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <Award className="text-primary" size={20} />
              <div>
                <h4 className="text-sm font-semibold text-slate-900 dark:text-white">
                  {certificate.subjectName || 'Certificado Digital'}
                </h4>
                <p className="text-xs text-slate-500 dark:text-slate-400">
                  {certificate.issuerName || 'ICP-Brasil'}
                </p>
              </div>
            </div>

            <span
              className={`inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-semibold ${
                certificate.isExpired
                  ? 'bg-red-100 text-red-800 dark:bg-red-950/80 dark:text-red-300'
                  : certificate.daysUntilExpiration <= 30
                  ? 'bg-amber-100 text-amber-800 dark:bg-amber-950/80 dark:text-amber-300'
                  : 'bg-emerald-100 text-emerald-800 dark:bg-emerald-950/80 dark:text-emerald-300'
              }`}
            >
              {certificate.isExpired ? (
                <>
                  <XCircle size={14} /> {t('companies.certificate.expired')}
                </>
              ) : (
                <>
                  <CheckCircle2 size={14} /> {t('companies.certificate.valid')}
                </>
              )}
            </span>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-3 pt-2 text-xs">
            <div>
              <span className="text-slate-500 dark:text-slate-400 block">{t('companies.certificate.cnpj')}</span>
              <span className="font-mono font-medium text-slate-900 dark:text-slate-100">
                {certificate.cnpj || '-'}
              </span>
            </div>
            <div>
              <span className="text-slate-500 dark:text-slate-400 block">{t('companies.certificate.validUntil')}</span>
              <span className="font-medium text-slate-900 dark:text-slate-100">
                {formatDate(certificate.validUntil)}
              </span>
              <span className="text-[11px] text-slate-500 dark:text-slate-400 ml-1 block">
                ({t('companies.certificate.daysRemaining', { days: certificate.daysUntilExpiration })})
              </span>
            </div>
            <div>
              <span className="text-slate-500 dark:text-slate-400 block">{t('companies.certificate.serialNumber')}</span>
              <span className="font-mono text-slate-700 dark:text-slate-300 truncate block">
                {certificate.serialNumber || '-'}
              </span>
            </div>
          </div>

          <div className="pt-2 flex justify-end">
            <button
              type="button"
              onClick={handleDelete}
              disabled={loading}
              className="inline-flex items-center gap-1.5 px-3 py-1.5 text-xs text-red-600 hover:text-red-700 hover:bg-red-50 dark:hover:bg-red-950/30 rounded-lg transition"
            >
              <Trash2 size={14} />
              {t('companies.certificate.remove')}
            </button>
          </div>
        </div>
      ) : (
        <div className="text-center py-6 border-2 border-dashed border-slate-200 dark:border-slate-800 rounded-xl text-slate-500 dark:text-slate-400 text-xs">
          {t('companies.certificate.noCertificate')}
        </div>
      )}

      {/* Upload New Certificate Form */}
      <form onSubmit={handleUploadSubmit} className="space-y-4 pt-2">
        <h4 className="text-xs font-semibold uppercase tracking-wider text-slate-500 dark:text-slate-400 flex items-center gap-1.5">
          <FileUp size={14} />
          {t('companies.certificate.upload')}
        </h4>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <label className="block text-xs font-medium text-slate-700 dark:text-slate-300 mb-1">
              {t('companies.certificate.selectFile')} (.pfx / .p12) *
            </label>
            <input
              type="file"
              accept=".pfx,.p12"
              onChange={handleFileChange}
              className="w-full text-xs text-slate-500 file:mr-4 file:py-2 file:px-4 file:rounded-lg file:border-0 file:text-xs file:font-semibold file:bg-primary file:text-primary-foreground hover:file:bg-primary/90 cursor-pointer"
              required
            />
          </div>

          <div>
            <label className="block text-xs font-medium text-slate-700 dark:text-slate-300 mb-1">
              {t('companies.certificate.password')} *
            </label>
            <div className="relative">
              <input
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="••••••••"
                className="w-full bg-white dark:bg-slate-950 border border-slate-300 dark:border-slate-800 rounded-lg pl-8 pr-3 py-2 text-sm text-slate-900 dark:text-slate-100 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary"
                required
              />
              <Key size={14} className="absolute left-2.5 top-3 text-slate-400" />
            </div>
          </div>
        </div>

        <div className="flex justify-end pt-2">
          <button
            type="submit"
            disabled={loading || !file || !password}
            className="inline-flex items-center gap-1.5 px-4 py-2 bg-primary hover:bg-primary/90 text-primary-foreground rounded-lg text-sm font-medium transition shadow-sm disabled:opacity-50"
          >
            <FileUp size={16} />
            {loading ? t('common.loading') : t('companies.certificate.upload')}
          </button>
        </div>
      </form>
    </div>
  );
};
