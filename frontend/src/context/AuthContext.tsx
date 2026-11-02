import React, { createContext, useContext, useState } from 'react';
import { AuthContextType, LoginResponse, UserSummary } from '../types/auth';

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<UserSummary | null>(() => {
    const saved = localStorage.getItem('opencontrol_user');
    return saved ? JSON.parse(saved) : null;
  });
  const [token, setToken] = useState<string | null>(() => {
    return localStorage.getItem('opencontrol_token');
  });

  const login = (data: LoginResponse) => {
    setToken(data.accessToken);
    setUser(data.user);
    localStorage.setItem('opencontrol_token', data.accessToken);
    localStorage.setItem('opencontrol_user', JSON.stringify(data.user));
    if (data.user.preferredLanguage === 'en-US' || data.user.preferredLanguage === 'pt-BR') {
      localStorage.setItem('opencontrol_language', data.user.preferredLanguage);
      document.documentElement.lang = data.user.preferredLanguage;
    }
  };

  const logout = () => {
    setToken(null);
    setUser(null);
    localStorage.removeItem('opencontrol_token');
    localStorage.removeItem('opencontrol_user');
  };

  const hasPermission = (permission: string): boolean => {
    if (!user) return false;
    if (user.profile.name.toLowerCase() === 'administrator') return true;
    return user.permissions?.includes(permission) ?? false;
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        token,
        isAuthenticated: !!token,
        login,
        logout,
        hasPermission,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = (): AuthContextType => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
