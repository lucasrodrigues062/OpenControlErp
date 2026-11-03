export type CustomerType = 'INDIVIDUAL' | 'LEGAL_ENTITY';
export type IcmsTaxpayerType = 'CONTRIBUTOR' | 'NON_CONTRIBUTOR' | 'EXEMPT';
export type CreditStatus = 'APPROVED' | 'SUSPENDED' | 'BLOCKED';

export interface CustomerCategory {
  id: string;
  code: string;
  name: string;
  description?: string | null;
  colorHex: string;
  defaultDiscountPct: number;
  isActive: boolean;
}

export interface EconomicGroup {
  id: string;
  code: string;
  name: string;
  holdingCnpj?: string | null;
  consolidatedCreditLimit: number;
  isActive: boolean;
}

export interface Customer {
  id: string;
  tenantId: string;
  branchId?: string | null;
  type: CustomerType;
  document: string;
  formattedDocument: string;
  legalName: string;
  tradeName?: string | null;
  stateRegistration?: string | null;
  isStateRegistrationExempt: boolean;
  municipalRegistration?: string | null;
  icmsTaxpayerType: IcmsTaxpayerType;
  mainCnae?: string | null;
  birthOrFoundingDate?: string | null;
  phone1?: string | null;
  phone2?: string | null;
  email?: string | null;
  contactPerson?: string | null;
  website?: string | null;
  categoryId?: string | null;
  categoryName?: string | null;
  categoryColorHex?: string | null;
  economicGroupId?: string | null;
  economicGroupName?: string | null;
  creditLimit: number;
  creditStatus: CreditStatus;
  notes?: string | null;
  isActive: boolean;

  // Primary Address
  postalCode?: string | null;
  street?: string | null;
  number?: string | null;
  complement?: string | null;
  neighborhood?: string | null;
  cityName?: string | null;
  cityIbgeCode?: number | null;
  stateCode?: string | null;
  countryCode?: string;

  // Billing Address
  billingPostalCode?: string | null;
  billingStreet?: string | null;
  billingNumber?: string | null;
  billingComplement?: string | null;
  billingNeighborhood?: string | null;
  billingCityName?: string | null;
  billingStateCode?: string | null;

  // Delivery Address
  deliveryPostalCode?: string | null;
  deliveryStreet?: string | null;
  deliveryNumber?: string | null;
  deliveryComplement?: string | null;
  deliveryNeighborhood?: string | null;
  deliveryCityName?: string | null;
  deliveryStateCode?: string | null;

  createdAt: string;
  updatedAt: string;
}

