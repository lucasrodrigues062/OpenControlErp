import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AuthProvider, useAuth } from './context/AuthContext';
import { ProtectedRoute } from './components/common/ProtectedRoute';
import { AppLayout } from './components/layout/AppLayout';
import { LoginPage } from './pages/LoginPage';
import { UsersPage } from './pages/UsersPage';
import { ProfilesPage } from './pages/ProfilesPage';
import { AuditPage } from './pages/AuditPage';

const queryClient = new QueryClient();

const IndexRedirect: React.FC = () => {
  const { hasPermission } = useAuth();
  if (hasPermission('USERS_READ')) return <Navigate to="/users" replace />;
  if (hasPermission('PROFILES_MANAGE')) return <Navigate to="/profiles" replace />;
  if (hasPermission('AUDIT_READ')) return <Navigate to="/audit" replace />;
  return (
    <div className="flex flex-col items-center justify-center py-16 text-center">
      <h2 className="text-lg font-semibold text-slate-800 dark:text-slate-100">Welcome to OpenControl ERP</h2>
      <p className="text-sm text-slate-500 mt-2 max-w-md">
        You currently have no module permissions assigned to your profile. Please contact an administrator.
      </p>
    </div>
  );
};

export const App: React.FC = () => {
  return (
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <BrowserRouter>
          <Routes>
            <Route path="/login" element={<LoginPage />} />

            <Route element={<ProtectedRoute />}>
              <Route element={<AppLayout />}>
                <Route index element={<IndexRedirect />} />
                <Route element={<ProtectedRoute requiredPermission="USERS_READ" />}>
                  <Route path="/users" element={<UsersPage />} />
                </Route>
                <Route element={<ProtectedRoute requiredPermission="PROFILES_MANAGE" />}>
                  <Route path="/profiles" element={<ProfilesPage />} />
                </Route>
                <Route element={<ProtectedRoute requiredPermission="AUDIT_READ" />}>
                  <Route path="/audit" element={<AuditPage />} />
                </Route>
              </Route>
            </Route>

            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </BrowserRouter>
      </AuthProvider>
    </QueryClientProvider>
  );
};

export default App;
