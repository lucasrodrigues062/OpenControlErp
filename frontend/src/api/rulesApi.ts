import { apiClient } from './client';
import {
  CustomRule,
  RuleRequest,
  RuleSimulationRequest,
  RuleSimulationResponse,
  RuleSummary,
  RuleExecutionTrace,
} from '../types/rules';

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export const rulesApi = {
  listRules: async (page = 0, size = 20): Promise<PageResponse<RuleSummary>> => {
    const res = await apiClient.get<PageResponse<RuleSummary>>('/rules', {
      params: { page, size },
    });
    return res.data;
  },

  createRule: async (payload: RuleRequest): Promise<CustomRule> => {
    const res = await apiClient.post<CustomRule>('/rules', payload);
    return res.data;
  },

  getRule: async (id: string): Promise<CustomRule> => {
    const res = await apiClient.get<CustomRule>(`/rules/${id}`);
    return res.data;
  },

  updateRule: async (id: string, payload: RuleRequest): Promise<CustomRule> => {
    const res = await apiClient.put<CustomRule>(`/rules/${id}`, payload);
    return res.data;
  },

  deleteRule: async (id: string): Promise<void> => {
    await apiClient.delete(`/rules/${id}`);
  },

  toggleRule: async (id: string): Promise<CustomRule> => {
    const res = await apiClient.patch<CustomRule>(`/rules/${id}/toggle`);
    return res.data;
  },

  simulateRule: async (payload: RuleSimulationRequest): Promise<RuleSimulationResponse> => {
    const res = await apiClient.post<RuleSimulationResponse>('/rules/test', payload);
    return res.data;
  },

  listTraces: async (ruleId?: string, page = 0, size = 20): Promise<PageResponse<RuleExecutionTrace>> => {
    const res = await apiClient.get<PageResponse<RuleExecutionTrace>>('/rules/traces', {
      params: { ruleId, page, size },
    });
    return res.data;
  },
};
