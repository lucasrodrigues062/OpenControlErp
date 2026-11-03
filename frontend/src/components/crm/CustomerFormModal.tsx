import React, { useState, useEffect } from 'react';
import {
  Customer,
  CreateCustomerRequest,
  UpdateCustomerRequest,
  CustomerType,
  IcmsTaxpayerType,
  CreditStatus,
  CustomerCategory,
  EconomicGroup,
} from '../../types/crm';
import { crmApi } from '../../services/crmApi';
import { customFieldsApi } from '../../api/customFieldsApi';
import { CustomFieldFormSection } from '../customFields/CustomFieldFormSection';
import { X, User, Building2, MapPin, Layers, Sparkles } from 'lucide-react';

interface CustomerFormModalProps {
  isOpen: boolean;
  onClose: () => void;
  customerToEdit?: Customer | null;
  onSaved: () => void;
}

export const CustomerFormModal: React.FC<CustomerFormModalProps> = ({
  isOpen,
  onClose,
  customerToEdit,
  onSaved,
}) => {
  const [activeTab, setActiveTab] = useState<'main' | 'address' | 'additionalAddress' | 'customFields'>('main');

  const [type, setType] = useState<CustomerType>('LEGAL_ENTITY');
  const [document, setDocument] = useState('');
  const [legalName, setLegalName] = useState('');
  const [tradeName, setTradeName] = useState('');
  const [stateRegistration, setStateRegistration] = useState('');
  const [isStateRegistrationExempt, setIsStateRegistrationExempt] = useState(false);
  const [municipalRegistration, setMunicipalRegistration] = useState('');
  const [icmsTaxpayerType, setIcmsTaxpayerType] = useState<IcmsTaxpayerType>('NON_CONTRIBUTOR');
  const [phone1, setPhone1] = useState('');
  const [email, setEmail] = useState('');
  const [contactPerson, setContactPerson] = useState('');
  const [website, setWebsite] = useState('');
  const [categoryId, setCategoryId] = useState('');
  const [economicGroupId, setEconomicGroupId] = useState('');
  const [creditLimit, setCreditLimit] = useState('0');
  const [creditStatus, setCreditStatus] = useState<CreditStatus>('APPROVED');
  const [notes, setNotes] = useState('');

  // Primary Address
  const [postalCode, setPostalCode] = useState('');
  const [street, setStreet] = useState('');
  const [number, setNumber] = useState('');
  const [complement, setComplement] = useState('');
  const [neighborhood, setNeighborhood] = useState('');
  const [cityName, setCityName] = useState('');
  const [cityIbgeCode, setCityIbgeCode] = useState<number | undefined>(undefined);
  const [stateCode, setStateCode] = useState('');

  // Billing Address
  const [billingPostalCode, setBillingPostalCode] = useState('');
  const [billingStreet, setBillingStreet] = useState('');
  const [billingNumber, setBillingNumber] = useState('');
  const [billingComplement, setBillingComplement] = useState('');
  const [billingNeighborhood, setBillingNeighborhood] = useState('');
  const [billingCityName, setBillingCityName] = useState('');
  const [billingStateCode, setBillingStateCode] = useState('');

  // Delivery Address
  const [deliveryPostalCode, setDeliveryPostalCode] = useState('');
  const [deliveryStreet, setDeliveryStreet] = useState('');
  const [deliveryNumber, setDeliveryNumber] = useState('');
  const [deliveryComplement, setDeliveryComplement] = useState('');
  const [deliveryNeighborhood, setDeliveryNeighborhood] = useState('');
  const [deliveryCityName, setDeliveryCityName] = useState('');
  const [deliveryStateCode, setDeliveryStateCode] = useState('');

  // Custom Fields
  const [customFieldValues, setCustomFieldValues] = useState<Record<string, any>>({});

  // Options
  const [categories, setCategories] = useState<CustomerCategory[]>([]);
  const [economicGroups, setEconomicGroups] = useState<EconomicGroup[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (isOpen) {
      loadDependencies();
      if (customerToEdit) {
        populateForm(customerToEdit);
      } else {
        resetForm();
      }
    }
  }, [isOpen, customerToEdit]);

  const loadDependencies = async () => {
    try {
      const [cats, grps] = await Promise.all([
        crmApi.listCustomerCategories(),
        crmApi.listEconomicGroups(),
      ]);
      setCategories(cats);
      setEconomicGroups(grps);
    } catch (e) {
      console.error('Error loading categories/groups:', e);
    }
  };

  const populateForm = async (cust: Customer) => {
    setType(cust.type);
    setDocument(cust.formattedDocument || cust.document);
    setLegalName(cust.legalName);
    setTradeName(cust.tradeName || '');
    setStateRegistration(cust.stateRegistration || '');
    setIsStateRegistrationExempt(cust.isStateRegistrationExempt);
    setMunicipalRegistration(cust.municipalRegistration || '');
    setIcmsTaxpayerType(cust.icmsTaxpayerType);
    setPhone1(cust.phone1 || '');
    setEmail(cust.email || '');
    setContactPerson(cust.contactPerson || '');
    setWebsite(cust.website || '');
    setCategoryId(cust.categoryId || '');
    setEconomicGroupId(cust.economicGroupId || '');
    setCreditLimit(cust.creditLimit?.toString() || '0');
    setCreditStatus(cust.creditStatus);
    setNotes(cust.notes || '');

    setPostalCode(cust.postalCode || '');
    setStreet(cust.street || '');
    setNumber(cust.number || '');
    setComplement(cust.complement || '');
    setNeighborhood(cust.neighborhood || '');
    setCityName(cust.cityName || '');
    setCityIbgeCode(cust.cityIbgeCode || undefined);
    setStateCode(cust.stateCode || '');

    setBillingPostalCode(cust.billingPostalCode || '');
    setBillingStreet(cust.billingStreet || '');
    setBillingNumber(cust.billingNumber || '');
    setBillingComplement(cust.billingComplement || '');
    setBillingNeighborhood(cust.billingNeighborhood || '');
    setBillingCityName(cust.billingCityName || '');
    setBillingStateCode(cust.billingStateCode || '');

    setDeliveryPostalCode(cust.deliveryPostalCode || '');
    setDeliveryStreet(cust.deliveryStreet || '');
    setDeliveryNumber(cust.deliveryNumber || '');
    setDeliveryComplement(cust.deliveryComplement || '');
    setDeliveryNeighborhood(cust.deliveryNeighborhood || '');
    setDeliveryCityName(cust.deliveryCityName || '');
    setDeliveryStateCode(cust.deliveryStateCode || '');

    // Load custom field values
    try {
      const resp = await customFieldsApi.getEntityCustomValues('CUSTOMER', cust.id);
      setCustomFieldValues(resp.values || {});
    } catch {
      setCustomFieldValues({});
    }
  };

  const resetForm = () => {
    setType('LEGAL_ENTITY');
    setDocument('');
    setLegalName('');
    setTradeName('');
    setStateRegistration('');
    setIsStateRegistrationExempt(false);
    setMunicipalRegistration('');
    setIcmsTaxpayerType('NON_CONTRIBUTOR');
    setPhone1('');
    setEmail('');
    setContactPerson('');
    setWebsite('');
    setCategoryId('');
    setEconomicGroupId('');
    setCreditLimit('0');
    setCreditStatus('APPROVED');
    setNotes('');

    setPostalCode('');
    setStreet('');
    setNumber('');
    setComplement('');
    setNeighborhood('');
    setCityName('');
    setCityIbgeCode(undefined);
    setStateCode('');

    setBillingPostalCode('');
    setBillingStreet('');
    setBillingNumber('');
    setBillingComplement('');
    setBillingNeighborhood('');
    setBillingCityName('');
    setBillingStateCode('');

    setDeliveryPostalCode('');
    setDeliveryStreet('');
    setDeliveryNumber('');
    setDeliveryComplement('');
    setDeliveryNeighborhood('');
    setDeliveryCityName('');
    setDeliveryStateCode('');

    setCustomFieldValues({});
    setError(null);
    setActiveTab('main');
  };

  // ViaCEP integration
  const handleCepLookup = async (cepValue: string) => {
    const cleanCep = cepValue.replace(/\D/g, '');
    setPostalCode(cepValue);
    if (cleanCep.length === 8) {
      try {
        const resp = await fetch(`https://viacep.com.br/ws/${cleanCep}/json/`);
        const data = await resp.json();
        if (!data.erro) {
          setStreet(data.logradouro || '');
          setNeighborhood(data.bairro || '');
          setCityName(data.localidade || '');
          setStateCode(data.uf || '');
          if (data.ibge) setCityIbgeCode(parseInt(data.ibge, 10));
        }
      } catch (err) {
        console.error('ViaCEP lookup failed:', err);
      }
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!document.trim() || !legalName.trim()) {
      setError('Documento e Razão Social / Nome são obrigatórios.');
      return;
    }

    try {
      setLoading(true);
      setError(null);

      let savedCustomerId = customerToEdit?.id;

      if (customerToEdit) {
        const payload: UpdateCustomerRequest = {
          legalName: legalName.trim(),
          tradeName: tradeName.trim() || undefined,
          stateRegistration: isStateRegistrationExempt ? 'ISENTO' : (stateRegistration.trim() || undefined),
          isStateRegistrationExempt,
          municipalRegistration: municipalRegistration.trim() || undefined,
          icmsTaxpayerType,
          phone1: phone1.trim() || undefined,
          email: email.trim() || undefined,
          contactPerson: contactPerson.trim() || undefined,
          website: website.trim() || undefined,
          categoryId: categoryId || undefined,
          economicGroupId: economicGroupId || undefined,
          creditLimit: parseFloat(creditLimit) || 0,
          creditStatus,
          notes: notes.trim() || undefined,
          postalCode: postalCode.replace(/\D/g, '') || undefined,
          street: street.trim() || undefined,
          number: number.trim() || undefined,
          complement: complement.trim() || undefined,
          neighborhood: neighborhood.trim() || undefined,
          cityName: cityName.trim() || undefined,
          cityIbgeCode,
          stateCode: stateCode.trim().toUpperCase() || undefined,
          billingPostalCode: billingPostalCode.replace(/\D/g, '') || undefined,
          billingStreet: billingStreet.trim() || undefined,
          billingNumber: billingNumber.trim() || undefined,
          billingComplement: billingComplement.trim() || undefined,
          billingNeighborhood: billingNeighborhood.trim() || undefined,
          billingCityName: billingCityName.trim() || undefined,
          billingStateCode: billingStateCode.trim().toUpperCase() || undefined,
          deliveryPostalCode: deliveryPostalCode.replace(/\D/g, '') || undefined,
          deliveryStreet: deliveryStreet.trim() || undefined,
          deliveryNumber: deliveryNumber.trim() || undefined,
          deliveryComplement: deliveryComplement.trim() || undefined,
          deliveryNeighborhood: deliveryNeighborhood.trim() || undefined,
          deliveryCityName: deliveryCityName.trim() || undefined,
          deliveryStateCode: deliveryStateCode.trim().toUpperCase() || undefined,
        };
        const updated = await crmApi.updateCustomer(customerToEdit.id, payload);
        savedCustomerId = updated.id;
      } else {
        const payload: CreateCustomerRequest = {
          type,
          document: document.replace(/\D/g, ''),
          legalName: legalName.trim(),
          tradeName: tradeName.trim() || undefined,
          stateRegistration: isStateRegistrationExempt ? 'ISENTO' : (stateRegistration.trim() || undefined),
          isStateRegistrationExempt,
          municipalRegistration: municipalRegistration.trim() || undefined,
          icmsTaxpayerType,
          phone1: phone1.trim() || undefined,
          email: email.trim() || undefined,
          contactPerson: contactPerson.trim() || undefined,
          website: website.trim() || undefined,
          categoryId: categoryId || undefined,
          economicGroupId: economicGroupId || undefined,
          creditLimit: parseFloat(creditLimit) || 0,
          creditStatus,
          notes: notes.trim() || undefined,
          postalCode: postalCode.replace(/\D/g, '') || undefined,
          street: street.trim() || undefined,
          number: number.trim() || undefined,
          complement: complement.trim() || undefined,
          neighborhood: neighborhood.trim() || undefined,
          cityName: cityName.trim() || undefined,
          cityIbgeCode,
          stateCode: stateCode.trim().toUpperCase() || undefined,
          billingPostalCode: billingPostalCode.replace(/\D/g, '') || undefined,
          billingStreet: billingStreet.trim() || undefined,
          billingNumber: billingNumber.trim() || undefined,
          billingComplement: billingComplement.trim() || undefined,
          billingNeighborhood: billingNeighborhood.trim() || undefined,
          billingCityName: billingCityName.trim() || undefined,
          billingStateCode: billingStateCode.trim().toUpperCase() || undefined,
          deliveryPostalCode: deliveryPostalCode.replace(/\D/g, '') || undefined,
          deliveryStreet: deliveryStreet.trim() || undefined,
          deliveryNumber: deliveryNumber.trim() || undefined,
          deliveryComplement: deliveryComplement.trim() || undefined,
          deliveryNeighborhood: deliveryNeighborhood.trim() || undefined,
          deliveryCityName: deliveryCityName.trim() || undefined,
          deliveryStateCode: deliveryStateCode.trim().toUpperCase() || undefined,
        };
        const created = await crmApi.createCustomer(payload);
        savedCustomerId = created.id;
      }

      // Save custom fields if any
      if (savedCustomerId && Object.keys(customFieldValues).length > 0) {
        try {
          await customFieldsApi.saveEntityCustomValues('CUSTOMER', savedCustomerId, { values: customFieldValues });
        } catch (cfErr) {
          console.warn('Warning: custom fields save failed:', cfErr);
        }
      }

      onSaved();
      onClose();
    } catch (err: any) {
      setError(err?.response?.data?.message || 'Falha ao salvar cliente. Verifique os dados informados.');
    } finally {
      setLoading(false);
    }
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
      <div className="flex max-h-[90vh] w-full max-w-4xl flex-col rounded-xl bg-white shadow-2xl dark:bg-gray-800">
        {/* Header */}
        <div className="flex items-center justify-between border-b border-gray-200 px-6 py-4 dark:border-gray-700">
          <div className="flex items-center gap-3">
            {type === 'LEGAL_ENTITY' ? (
              <Building2 className="h-6 w-6 text-indigo-600 dark:text-indigo-400" />
            ) : (
              <User className="h-6 w-6 text-indigo-600 dark:text-indigo-400" />
            )}
            <div>
              <h2 className="text-lg font-bold text-gray-900 dark:text-white">
                {customerToEdit ? 'Editar Cliente' : 'Novo Cliente'}
              </h2>
              <p className="text-xs text-gray-500 dark:text-gray-400">
                Cadastro comercial com validação fiscal e controle de crédito
              </p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="rounded-lg p-1 text-gray-400 hover:bg-gray-100 hover:text-gray-700 dark:hover:bg-gray-700"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        {/* Tab Navigation */}
        <div className="flex border-b border-gray-200 bg-gray-50 px-6 dark:border-gray-700 dark:bg-gray-850">
          <button
            type="button"
            onClick={() => setActiveTab('main')}
            className={`flex items-center gap-2 border-b-2 py-3 px-4 text-xs font-semibold ${
              activeTab === 'main'
                ? 'border-indigo-600 text-indigo-600 dark:text-indigo-400'
                : 'border-transparent text-gray-500 hover:text-gray-700 dark:text-gray-400'
            }`}
          >
            <User className="h-4 w-4" /> Dados Gerais & Fiscais
          </button>
          <button
            type="button"
            onClick={() => setActiveTab('address')}
            className={`flex items-center gap-2 border-b-2 py-3 px-4 text-xs font-semibold ${
              activeTab === 'address'
                ? 'border-indigo-600 text-indigo-600 dark:text-indigo-400'
                : 'border-transparent text-gray-500 hover:text-gray-700 dark:text-gray-400'
            }`}
          >
            <MapPin className="h-4 w-4" /> Endereço Principal
          </button>
          <button
            type="button"
            onClick={() => setActiveTab('additionalAddress')}
            className={`flex items-center gap-2 border-b-2 py-3 px-4 text-xs font-semibold ${
              activeTab === 'additionalAddress'
                ? 'border-indigo-600 text-indigo-600 dark:text-indigo-400'
                : 'border-transparent text-gray-500 hover:text-gray-700 dark:text-gray-400'
            }`}
          >
            <Layers className="h-4 w-4" /> Cobrança & Entrega
          </button>
          <button
            type="button"
            onClick={() => setActiveTab('customFields')}
            className={`flex items-center gap-2 border-b-2 py-3 px-4 text-xs font-semibold ${
              activeTab === 'customFields'
                ? 'border-indigo-600 text-indigo-600 dark:text-indigo-400'
                : 'border-transparent text-gray-500 hover:text-gray-700 dark:text-gray-400'
            }`}
          >
            <Sparkles className="h-4 w-4" /> Campos Customizados
          </button>
        </div>

        {/* Form Body */}
        <form onSubmit={handleSubmit} className="flex flex-1 flex-col overflow-y-auto p-6">
          {error && (
            <div className="mb-4 rounded-lg bg-red-50 p-3 text-sm text-red-700 dark:bg-red-900/30 dark:text-red-300">
              {error}
            </div>
          )}

          {activeTab === 'main' && (
            <div className="space-y-4">
              {/* Type selector */}
              {!customerToEdit && (
                <div className="flex gap-4 p-1 bg-gray-100 rounded-lg dark:bg-gray-700 w-fit">
                  <button
                    type="button"
                    onClick={() => setType('LEGAL_ENTITY')}
                    className={`px-4 py-1.5 rounded-md text-xs font-semibold transition ${
                      type === 'LEGAL_ENTITY'
                        ? 'bg-white shadow text-indigo-600 dark:bg-gray-800 dark:text-indigo-400'
                        : 'text-gray-600 dark:text-gray-300'
                    }`}
                  >
                    Pessoa Jurídica (CNPJ)
                  </button>
                  <button
                    type="button"
                    onClick={() => setType('INDIVIDUAL')}
                    className={`px-4 py-1.5 rounded-md text-xs font-semibold transition ${
                      type === 'INDIVIDUAL'
                        ? 'bg-white shadow text-indigo-600 dark:bg-gray-800 dark:text-indigo-400'
                        : 'text-gray-600 dark:text-gray-300'
                    }`}
                  >
                    Pessoa Física (CPF)
                  </button>
                </div>
              )}

              <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                <div>
                  <label className="block text-xs font-medium text-gray-700 dark:text-gray-300">
                    {type === 'LEGAL_ENTITY' ? 'CNPJ *' : 'CPF *'}
                  </label>
                  <input
                    type="text"
                    required
                    disabled={!!customerToEdit}
                    placeholder={type === 'LEGAL_ENTITY' ? '00.000.000/0000-00' : '000.000.000-00'}
                    value={document}
                    onChange={(e) => setDocument(e.target.value)}
                    className="mt-1 w-full rounded-md border border-gray-300 px-3 py-2 text-sm dark:border-gray-600 dark:bg-gray-700 dark:text-white disabled:bg-gray-100 dark:disabled:bg-gray-800 font-mono"
                  />
                </div>

                <div className="md:col-span-2">
                  <label className="block text-xs font-medium text-gray-700 dark:text-gray-300">
                    {type === 'LEGAL_ENTITY' ? 'Razão Social *' : 'Nome Completo *'}
                  </label>
                  <input
                    type="text"
                    required
                    placeholder={type === 'LEGAL_ENTITY' ? 'Ex: Tech Solutions Ltda' : 'Ex: João da Silva'}
                    value={legalName}
                    onChange={(e) => setLegalName(e.target.value)}
                    className="mt-1 w-full rounded-md border border-gray-300 px-3 py-2 text-sm dark:border-gray-600 dark:bg-gray-700 dark:text-white"
                  />
                </div>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                <div>
                  <label className="block text-xs font-medium text-gray-700 dark:text-gray-300">
                    {type === 'LEGAL_ENTITY' ? 'Nome Fantasia' : 'Apelido'}
                  </label>
                  <input
                    type="text"
                    placeholder="Ex: Tech Sol"
                    value={tradeName}
                    onChange={(e) => setTradeName(e.target.value)}
                    className="mt-1 w-full rounded-md border border-gray-300 px-3 py-2 text-sm dark:border-gray-600 dark:bg-gray-700 dark:text-white"
                  />
                </div>

                <div>
                  <div className="flex justify-between items-center">
                    <label className="block text-xs font-medium text-gray-700 dark:text-gray-300">
                      {type === 'LEGAL_ENTITY' ? 'Inscrição Estadual' : 'RG'}
                    </label>
                    {type === 'LEGAL_ENTITY' && (
                      <label className="flex items-center gap-1 text-xs text-gray-500">
                        <input
                          type="checkbox"
                          checked={isStateRegistrationExempt}
                          onChange={(e) => setIsStateRegistrationExempt(e.target.checked)}
                          className="rounded border-gray-300 text-indigo-600"
                        />
                        Isento
                      </label>
                    )}
                  </div>
                  <input
                    type="text"
                    disabled={isStateRegistrationExempt}
                    placeholder={isStateRegistrationExempt ? 'ISENTO' : 'Número da IE'}
                    value={isStateRegistrationExempt ? 'ISENTO' : stateRegistration}
                    onChange={(e) => setStateRegistration(e.target.value)}
                    className="mt-1 w-full rounded-md border border-gray-300 px-3 py-2 text-sm dark:border-gray-600 dark:bg-gray-700 dark:text-white disabled:bg-gray-100 dark:disabled:bg-gray-800"
                  />
                </div>

                <div>
                  <label className="block text-xs font-medium text-gray-700 dark:text-gray-300">Indicador ICMS</label>
                  <select
                    value={icmsTaxpayerType}
                    onChange={(e) => setIcmsTaxpayerType(e.target.value as IcmsTaxpayerType)}
                    className="mt-1 w-full rounded-md border border-gray-300 px-3 py-2 text-sm dark:border-gray-600 dark:bg-gray-700 dark:text-white"
                  >
                    <option value="CONTRIBUTOR">1 - Contribuinte ICMS</option>
                    <option value="NON_CONTRIBUTOR">9 - Não Contribuinte</option>
                    <option value="EXEMPT">2 - Isento</option>
                  </select>
                </div>
              </div>

              {/* Segmentation & Financials */}
              <div className="rounded-lg border border-gray-200 p-4 dark:border-gray-700 space-y-4">
                <h3 className="text-xs font-bold uppercase tracking-wider text-gray-500 dark:text-gray-400">
                  Segmentação Comercial & Crédito
                </h3>
                <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
                  <div>
                    <label className="block text-xs font-medium text-gray-700 dark:text-gray-300">Categoria</label>
                    <select
                      value={categoryId}
                      onChange={(e) => setCategoryId(e.target.value)}
                      className="mt-1 w-full rounded-md border border-gray-300 px-3 py-2 text-sm dark:border-gray-600 dark:bg-gray-700 dark:text-white"
                    >
                      <option value="">Nenhuma Categoria</option>
                      {categories.map((c) => (
                        <option key={c.id} value={c.id}>
                          {c.name} ({c.code})
                        </option>
                      ))}
                    </select>
                  </div>

                  <div>
                    <label className="block text-xs font-medium text-gray-700 dark:text-gray-300">Grupo Econômico</label>
                    <select
                      value={economicGroupId}
                      onChange={(e) => setEconomicGroupId(e.target.value)}
                      className="mt-1 w-full rounded-md border border-gray-300 px-3 py-2 text-sm dark:border-gray-600 dark:bg-gray-700 dark:text-white"
                    >
                      <option value="">Nenhum Grupo</option>
                      {economicGroups.map((g) => (
                        <option key={g.id} value={g.id}>
                          {g.name} ({g.code})
                        </option>
                      ))}
                    </select>
                  </div>

                  <div>
                    <label className="block text-xs font-medium text-gray-700 dark:text-gray-300">Limite de Crédito (R$)</label>
                    <input
                      type="number"
                      step="0.01"
                      min="0"
                      value={creditLimit}
                      onChange={(e) => setCreditLimit(e.target.value)}
                      className="mt-1 w-full rounded-md border border-gray-300 px-3 py-2 text-sm dark:border-gray-600 dark:bg-gray-700 dark:text-white"
                    />
                  </div>

                  <div>
                    <label className="block text-xs font-medium text-gray-700 dark:text-gray-300">Status de Crédito</label>
                    <select
                      value={creditStatus}
                      onChange={(e) => setCreditStatus(e.target.value as CreditStatus)}
                      className="mt-1 w-full rounded-md border border-gray-300 px-3 py-2 text-sm dark:border-gray-600 dark:bg-gray-700 dark:text-white"
                    >
                      <option value="APPROVED">Aprovado</option>
                      <option value="SUSPENDED">Suspenso</option>
                      <option value="BLOCKED">Bloqueado</option>
                    </select>
                  </div>
                </div>
              </div>

              {/* Contact Info */}
              <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                <div>
                  <label className="block text-xs font-medium text-gray-700 dark:text-gray-300">Telefone / WhatsApp</label>
                  <input
                    type="text"
                    placeholder="(11) 99999-9999"
                    value={phone1}
                    onChange={(e) => setPhone1(e.target.value)}
                    className="mt-1 w-full rounded-md border border-gray-300 px-3 py-2 text-sm dark:border-gray-600 dark:bg-gray-700 dark:text-white"
                  />
                </div>
                <div>
                  <label className="block text-xs font-medium text-gray-700 dark:text-gray-300">E-mail Comercial</label>
                  <input
                    type="email"
                    placeholder="financeiro@cliente.com.br"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    className="mt-1 w-full rounded-md border border-gray-300 px-3 py-2 text-sm dark:border-gray-600 dark:bg-gray-700 dark:text-white"
                  />
                </div>
                <div>
                  <label className="block text-xs font-medium text-gray-700 dark:text-gray-300">Pessoa de Contato</label>
                  <input
                    type="text"
                    placeholder="Nome do Comprador / Gerente"
                    value={contactPerson}
                    onChange={(e) => setContactPerson(e.target.value)}
                    className="mt-1 w-full rounded-md border border-gray-300 px-3 py-2 text-sm dark:border-gray-600 dark:bg-gray-700 dark:text-white"
                  />
                </div>
              </div>
            </div>
          )}

          {activeTab === 'address' && (
            <div className="space-y-4">
              <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
                <div>
                  <label className="block text-xs font-medium text-gray-700 dark:text-gray-300">CEP (Busca Automática)</label>
                  <input
                    type="text"
                    placeholder="00000-000"
                    value={postalCode}
                    onChange={(e) => handleCepLookup(e.target.value)}
                    className="mt-1 w-full rounded-md border border-gray-300 px-3 py-2 text-sm dark:border-gray-600 dark:bg-gray-700 dark:text-white font-mono"
                  />
                </div>
                <div className="md:col-span-2">
                  <label className="block text-xs font-medium text-gray-700 dark:text-gray-300">Logradouro / Rua</label>
                  <input
                    type="text"
                    value={street}
                    onChange={(e) => setStreet(e.target.value)}
                    className="mt-1 w-full rounded-md border border-gray-300 px-3 py-2 text-sm dark:border-gray-600 dark:bg-gray-700 dark:text-white"
                  />
                </div>
                <div>
                  <label className="block text-xs font-medium text-gray-700 dark:text-gray-300">Número</label>
                  <input
                    type="text"
                    value={number}
                    onChange={(e) => setNumber(e.target.value)}
                    className="mt-1 w-full rounded-md border border-gray-300 px-3 py-2 text-sm dark:border-gray-600 dark:bg-gray-700 dark:text-white"
                  />
                </div>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                <div>
                  <label className="block text-xs font-medium text-gray-700 dark:text-gray-300">Complemento</label>
                  <input
                    type="text"
                    placeholder="Sala, Andar, Galpão"
                    value={complement}
                    onChange={(e) => setComplement(e.target.value)}
                    className="mt-1 w-full rounded-md border border-gray-300 px-3 py-2 text-sm dark:border-gray-600 dark:bg-gray-700 dark:text-white"
                  />
                </div>
                <div>
                  <label className="block text-xs font-medium text-gray-700 dark:text-gray-300">Bairro</label>
                  <input
                    type="text"
                    value={neighborhood}
                    onChange={(e) => setNeighborhood(e.target.value)}
                    className="mt-1 w-full rounded-md border border-gray-300 px-3 py-2 text-sm dark:border-gray-600 dark:bg-gray-700 dark:text-white"
                  />
                </div>
                <div className="grid grid-cols-3 gap-2">
                  <div className="col-span-2">
                    <label className="block text-xs font-medium text-gray-700 dark:text-gray-300">Cidade</label>
                    <input
                      type="text"
                      value={cityName}
                      onChange={(e) => setCityName(e.target.value)}
                      className="mt-1 w-full rounded-md border border-gray-300 px-3 py-2 text-sm dark:border-gray-600 dark:bg-gray-700 dark:text-white"
                    />
                  </div>
                  <div>
                    <label className="block text-xs font-medium text-gray-700 dark:text-gray-300">UF</label>
                    <input
                      type="text"
                      maxLength={2}
                      value={stateCode}
                      onChange={(e) => setStateCode(e.target.value.toUpperCase())}
                      className="mt-1 w-full rounded-md border border-gray-300 px-3 py-2 text-sm dark:border-gray-600 dark:bg-gray-700 dark:text-white uppercase font-mono"
                    />
                  </div>
                </div>
              </div>
            </div>
          )}

          {activeTab === 'additionalAddress' && (
            <div className="space-y-6">
              {/* Billing */}
              <div className="rounded-lg border border-gray-200 p-4 dark:border-gray-700 space-y-3">
                <h3 className="text-xs font-bold uppercase tracking-wider text-indigo-600 dark:text-indigo-400">
                  Endereço de Cobrança (Opcional)
                </h3>
                <div className="grid grid-cols-1 md:grid-cols-4 gap-3">
                  <div>
                    <label className="block text-xs font-medium text-gray-700 dark:text-gray-300">CEP</label>
                    <input
                      type="text"
                      value={billingPostalCode}
                      onChange={(e) => setBillingPostalCode(e.target.value)}
                      className="mt-1 w-full rounded-md border border-gray-300 px-3 py-1.5 text-sm dark:border-gray-600 dark:bg-gray-700 dark:text-white"
                    />
                  </div>
                  <div className="md:col-span-2">
                    <label className="block text-xs font-medium text-gray-700 dark:text-gray-300">Rua</label>
                    <input
                      type="text"
                      value={billingStreet}
                      onChange={(e) => setBillingStreet(e.target.value)}
                      className="mt-1 w-full rounded-md border border-gray-300 px-3 py-1.5 text-sm dark:border-gray-600 dark:bg-gray-700 dark:text-white"
                    />
                  </div>
                  <div>
                    <label className="block text-xs font-medium text-gray-700 dark:text-gray-300">Número</label>
                    <input
                      type="text"
                      value={billingNumber}
                      onChange={(e) => setBillingNumber(e.target.value)}
                      className="mt-1 w-full rounded-md border border-gray-300 px-3 py-1.5 text-sm dark:border-gray-600 dark:bg-gray-700 dark:text-white"
                    />
                  </div>
                </div>
              </div>

              {/* Delivery */}
              <div className="rounded-lg border border-gray-200 p-4 dark:border-gray-700 space-y-3">
                <h3 className="text-xs font-bold uppercase tracking-wider text-emerald-600 dark:text-emerald-400">
                  Endereço de Entrega (Opcional)
                </h3>
                <div className="grid grid-cols-1 md:grid-cols-4 gap-3">
                  <div>
                    <label className="block text-xs font-medium text-gray-700 dark:text-gray-300">CEP</label>
                    <input
                      type="text"
                      value={deliveryPostalCode}
                      onChange={(e) => setDeliveryPostalCode(e.target.value)}
                      className="mt-1 w-full rounded-md border border-gray-300 px-3 py-1.5 text-sm dark:border-gray-600 dark:bg-gray-700 dark:text-white"
                    />
                  </div>
                  <div className="md:col-span-2">
                    <label className="block text-xs font-medium text-gray-700 dark:text-gray-300">Rua</label>
                    <input
                      type="text"
                      value={deliveryStreet}
                      onChange={(e) => setDeliveryStreet(e.target.value)}
                      className="mt-1 w-full rounded-md border border-gray-300 px-3 py-1.5 text-sm dark:border-gray-600 dark:bg-gray-700 dark:text-white"
                    />
                  </div>
                  <div>
                    <label className="block text-xs font-medium text-gray-700 dark:text-gray-300">Número</label>
                    <input
                      type="text"
                      value={deliveryNumber}
                      onChange={(e) => setDeliveryNumber(e.target.value)}
                      className="mt-1 w-full rounded-md border border-gray-300 px-3 py-1.5 text-sm dark:border-gray-600 dark:bg-gray-700 dark:text-white"
                    />
                  </div>
                </div>
              </div>
            </div>
          )}

          {activeTab === 'customFields' && (
            <div className="space-y-4">
              <CustomFieldFormSection
                targetEntity="CUSTOMER"
                values={customFieldValues}
                onChange={(key, val) => setCustomFieldValues((prev) => ({ ...prev, [key]: val }))}
                isNewRecord={!customerToEdit}
              />
            </div>
          )}

          {/* Footer Actions */}
          <div className="mt-8 flex justify-end gap-3 border-t border-gray-200 pt-4 dark:border-gray-700">
            <button
              type="button"
              onClick={onClose}
              className="rounded-lg bg-gray-100 px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-200 dark:bg-gray-700 dark:text-gray-300"
            >
              Cancelar
            </button>
            <button
              type="submit"
              disabled={loading}
              className="rounded-lg bg-indigo-600 px-5 py-2 text-sm font-medium text-white hover:bg-indigo-700 disabled:opacity-50"
            >
              {loading ? 'Salvando...' : customerToEdit ? 'Atualizar Cliente' : 'Cadastrar Cliente'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