export interface CustomerPageResponse {
  content: Customer[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export interface CreateCustomerRequest {
  type: CustomerType;
  document: string;
  legalName: string;
  tradeName?: string;
  stateRegistration?: string;
  isStateRegistrationExempt?: boolean;
  municipalRegistration?: string;
  icmsTaxpayerType?: IcmsTaxpayerType;
  mainCnae?: string;
  birthOrFoundingDate?: string;
  phone1?: string;
  phone2?: string;
  email?: string;
  contactPerson?: string;
  website?: string;
  categoryId?: string;
  economicGroupId?: string;
  branchId?: string;
  creditLimit?: number;
  creditStatus?: CreditStatus;
  notes?: string;
  postalCode?: string;
  street?: string;
  number?: string;
  complement?: string;
  neighborhood?: string;
  cityName?: string;
  cityIbgeCode?: number;
  stateCode?: string;
  billingPostalCode?: string;
  billingStreet?: string;
  billingNumber?: string;
  billingComplement?: string;
  billingNeighborhood?: string;
  billingCityName?: string;
  billingStateCode?: string;
  deliveryPostalCode?: string;
  deliveryStreet?: string;
  deliveryNumber?: string;
  deliveryComplement?: string;
  deliveryNeighborhood?: string;
  deliveryCityName?: string;
  deliveryStateCode?: string;
}

export interface UpdateCustomerRequest {
  legalName: string;
  tradeName?: string;
  stateRegistration?: string;
  isStateRegistrationExempt?: boolean;
  municipalRegistration?: string;
  icmsTaxpayerType?: IcmsTaxpayerType;
  mainCnae?: string;
  birthOrFoundingDate?: string;
  phone1?: string;
  phone2?: string;
  email?: string;
  contactPerson?: string;
  website?: string;
  categoryId?: string;
  economicGroupId?: string;
  creditLimit?: number;
  creditStatus?: CreditStatus;
  notes?: string;
  isActive?: boolean;
  postalCode?: string;
  street?: string;
  number?: string;
  complement?: string;
  neighborhood?: string;
  cityName?: string;
  cityIbgeCode?: number;
  stateCode?: string;
  billingPostalCode?: string;
  billingStreet?: string;
  billingNumber?: string;
  billingComplement?: string;
  billingNeighborhood?: string;
  billingCityName?: string;
  billingStateCode?: string;
  deliveryPostalCode?: string;
  deliveryStreet?: string;
  deliveryNumber?: string;
  deliveryComplement?: string;
  deliveryNeighborhood?: string;
  deliveryCityName?: string;
  deliveryStateCode?: string;
}

export type LeadStatus = 'NEW' | 'IN_QUALIFICATION' | 'QUALIFIED' | 'DISQUALIFIED' | 'CONVERTED';
export type LeadSource = 'WEBSITE' | 'INBOUND' | 'EVENT' | 'OUTBOUND' | 'REFERRAL' | 'SOCIAL_MEDIA' | 'OTHER';

export interface Lead {
  id: string;
  tenantId: string;
  branchId?: string | null;
  name: string;
  companyName?: string | null;
  email?: string | null;
  phone?: string | null;
  source: LeadSource;
  status: LeadStatus;
  estimatedBudget: number;
  interestSummary?: string | null;
  assignedUserId?: string | null;
  assignedUserName?: string | null;
  disqualificationReason?: string | null;
  convertedCustomerId?: string | null;
  convertedDealId?: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreateLeadRequest {
  name: string;
  companyName?: string;
  email?: string;
  phone?: string;
  source?: LeadSource;
  estimatedBudget?: number;
  interestSummary?: string;
  assignedUserId?: string;
  branchId?: string;
}

export interface ConvertLeadRequest {
  customerType: CustomerType;
  customerDocument: string;
  customerLegalName?: string;
  dealTitle: string;
  dealEstimatedValue: number;
  pipelineId: string;
  stageId: string;
}

export interface LeadConversionResponse {
  leadId: string;
  customerId: string;
  dealId: string;
}

export type StageType = 'IN_PROGRESS' | 'WON' | 'LOST';

export interface PipelineStage {
  id: string;
  pipelineId: string;
  name: string;
  orderIndex: number;
  winProbabilityPct: number;
  colorHex: string;
  maxStagnantDays: number;
  stageType: StageType;
}

export interface CreatePipelineStageRequest {
  name: string;
  orderIndex?: number;
  winProbabilityPct?: number;
  colorHex?: string;
  maxStagnantDays?: number;
  stageType?: StageType;
}

export interface UpdatePipelineStageRequest {
  name: string;
  winProbabilityPct: number;
  colorHex: string;
  maxStagnantDays: number;
  stageType: StageType;
}

export interface SalesPipeline {
  id: string;
  code: string;
  name: string;
  isDefault: boolean;
  isActive: boolean;
  stages: PipelineStage[];
}

export type DealStatus = 'OPEN' | 'WON' | 'LOST';

export interface Deal {
  id: string;
  tenantId: string;
  branchId?: string | null;
  pipelineId: string;
  stageId: string;
  stageName?: string;
  stageColorHex?: string;
  customerId: string;
  customerName?: string;
  leadId?: string | null;
  title: string;
  estimatedValue: number;
  currency: string;
  expectedCloseDate?: string | null;
  status: DealStatus;
  lossReason?: string | null;
  assignedUserId?: string | null;
  assignedUserName?: string | null;
  closedAt?: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreateDealRequest {
  title: string;
  pipelineId: string;
  stageId: string;
  customerId: string;
  leadId?: string;
  estimatedValue: number;
  currency?: string;
  expectedCloseDate?: string;
  assignedUserId?: string;
  branchId?: string;
}

export interface PipelineMetrics {
  totalOpenDeals: number;
  totalPipelineValue: number;
  weightedPipelineValue: number;
  winRatePct: number;
}

export type ActivityEntityType = 'CUSTOMER' | 'LEAD' | 'DEAL';
export type ActivityType = 'NOTE' | 'CALL' | 'MEETING' | 'WHATSAPP' | 'TASK';

export interface CrmActivity {
  id: string;
  tenantId: string;
  entityType: ActivityEntityType;
  entityId: string;
  activityType: ActivityType;
  summary: string;
  description?: string | null;
  performedAt: string;
  dueDate?: string | null;
  isCompleted: boolean;
  authorUserId?: string | null;
  authorUserName?: string | null;
  createdAt: string;
}

export interface CreateCrmActivityRequest {
  entityType: ActivityEntityType;
  entityId: string;
  activityType: ActivityType;
  summary: string;
  description?: string;
  performedAt?: string;
  dueDate?: string;
  isCompleted?: boolean;
}
