export type EstablishmentType = 'HEADQUARTERS' | 'BRANCH';

export type TaxRegime = 'SIMPLES_NACIONAL' | 'SIMPLES_EXCESSO_SUBLIMITE' | 'REGIME_NORMAL';

export type PisCofinsRegime = 'CUMULATIVO' | 'NAO_CUMULATIVO';

export type NfeEnvironment = 'HOMOLOGATION' | 'PRODUCTION';

export type DanfeLayout = 'PORTRAIT' | 'LANDSCAPE';

export interface AddressDto {
  postalCode?: string;
  street?: string;
  number?: string;
  complement?: string;
  neighborhood?: string;
  cityName: string;
  cityIbgeCode?: number;
  stateCode: string;
  countryCode?: string;
}

export interface FiscalSettingsDto {
  taxRegime: TaxRegime;
  pisCofinsRegime: PisCofinsRegime;
  isTaxSubstitute: boolean;
  isIpiTaxpayer: boolean;
  isCulturalPromoter: boolean;
  calculateFcp: boolean;
  deductIcmsFromPiscofinsBasis: boolean;
  icmsSnCreditRate?: number;
  issRate?: number;
  cbsRate?: number;
  nfeEnvironment: NfeEnvironment;
  nfeSeries?: string;
  nfeModel?: string;
  nfseSeries?: string;
  mdfeSeries?: string;
  defaultInvoiceMessage?: string;
  danfeLayout?: DanfeLayout;
  nfseUser?: string;
  nfsePassword?: string;
  nfseAccessKey?: string;
}

export interface CertificateMetadataDto {
  certificateType: string;
  serialNumber?: string;
  subjectName?: string;
  issuerName?: string;
  cnpj?: string;
  validFrom?: string;
  validUntil: string;
  isExpired: boolean;
  daysUntilExpiration: number;
}

export interface AccountantDto {
  name: string;
  cpf?: string;
  crc?: string;
  email?: string;
  phone?: string;
  postalCode?: string;
  street?: string;
  number?: string;
  neighborhood?: string;
  cityName?: string;
  stateCode?: string;
}

export interface CompanyBranchSummary {
  id: string;
  type: EstablishmentType;
  parentId?: string | null;
  code: string;
  tradeName: string;
  legalName: string;
  cnpj: string;
  cityName: string;
  stateCode: string;
  isActive?: boolean;
  active?: boolean;
  logoUrl?: string;
  certificateValidUntil?: string | null;
  isCertificateExpired?: boolean;
  certificateExpired?: boolean;
}

export interface CompanyBranchDetail {
  id: string;
  type: EstablishmentType;
  parentId?: string | null;
  code: string;
  tradeName: string;
  legalName: string;
  cnpj: string;
  stateRegistration?: string;
  isStateRegistrationExempt?: boolean;
  stateRegistrationExempt?: boolean;
  municipalRegistration?: string;
  mainCnae?: string;
  nfseCnae?: string;
  phone1?: string;
  phone2?: string;
  email?: string;
  responsiblePerson?: string;
  website?: string;
  logoUrl?: string;
  address: AddressDto;
  isActive?: boolean;
  active?: boolean;
  fiscalSettings?: FiscalSettingsDto;
  accountant?: AccountantDto;
  certificate?: CertificateMetadataDto;
  createdAt: string;
  updatedAt: string;
}

export interface CreateCompanyBranchRequest {
  type: EstablishmentType;
  parentId?: string | null;
  code: string;
  tradeName: string;
  legalName: string;
  cnpj: string;
  stateRegistration?: string;
  isStateRegistrationExempt?: boolean;
  municipalRegistration?: string;
  mainCnae?: string;
  nfseCnae?: string;
  phone1?: string;
  phone2?: string;
  email?: string;
  responsiblePerson?: string;
  website?: string;
  logoUrl?: string;
  address: AddressDto;
}

export interface UpdateCompanyBranchRequest {
  tradeName: string;
  legalName: string;
  stateRegistration?: string;
  isStateRegistrationExempt?: boolean;
  municipalRegistration?: string;
  mainCnae?: string;
  nfseCnae?: string;
  phone1?: string;
  phone2?: string;
  email?: string;
  responsiblePerson?: string;
  website?: string;
  logoUrl?: string;
  address: AddressDto;
}

export interface UserBranchesResponse {
  activeBranchId: string | null;
  branches: CompanyBranchSummary[];
}
