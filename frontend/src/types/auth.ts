export interface UserSummary {
  id: string;
  username: string;
  email: string;
  fullName: string;
  preferredLanguage?: string;
  status: 'ACTIVE' | 'INACTIVE' | 'SUSPENDED';
  profile: {
    id: string;
    name: string;
  };
  permissions?: string[];
}

export interface LoginResponse {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
  user: UserSummary;
}

export interface AuthContextType {
  user: UserSummary | null;
  token: string | null;
  isAuthenticated: boolean;
  login: (data: LoginResponse) => void;
  logout: () => void;
  hasPermission: (permission: string) => boolean;
}
