export interface Profile {
  id: string;
  name: string;
  description?: string;
  isSystem: boolean;
  permissions: string[];
  userCount?: number;
  createdAt: string;
  updatedAt: string;
}

export interface PermissionCatalogItem {
  code: string;
  category: string;
  label: string;
  description: string;
}

export interface CreateProfilePayload {
  name: string;
  description?: string;
  permissions: string[];
}

export interface UpdateProfilePayload {
  name: string;
  description?: string;
  permissions: string[];
}
