import React, { useState } from 'react';
import { inboundApi } from '../../services/inboundApi';
import { X, UploadCloud, FileCode2, CheckCircle2 } from 'lucide-react';

interface NFeXmlUploadModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSuccess: () => void;
  branchId: string;
}

export const NFeXmlUploadModal: React.FC<NFeXmlUploadModalProps> = ({
  isOpen,
  onClose,
  onSuccess,
  branchId,
}) => {
  const [file, setFile] = useState<File | null>(null);
  const [loading, setLoading] = useState(false);
  const [errorMsg, setErrorMsg] = useState('');

  if (!isOpen) return null;

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files.length > 0) {
      const selected = e.target.files[0];
      if (!selected.name.toLowerCase().endsWith('.xml')) {
        setErrorMsg('Por favor, selecione um arquivo XML de NF-e válido.');
        setFile(null);
        return;
      }
      setFile(selected);
      setErrorMsg('');
    }
  };

  const handleUpload = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!file) {
      setErrorMsg('Selecione um arquivo XML da SEFAZ para continuar.');
      return;
    }

    setLoading(true);
    setErrorMsg('');
    try {
      const formData = new FormData();
      formData.append('file', file);
      formData.append('targetBranchId', branchId);

      await inboundApi.uploadNFeXml(formData);
      onSuccess();
      onClose();
    } catch (err: any) {
      setErrorMsg(err.response?.data?.detail || err.message || 'Erro ao importar XML da NF-e');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4 backdrop-blur-xs">
      <div className="w-full max-w-lg rounded-2xl bg-white shadow-2xl dark:bg-gray-800 border border-gray-100 dark:border-gray-700">
        <div className="flex items-center justify-between border-b border-gray-100 p-5 dark:border-gray-700">
          <div className="flex items-center gap-2.5">
            <div className="rounded-xl bg-emerald-50 p-2 text-emerald-600 dark:bg-emerald-900/30 dark:text-emerald-400">
              <UploadCloud className="h-5 w-5" />
            </div>
            <div>
              <h2 className="text-lg font-bold text-gray-900 dark:text-white">Importar NF-e (XML SEFAZ)</h2>
              <p className="text-xs text-gray-500">Layout 4.00 com extração automática de itens, frete e tributos</p>
            </div>
          </div>
          <button onClick={onClose} className="rounded-lg p-1.5 text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-700">
            <X className="h-5 w-5" />
          </button>
        </div>

        {errorMsg && (
          <div className="mx-6 mt-4 rounded-xl bg-red-50 p-3 text-xs text-red-700 border border-red-200 dark:bg-red-900/20 dark:text-red-300">
            {errorMsg}
          </div>
        )}

        <form onSubmit={handleUpload} className="p-6 space-y-4">
          <div className="border-2 border-dashed border-gray-300 dark:border-gray-600 rounded-2xl p-8 text-center hover:border-emerald-500 transition cursor-pointer">
            <input
              type="file"
              accept=".xml"
              onChange={handleFileChange}
              id="xmlFileInput"
              className="hidden"
            />
            <label htmlFor="xmlFileInput" className="cursor-pointer flex flex-col items-center">
              {file ? (
                <>
                  <FileCode2 className="h-10 w-10 text-emerald-600 mb-2" />
                  <span className="font-semibold text-sm text-gray-800 dark:text-white">{file.name}</span>
                  <span className="text-xs text-gray-400 mt-1">{(file.size / 1024).toFixed(1)} KB</span>
                  <span className="mt-3 text-xs font-semibold text-emerald-600 hover:underline">Clique para trocar</span>
                </>
              ) : (
                <>
                  <UploadCloud className="h-10 w-10 text-gray-400 mb-2" />
                  <span className="font-semibold text-sm text-gray-800 dark:text-white">
                    Clique para selecionar ou arraste o arquivo XML
                  </span>
                  <span className="text-xs text-gray-500 mt-1">Formatos suportados: NF-e SEFAZ (.xml)</span>
                </>
              )}
            </label>
          </div>

          <div className="rounded-xl bg-emerald-50/60 p-3.5 border border-emerald-100 dark:bg-emerald-950/20 dark:border-emerald-900/40 text-xs text-emerald-900 dark:text-emerald-300 space-y-1">
            <div className="font-bold flex items-center gap-1.5">
              <CheckCircle2 className="h-4 w-4 text-emerald-600" /> O que o sistema faz automaticamente:
            </div>
            <ul className="list-disc list-inside pl-1 text-[11px] text-gray-600 dark:text-gray-400 space-y-0.5">
              <li>Reconhece o fornecedor pelo CNPJ/CPF da nota</li>
              <li>Faz de-para inteligente de itens já conhecidos</li>
              <li>Aplica fator de conversão de unidades (ex: caixas $\rightarrow$ unidades)</li>
              <li>Calcula frete, impostos e custos landed em tempo real</li>
            </ul>
          </div>

          <div className="flex justify-end gap-3 pt-3 border-t border-gray-100 dark:border-gray-700">
            <button
              type="button"
              onClick={onClose}
              disabled={loading}
              className="rounded-lg border border-gray-300 px-4 py-2 text-xs font-medium text-gray-700 hover:bg-gray-50 dark:border-gray-600 dark:text-gray-300"
            >
              Cancelar
            </button>
            <button
              type="submit"
              disabled={loading || !file}
              className="rounded-lg bg-emerald-600 px-5 py-2 text-xs font-semibold text-white hover:bg-emerald-700 disabled:opacity-50 transition"
            >
              {loading ? 'Processando XML...' : 'Importar NF-e'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
