export interface User {
  id: string;
  username: string;
  email: string;
  fullName: string;
  status: 'ACTIVE' | 'INACTIVE' | 'SUSPENDED';
  profile: {
    id: string;
    name: string;
  };
  permissions?: string[];
  createdAt: string;
}

export interface UserPage {
  items: User[];
  totalElements: number;
  totalPages: number;
  currentPage: number;
}

export interface CreateUserPayload {
  username: string;
  email: string;
  password: string;
  fullName: string;
  profileId: string;
}

export interface UpdateUserPayload {
  email: string;
  fullName: string;
  status: string;
  profileId: string;
}
