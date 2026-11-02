import { apiClient } from './client';
import { LoginResponse } from '../types/auth';

export const authApi = {
  login: async (username: string, password: string): Promise<LoginResponse> => {
    const res = await apiClient.post<LoginResponse>('/auth/login', { username, password });
    return res.data;
  },
};
