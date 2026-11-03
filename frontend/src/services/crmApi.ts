import axios from 'axios';
import {
  Customer,
  CustomerPageResponse,
  CreateCustomerRequest,
  UpdateCustomerRequest,
  CustomerCategory,
  EconomicGroup,
  Lead,
  CreateLeadRequest,
  ConvertLeadRequest,
  LeadConversionResponse,
  SalesPipeline,
  PipelineStage,
  CreatePipelineStageRequest,
  UpdatePipelineStageRequest,
  Deal,
  CreateDealRequest,
  PipelineMetrics,
  CrmActivity,
  CreateCrmActivityRequest,
} from '../types/crm';

const api = axios.create({
  baseURL: '/api/v1/crm',
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('opencontrol_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

export const crmApi = {
  // Customers
  async listCustomers(params?: {
    query?: string;
    type?: string;
    categoryId?: string;
    economicGroupId?: string;
    activeOnly?: boolean;
    page?: number;
    size?: number;
  }): Promise<CustomerPageResponse> {
    const res = await api.get<CustomerPageResponse>('/customers', { params });
    return res.data;
  },

  async getCustomer(id: string): Promise<Customer> {
    const res = await api.get<Customer>(`/customers/${id}`);
    return res.data;
  },

  async createCustomer(data: CreateCustomerRequest): Promise<Customer> {
    const res = await api.post<Customer>('/customers', data);
    return res.data;
  },

  async updateCustomer(id: string, data: UpdateCustomerRequest): Promise<Customer> {
    const res = await api.put<Customer>(`/customers/${id}`, data);
    return res.data;
  },

  async toggleCustomerStatus(id: string, active: boolean): Promise<Customer> {
    const res = await api.patch<Customer>(`/customers/${id}/status`, { active });
    return res.data;
  },

  // Customer Categories
  async listCustomerCategories(): Promise<CustomerCategory[]> {
    const res = await api.get<CustomerCategory[]>('/customer-categories');
    return res.data;
  },

  async createCustomerCategory(data: Partial<CustomerCategory>): Promise<CustomerCategory> {
    const res = await api.post<CustomerCategory>('/customer-categories', data);
    return res.data;
  },

  async updateCustomerCategory(id: string, data: Partial<CustomerCategory>): Promise<CustomerCategory> {
    const res = await api.put<CustomerCategory>(`/customer-categories/${id}`, data);
    return res.data;
  },

  async deleteCustomerCategory(id: string): Promise<void> {
    await api.delete(`/customer-categories/${id}`);
  },

  // Economic Groups
  async listEconomicGroups(): Promise<EconomicGroup[]> {
    const res = await api.get<EconomicGroup[]>('/economic-groups');
    return res.data;
  },

  async createEconomicGroup(data: Partial<EconomicGroup>): Promise<EconomicGroup> {
    const res = await api.post<EconomicGroup>('/economic-groups', data);
    return res.data;
  },

  async updateEconomicGroup(id: string, data: Partial<EconomicGroup>): Promise<EconomicGroup> {
    const res = await api.put<EconomicGroup>(`/economic-groups/${id}`, data);
    return res.data;
  },

  async deleteEconomicGroup(id: string): Promise<void> {
    await api.delete(`/economic-groups/${id}`);
  },

  // Leads
  async listLeads(params?: {
    status?: string;
    source?: string;
    assignedUserId?: string;
  }): Promise<Lead[]> {
    const res = await api.get<Lead[]>('/leads', { params });
    return res.data;
  },

  async getLead(id: string): Promise<Lead> {
    const res = await api.get<Lead>(`/leads/${id}`);
    return res.data;
  },

  async createLead(data: CreateLeadRequest): Promise<Lead> {
    const res = await api.post<Lead>('/leads', data);
    return res.data;
  },

  async updateLead(id: string, data: Partial<CreateLeadRequest>): Promise<Lead> {
    const res = await api.put<Lead>(`/leads/${id}`, data);
    return res.data;
  },

  async updateLeadStatus(id: string, status: string): Promise<Lead> {
    const res = await api.patch<Lead>(`/leads/${id}/status`, { status });
    return res.data;
  },

  async disqualifyLead(id: string, reason: string): Promise<Lead> {
    const res = await api.post<Lead>(`/leads/${id}/disqualify`, { reason });
    return res.data;
  },

  async convertLead(id: string, data: ConvertLeadRequest): Promise<LeadConversionResponse> {
    const res = await api.post<LeadConversionResponse>(`/leads/${id}/convert`, data);
    return res.data;
  },

  // Pipelines & Stages
  async listPipelines(): Promise<SalesPipeline[]> {
    const res = await api.get<SalesPipeline[]>('/pipelines');
    return res.data;
  },

  async createPipeline(data: { code: string; name: string; isDefault?: boolean }): Promise<SalesPipeline> {
    const res = await api.post<SalesPipeline>('/pipelines', data);
    return res.data;
  },

  async updatePipeline(id: string, data: { name: string; isDefault?: boolean }): Promise<SalesPipeline> {
    const res = await api.put<SalesPipeline>(`/pipelines/${id}`, data);
    return res.data;
  },

  async addStage(pipelineId: string, data: CreatePipelineStageRequest): Promise<PipelineStage> {
    const res = await api.post<PipelineStage>(`/pipelines/${pipelineId}/stages`, data);
    return res.data;
  },

  async updateStage(pipelineId: string, stageId: string, data: UpdatePipelineStageRequest): Promise<PipelineStage> {
    const res = await api.put<PipelineStage>(`/pipelines/${pipelineId}/stages/${stageId}`, data);
    return res.data;
  },

  async deleteStage(pipelineId: string, stageId: string): Promise<void> {
    await api.delete(`/pipelines/${pipelineId}/stages/${stageId}`);
  },

  async reorderStages(pipelineId: string, stageIds: string[]): Promise<PipelineStage[]> {
    const res = await api.put<PipelineStage[]>(`/pipelines/${pipelineId}/stages/reorder`, { stageIds });
    return res.data;
  },

  // Deals
  async listDeals(params?: {
    pipelineId?: string;
    stageId?: string;
    status?: string;
    assignedUserId?: string;
  }): Promise<Deal[]> {
    const res = await api.get<Deal[]>('/deals', { params });
    return res.data;
  },

  async createDeal(data: CreateDealRequest): Promise<Deal> {
    const res = await api.post<Deal>('/deals', data);
    return res.data;
  },

  async updateDeal(id: string, data: Partial<CreateDealRequest>): Promise<Deal> {
    const res = await api.put<Deal>(`/deals/${id}`, data);
    return res.data;
  },

  async updateDealStage(id: string, stageId: string, lossReason?: string): Promise<Deal> {
    const res = await api.patch<Deal>(`/deals/${id}/stage`, { stageId, lossReason });
    return res.data;
  },

  async getPipelineMetrics(pipelineId?: string): Promise<PipelineMetrics> {
    const res = await api.get<PipelineMetrics>('/deals/metrics', { params: { pipelineId } });
    return res.data;
  },

  // CRM Activities
  async listActivities(params: { entityType: string; entityId: string }): Promise<CrmActivity[]> {
    const res = await api.get<CrmActivity[]>('/activities', { params });
    return res.data;
  },

  async createActivity(data: CreateCrmActivityRequest): Promise<CrmActivity> {
    const res = await api.post<CrmActivity>('/activities', data);
    return res.data;
  },
};
