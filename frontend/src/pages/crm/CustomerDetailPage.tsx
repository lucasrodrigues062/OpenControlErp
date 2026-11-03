import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Customer } from '../../types/crm';
import { crmApi } from '../../services/crmApi';
import { customFieldsApi } from '../../api/customFieldsApi';
import { CustomFieldDefinition } from '../../types/customFields';
import { CustomerFormModal } from '../../components/crm/CustomerFormModal';
import { ActivityTimeline } from '../../components/crm/ActivityTimeline';
import {
  ArrowLeft,
  Building2,
  User,
  Edit,
  MapPin,
  CreditCard,
  Phone,
  Mail,
  Sparkles,
  Layers,
  AlertCircle,
  Clock,
} from 'lucide-react';

export const CustomerDetailPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const [customer, setCustomer] = useState<Customer | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const [activeTab, setActiveTab] = useState<'overview' | 'addresses' | 'customFields' | 'activities'>('overview');

  // Custom Fields
  const [customFields, setCustomFields] = useState<CustomFieldDefinition[]>([]);
  const [customFieldValues, setCustomFieldValues] = useState<Record<string, any>>({});

  const fetchCustomer = async () => {
    if (!id) return;
    try {
      setLoading(true);
      setError(null);
      const data = await crmApi.getCustomer(id);
      setCustomer(data);

      // Load custom field values
      try {
        const [defs, resp] = await Promise.all([
          customFieldsApi.listDefinitions('CUSTOMER', false),
          customFieldsApi.getEntityCustomValues('CUSTOMER', id),
        ]);
        setCustomFields(defs);
        setCustomFieldValues(resp.values || {});
      } catch (cfErr) {
        console.warn('Could not load custom fields:', cfErr);
      }
    } catch (err: any) {
      setError(err?.response?.data?.message || 'Cliente não encontrado.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchCustomer();
  }, [id]);

  if (loading) {
    return (
      <div className="flex h-96 items-center justify-center">
        <div className="h-8 w-8 animate-spin rounded-full border-4 border-indigo-600 border-t-transparent"></div>
      </div>
    );
  }

  if (error || !customer) {
    return (
      <div className="p-6">
        <div className="rounded-xl border border-red-200 bg-red-50 p-6 text-center text-red-700 dark:border-red-800 dark:bg-red-900/30 dark:text-red-300">
          <AlertCircle className="mx-auto h-12 w-12 text-red-500 mb-2" />
          <h2 className="text-lg font-bold">Erro ao carregar cliente</h2>
          <p className="mt-1 text-sm">{error || 'Cliente não localizado.'}</p>
          <button
            onClick={() => navigate('/crm/customers')}
            className="mt-4 inline-flex items-center gap-1.5 rounded-lg bg-red-600 px-4 py-2 text-xs font-semibold text-white hover:bg-red-700"
          >
            <ArrowLeft className="h-4 w-4" /> Voltar para lista de clientes
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="p-6 space-y-6">
      {/* Top Breadcrumb & Actions */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div className="flex items-center gap-3">
          <button
            onClick={() => navigate('/crm/customers')}
            className="rounded-lg border border-gray-300 p-2 text-gray-600 hover:bg-gray-100 dark:border-gray-600 dark:text-gray-300 dark:hover:bg-gray-700"
            title="Voltar"
          >
            <ArrowLeft className="h-5 w-5" />
          </button>
          <div>
            <div className="flex items-center gap-2">
              <h1 className="text-2xl font-bold text-gray-900 dark:text-white">
                {customer.legalName}
              </h1>
              <span
                className={`rounded-full px-2.5 py-0.5 text-xs font-semibold ${
                  customer.isActive
                    ? 'bg-emerald-100 text-emerald-800 dark:bg-emerald-900/40 dark:text-emerald-300'
                    : 'bg-rose-100 text-rose-800 dark:bg-rose-900/40 dark:text-rose-300'
                }`}
              >
                {customer.isActive ? 'Ativo' : 'Inativo'}
              </span>
              <span className="rounded-full bg-indigo-50 px-2.5 py-0.5 text-xs font-semibold text-indigo-700 dark:bg-indigo-900/40 dark:text-indigo-300">
                {customer.type === 'LEGAL_ENTITY' ? 'Pessoa Jurídica' : 'Pessoa Física'}
              </span>
            </div>
            {customer.tradeName && (
              <p className="text-sm text-gray-500 dark:text-gray-400">
                Nome Fantasia: {customer.tradeName}
              </p>
            )}
          </div>
        </div>

        <button
          onClick={() => setIsEditModalOpen(true)}
          className="inline-flex items-center gap-2 rounded-lg bg-indigo-600 px-4 py-2 text-sm font-medium text-white hover:bg-indigo-700"
        >
          <Edit className="h-4 w-4" /> Editar Cliente
        </button>
      </div>

      {/* KPI Cards */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        <div className="rounded-xl border border-gray-200 bg-white p-4 shadow-sm dark:border-gray-700 dark:bg-gray-800">
          <div className="text-xs font-medium text-gray-500 dark:text-gray-400">Documento Oficial</div>
          <div className="mt-1 text-lg font-bold text-gray-900 dark:text-white font-mono">
            {customer.formattedDocument || customer.document}
          </div>
          <div className="text-xs text-gray-400 mt-1">
            {customer.type === 'LEGAL_ENTITY' ? 'CNPJ' : 'CPF'}
          </div>
        </div>

        <div className="rounded-xl border border-gray-200 bg-white p-4 shadow-sm dark:border-gray-700 dark:bg-gray-800">
          <div className="text-xs font-medium text-gray-500 dark:text-gray-400">Limite de Crédito</div>
          <div className="mt-1 text-lg font-bold text-emerald-600 dark:text-emerald-400 font-mono">
            R$ {Number(customer.creditLimit || 0).toLocaleString('pt-BR', { minimumFractionDigits: 2 })}
          </div>
          <div className="text-xs text-gray-400 mt-1">
            Status: <span className="font-semibold">{customer.creditStatus}</span>
          </div>
        </div>

        <div className="rounded-xl border border-gray-200 bg-white p-4 shadow-sm dark:border-gray-700 dark:bg-gray-800">
          <div className="text-xs font-medium text-gray-500 dark:text-gray-400">Categoria</div>
          <div className="mt-1 flex items-center gap-2">
            {customer.categoryColorHex && (
              <span className="h-3 w-3 rounded-full" style={{ backgroundColor: customer.categoryColorHex }} />
            )}
            <span className="text-base font-semibold text-gray-900 dark:text-white">
              {customer.categoryName || 'Geral / Não atribuída'}
            </span>
          </div>
        </div>

        <div className="rounded-xl border border-gray-200 bg-white p-4 shadow-sm dark:border-gray-700 dark:bg-gray-800">
          <div className="text-xs font-medium text-gray-500 dark:text-gray-400">Grupo Econômico</div>
          <div className="mt-1 text-base font-semibold text-gray-900 dark:text-white">
            {customer.economicGroupName || 'Independente'}
          </div>
        </div>
      </div>

      {/* Tabs */}
      <div className="border-b border-gray-200 dark:border-gray-700">
        <nav className="flex gap-4">
          <button
            onClick={() => setActiveTab('overview')}
            className={`flex items-center gap-2 border-b-2 py-3 px-2 text-sm font-semibold ${
              activeTab === 'overview'
                ? 'border-indigo-600 text-indigo-600 dark:text-indigo-400'
                : 'border-transparent text-gray-500 hover:text-gray-700 dark:text-gray-400'
            }`}
          >
            <Building2 className="h-4 w-4" /> Dados Comerciais & Fiscais
          </button>
          <button
            onClick={() => setActiveTab('addresses')}
            className={`flex items-center gap-2 border-b-2 py-3 px-2 text-sm font-semibold ${
              activeTab === 'addresses'
                ? 'border-indigo-600 text-indigo-600 dark:text-indigo-400'
                : 'border-transparent text-gray-500 hover:text-gray-700 dark:text-gray-400'
            }`}
          >
            <MapPin className="h-4 w-4" /> Endereços (Principal, Cobrança, Entrega)
          </button>
          <button
            onClick={() => setActiveTab('customFields')}
            className={`flex items-center gap-2 border-b-2 py-3 px-2 text-sm font-semibold ${
              activeTab === 'customFields'
                ? 'border-indigo-600 text-indigo-600 dark:text-indigo-400'
                : 'border-transparent text-gray-500 hover:text-gray-700 dark:text-gray-400'
            }`}
          >
            <Sparkles className="h-4 w-4" /> Campos Customizados
          </button>
          <button
            onClick={() => setActiveTab('activities')}
            className={`flex items-center gap-2 border-b-2 py-3 px-2 text-sm font-semibold ${
              activeTab === 'activities'
                ? 'border-indigo-600 text-indigo-600 dark:text-indigo-400'
                : 'border-transparent text-gray-500 hover:text-gray-700 dark:text-gray-400'
            }`}
          >
            <Clock className="h-4 w-4" /> Atividades & Histórico
          </button>
        </nav>
      </div>

      {/* Tab Contents */}
      {activeTab === 'overview' && (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          {/* Statutory & Fiscal */}
          <div className="rounded-xl border border-gray-200 bg-white p-6 shadow-sm dark:border-gray-700 dark:bg-gray-800 space-y-4">
            <h3 className="text-sm font-bold uppercase tracking-wider text-gray-500 dark:text-gray-400">
              Informações Fiscais & Cadastrais
            </h3>
            <div className="grid grid-cols-2 gap-4 text-sm">
              <div>
                <span className="text-xs text-gray-500">Inscrição Estadual</span>
                <p className="font-semibold text-gray-900 dark:text-white">
                  {customer.isStateRegistrationExempt ? 'ISENTO' : customer.stateRegistration || 'Não informada'}
                </p>
              </div>
              <div>
                <span className="text-xs text-gray-500">Indicador de ICMS</span>
                <p className="font-semibold text-gray-900 dark:text-white">
                  {customer.icmsTaxpayerType === 'CONTRIBUTOR'
                    ? 'Contribuinte ICMS'
                    : customer.icmsTaxpayerType === 'EXEMPT'
                    ? 'Isento de Inscrição'
                    : 'Não Contribuinte'}
                </p>
              </div>
              <div>
                <span className="text-xs text-gray-500">Inscrição Municipal</span>
                <p className="font-semibold text-gray-900 dark:text-white">
                  {customer.municipalRegistration || 'Não informada'}
                </p>
              </div>
              <div>
                <span className="text-xs text-gray-500">CNAE Principal</span>
                <p className="font-semibold text-gray-900 dark:text-white">
                  {customer.mainCnae || 'Não informado'}
                </p>
              </div>
            </div>

            {customer.notes && (
              <div className="border-t border-gray-100 pt-3 dark:border-gray-700">
                <span className="text-xs text-gray-500">Observações Internas</span>
                <p className="mt-1 text-sm text-gray-700 dark:text-gray-300 whitespace-pre-line">
                  {customer.notes}
                </p>
              </div>
            )}
          </div>

          {/* Contact Details */}
          <div className="rounded-xl border border-gray-200 bg-white p-6 shadow-sm dark:border-gray-700 dark:bg-gray-800 space-y-4">
            <h3 className="text-sm font-bold uppercase tracking-wider text-gray-500 dark:text-gray-400">
              Contatos & Comunicação
            </h3>
            <div className="space-y-3 text-sm">
              <div className="flex items-center gap-3">
                <Phone className="h-4 w-4 text-gray-400" />
                <span className="font-medium text-gray-900 dark:text-white">
                  {customer.phone1 || 'Telefone não cadastrado'}
                </span>
              </div>
              <div className="flex items-center gap-3">
                <Mail className="h-4 w-4 text-gray-400" />
                <span className="font-medium text-gray-900 dark:text-white">
                  {customer.email || 'E-mail não cadastrado'}
                </span>
              </div>
              <div className="flex items-center gap-3">
                <User className="h-4 w-4 text-gray-400" />
                <span className="font-medium text-gray-900 dark:text-white">
                  Contato: {customer.contactPerson || 'Não informado'}
                </span>
              </div>
            </div>
          </div>
        </div>
      )}

      {activeTab === 'addresses' && (
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          {/* Primary */}
          <div className="rounded-xl border border-gray-200 bg-white p-6 shadow-sm dark:border-gray-700 dark:bg-gray-800 space-y-3">
            <div className="flex items-center gap-2 text-indigo-600 dark:text-indigo-400 font-semibold text-sm">
              <MapPin className="h-5 w-5" /> Endereço Principal (Faturamento)
            </div>
            <div className="text-sm text-gray-700 dark:text-gray-300 space-y-1">
              <p className="font-medium text-gray-900 dark:text-white">
                {customer.street ? `${customer.street}, ${customer.number || 'S/N'}` : 'Endereço não informado'}
              </p>
              {customer.complement && <p className="text-xs text-gray-500">{customer.complement}</p>}
              <p>{customer.neighborhood || ''}</p>
              <p>
                {customer.cityName ? `${customer.cityName} - ${customer.stateCode}` : ''}
              </p>
              <p className="font-mono text-xs text-gray-500">CEP: {customer.postalCode || 'Não informado'}</p>
            </div>
          </div>

          {/* Billing */}
          <div className="rounded-xl border border-gray-200 bg-white p-6 shadow-sm dark:border-gray-700 dark:bg-gray-800 space-y-3">
            <div className="flex items-center gap-2 text-purple-600 dark:text-purple-400 font-semibold text-sm">
              <CreditCard className="h-5 w-5" /> Endereço de Cobrança
            </div>
            <div className="text-sm text-gray-700 dark:text-gray-300 space-y-1">
              {customer.billingStreet ? (
                <>
                  <p className="font-medium text-gray-900 dark:text-white">
                    {customer.billingStreet}, {customer.billingNumber || 'S/N'}
                  </p>
                  {customer.billingComplement && (
                    <p className="text-xs text-gray-500">{customer.billingComplement}</p>
                  )}
                  <p>{customer.billingNeighborhood || ''}</p>
                  <p>{customer.billingCityName ? `${customer.billingCityName} - ${customer.billingStateCode}` : ''}</p>
                  <p className="font-mono text-xs text-gray-500">CEP: {customer.billingPostalCode}</p>
                </>
              ) : (
                <p className="text-xs text-gray-400 italic">Mesmo do endereço principal.</p>
              )}
            </div>
          </div>

          {/* Delivery */}
          <div className="rounded-xl border border-gray-200 bg-white p-6 shadow-sm dark:border-gray-700 dark:bg-gray-800 space-y-3">
            <div className="flex items-center gap-2 text-emerald-600 dark:text-emerald-400 font-semibold text-sm">
              <Layers className="h-5 w-5" /> Endereço de Entrega
            </div>
            <div className="text-sm text-gray-700 dark:text-gray-300 space-y-1">
              {customer.deliveryStreet ? (
                <>
                  <p className="font-medium text-gray-900 dark:text-white">
                    {customer.deliveryStreet}, {customer.deliveryNumber || 'S/N'}
                  </p>
                  {customer.deliveryComplement && (
                    <p className="text-xs text-gray-500">{customer.deliveryComplement}</p>
                  )}
                  <p>{customer.deliveryNeighborhood || ''}</p>
                  <p>{customer.deliveryCityName ? `${customer.deliveryCityName} - ${customer.deliveryStateCode}` : ''}</p>
                  <p className="font-mono text-xs text-gray-500">CEP: {customer.deliveryPostalCode}</p>
                </>
              ) : (
                <p className="text-xs text-gray-400 italic">Mesmo do endereço principal.</p>
              )}
            </div>
          </div>
        </div>
      )}

      {activeTab === 'customFields' && (
        <div className="rounded-xl border border-gray-200 bg-white p-6 shadow-sm dark:border-gray-700 dark:bg-gray-800 space-y-4">
          <h3 className="text-sm font-bold uppercase tracking-wider text-gray-500 dark:text-gray-400">
            Campos Customizados ({customFields.length})
          </h3>
          {customFields.length === 0 ? (
            <p className="text-xs text-gray-400 italic">
              Nenhum campo customizado configurado para a entidade Cliente. Defina campos adicionais em Configurações &gt; Campos Customizados.
            </p>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              {customFields.map((field) => (
                <div key={field.id} className="rounded-lg border border-gray-100 p-3 dark:border-gray-700 bg-gray-50 dark:bg-gray-750">
                  <span className="text-xs text-gray-500 block">{field.label}</span>
                  <p className="font-semibold text-sm text-gray-900 dark:text-white mt-1">
                    {customFieldValues[field.fieldKey] !== undefined && customFieldValues[field.fieldKey] !== null
                      ? String(customFieldValues[field.fieldKey])
                      : <span className="text-xs text-gray-400 italic">Não preenchido</span>}
                  </p>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {activeTab === 'activities' && (
        <div className="rounded-xl border border-gray-200 bg-white p-6 shadow-sm dark:border-gray-700 dark:bg-gray-800">
          <ActivityTimeline entityType="CUSTOMER" entityId={customer.id} />
        </div>
      )}

      {/* Edit Customer Modal */}
      <CustomerFormModal
        isOpen={isEditModalOpen}
        onClose={() => setIsEditModalOpen(false)}
        customerToEdit={customer}
        onSaved={fetchCustomer}
      />
    </div>
  );
};
