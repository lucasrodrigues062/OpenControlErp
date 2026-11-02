import { apiClient } from './client';
import { CreateUserPayload, UpdateUserPayload, User, UserPage, UserPreferencesPayload, UserPreferencesResponse } from '../types/user';

export const usersApi = {
  listUsers: async (query?: string, status?: string, page = 0, size = 20): Promise<UserPage> => {
    const res = await apiClient.get<UserPage>('/iam/users', {
      params: { query, status, page, size },
    });
    return res.data;
  },

  createUser: async (payload: CreateUserPayload): Promise<User> => {
    const res = await apiClient.post<User>('/iam/users', payload);
    return res.data;
  },

  updateUser: async (id: string, payload: UpdateUserPayload): Promise<User> => {
    const res = await apiClient.put<User>(`/iam/users/${id}`, payload);
    return res.data;
  },

  getUser: async (id: string): Promise<User> => {
    const res = await apiClient.get<User>(`/iam/users/${id}`);
    return res.data;
  },

  updatePreferences: async (payload: UserPreferencesPayload): Promise<UserPreferencesResponse> => {
    const res = await apiClient.patch<UserPreferencesResponse>('/iam/users/me/preferences', payload);
    return res.data;
  },
};
