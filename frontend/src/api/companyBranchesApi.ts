import {
  AccountantDto,
  CertificateMetadataDto,
  CompanyBranchDetail,
  CompanyBranchSummary,
  CreateCompanyBranchRequest,
  EstablishmentType,
  FiscalSettingsDto,
  UpdateCompanyBranchRequest,
  UserBranchesResponse,
} from '../types/companyBranches';

const API_BASE = '/api/v1/companies';

const getAuthHeaders = () => {
  const token = localStorage.getItem('opencontrol_token');
  return {
    'Content-Type': 'application/json',
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
  };
};

export const companyBranchesApi = {
  async list(type?: EstablishmentType, activeOnly?: boolean): Promise<CompanyBranchSummary[]> {
    const params = new URLSearchParams();
    if (type) params.append('type', type);
    if (activeOnly) params.append('activeOnly', 'true');

    const res = await fetch(`${API_BASE}?${params.toString()}`, {
      headers: getAuthHeaders(),
    });
    if (!res.ok) throw new Error(`Failed to list companies: ${res.statusText}`);
    return res.json();
  },

  async getById(id: string): Promise<CompanyBranchDetail> {
    const res = await fetch(`${API_BASE}/${id}`, {
      headers: getAuthHeaders(),
    });
    if (!res.ok) throw new Error(`Failed to get company: ${res.statusText}`);
    return res.json();
  },

  async create(req: CreateCompanyBranchRequest): Promise<CompanyBranchDetail> {
    const res = await fetch(API_BASE, {
      method: 'POST',
      headers: getAuthHeaders(),
      body: JSON.stringify(req),
    });
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || 'Failed to create company');
    }
    return res.json();
  },

  async update(id: string, req: UpdateCompanyBranchRequest): Promise<CompanyBranchDetail> {
    const res = await fetch(`${API_BASE}/${id}`, {
      method: 'PUT',
      headers: getAuthHeaders(),
      body: JSON.stringify(req),
    });
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || 'Failed to update company');
    }
    return res.json();
  },

  async toggleStatus(id: string): Promise<CompanyBranchDetail> {
    const res = await fetch(`${API_BASE}/${id}/status`, {
      method: 'PATCH',
      headers: getAuthHeaders(),
    });
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || 'Failed to toggle status');
    }
    return res.json();
  },

  async updateFiscalSettings(id: string, req: FiscalSettingsDto): Promise<FiscalSettingsDto> {
    const res = await fetch(`${API_BASE}/${id}/fiscal-settings`, {
      method: 'PUT',
      headers: getAuthHeaders(),
      body: JSON.stringify(req),
    });
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || 'Failed to update fiscal settings');
    }
    return res.json();
  },

  async updateAccountant(id: string, req: AccountantDto): Promise<AccountantDto> {
    const res = await fetch(`${API_BASE}/${id}/accountant`, {
      method: 'PUT',
      headers: getAuthHeaders(),
      body: JSON.stringify(req),
    });
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || 'Failed to update accountant');
    }
    return res.json();
  },

  async getCertificate(id: string): Promise<CertificateMetadataDto | null> {
    const res = await fetch(`${API_BASE}/${id}/certificate`, {
      headers: getAuthHeaders(),
    });
    if (res.status === 404) return null;
    if (!res.ok) throw new Error(`Failed to get certificate: ${res.statusText}`);
    return res.json();
  },

  async uploadCertificate(id: string, file: File, password: string): Promise<CertificateMetadataDto> {
    const token = localStorage.getItem('opencontrol_token');
    const formData = new FormData();
    formData.append('file', file);
    formData.append('password', password);

    const res = await fetch(`${API_BASE}/${id}/certificate`, {
      method: 'POST',
      headers: {
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
      body: formData,
    });
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || 'Failed to upload certificate');
    }
    return res.json();
  },

  async deleteCertificate(id: string): Promise<void> {
    const res = await fetch(`${API_BASE}/${id}/certificate`, {
      method: 'DELETE',
      headers: getAuthHeaders(),
    });
    if (!res.ok) throw new Error(`Failed to delete certificate: ${res.statusText}`);
  },

  async getUserBranches(): Promise<UserBranchesResponse> {
    const res = await fetch(`${API_BASE}/user-branches`, {
      headers: getAuthHeaders(),
    });
    if (!res.ok) throw new Error(`Failed to load user branches: ${res.statusText}`);
    return res.json();
  },
};
