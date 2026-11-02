import { apiClient } from './client';
import {
  CustomFieldDefinition,
  CreateCustomFieldDefinitionRequest,
  UpdateCustomFieldDefinitionRequest,
  TargetEntityDescriptor,
  FieldOrderDto,
  EntityCustomFieldsResponse,
  SaveEntityCustomFieldsRequest,
} from '../types/customFields';

export const customFieldsApi = {
  listEntities: async (): Promise<TargetEntityDescriptor[]> => {
    const res = await apiClient.get<TargetEntityDescriptor[]>('/custom-fields/entities');
    return res.data;
  },

  listDefinitions: async (targetEntity: string, activeOnly = false): Promise<CustomFieldDefinition[]> => {
    const res = await apiClient.get<CustomFieldDefinition[]>('/custom-fields/definitions', {
      params: { targetEntity, activeOnly },
    });
    return res.data;
  },

  getDefinition: async (id: string): Promise<CustomFieldDefinition> => {
    const res = await apiClient.get<CustomFieldDefinition>(`/custom-fields/definitions/${id}`);
    return res.data;
  },

  createDefinition: async (payload: CreateCustomFieldDefinitionRequest): Promise<CustomFieldDefinition> => {
    const res = await apiClient.post<CustomFieldDefinition>('/custom-fields/definitions', payload);
    return res.data;
  },

  updateDefinition: async (id: string, payload: UpdateCustomFieldDefinitionRequest): Promise<CustomFieldDefinition> => {
    const res = await apiClient.put<CustomFieldDefinition>(`/custom-fields/definitions/${id}`, payload);
    return res.data;
  },

  toggleStatus: async (id: string): Promise<CustomFieldDefinition> => {
    const res = await apiClient.patch<CustomFieldDefinition>(`/custom-fields/definitions/${id}/toggle-status`);
    return res.data;
  },

  reorderDefinitions: async (orders: FieldOrderDto[]): Promise<void> => {
    await apiClient.put('/custom-fields/definitions/reorder', orders);
  },

  getEntityCustomValues: async (targetEntity: string, entityId: string): Promise<EntityCustomFieldsResponse> => {
    const res = await apiClient.get<EntityCustomFieldsResponse>(`/custom-fields/values/${targetEntity}/${entityId}`);
    return res.data;
  },

  saveEntityCustomValues: async (
    targetEntity: string,
    entityId: string,
    payload: SaveEntityCustomFieldsRequest
  ): Promise<EntityCustomFieldsResponse> => {
    const res = await apiClient.put<EntityCustomFieldsResponse>(
      `/custom-fields/values/${targetEntity}/${entityId}`,
      payload
    );
    return res.data;
  },
};
