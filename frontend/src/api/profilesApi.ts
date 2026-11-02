import { apiClient } from './client';
import { CreateProfilePayload, PermissionCatalogItem, Profile, UpdateProfilePayload } from '../types/profile';

export const profilesApi = {
  listProfiles: async (): Promise<Profile[]> => {
    const res = await apiClient.get<Profile[]>('/iam/profiles');
    return res.data;
  },

  createProfile: async (payload: CreateProfilePayload): Promise<Profile> => {
    const res = await apiClient.post<Profile>('/iam/profiles', payload);
    return res.data;
  },

  updateProfile: async (id: string, payload: UpdateProfilePayload): Promise<Profile> => {
    const res = await apiClient.put<Profile>(`/iam/profiles/${id}`, payload);
    return res.data;
  },

  deleteProfile: async (id: string): Promise<void> => {
    await apiClient.delete(`/iam/profiles/${id}`);
  },

  getPermissionCatalog: async (): Promise<PermissionCatalogItem[]> => {
    const res = await apiClient.get<PermissionCatalogItem[]>('/iam/permissions');
    return res.data;
  },
};
