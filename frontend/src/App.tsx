import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AuthProvider, useAuth } from './context/AuthContext';
import { I18nProvider, useI18n } from './context/I18nContext';
import { BranchProvider } from './context/BranchContext';
import { ProtectedRoute } from './components/common/ProtectedRoute';
import { AppLayout } from './components/layout/AppLayout';
import { LoginPage } from './pages/LoginPage';
import { UsersPage } from './pages/UsersPage';
import { ProfilesPage } from './pages/ProfilesPage';
import { AuditPage } from './pages/AuditPage';
import { RulesPage } from './pages/RulesPage';
import { CustomFieldsSettingsPage } from './pages/CustomFieldsSettingsPage';
import { CompaniesPage } from './pages/CompaniesPage';

const queryClient = new QueryClient();

const IndexRedirect: React.FC = () => {
  const { hasPermission } = useAuth();
  const { t } = useI18n();

  if (hasPermission('COMPANIES_READ')) return <Navigate to="/companies" replace />;
  if (hasPermission('USERS_READ')) return <Navigate to="/users" replace />;
  if (hasPermission('PROFILES_MANAGE')) return <Navigate to="/profiles" replace />;
  if (hasPermission('RULES_READ')) return <Navigate to="/rules" replace />;
  if (hasPermission('AUDIT_READ')) return <Navigate to="/audit" replace />;

  return (
    <div className="flex flex-col items-center justify-center py-16 text-center">
      <h2 className="text-lg font-semibold text-slate-800 dark:text-slate-100">OpenControl ERP</h2>
      <p className="text-sm text-slate-500 mt-2 max-w-md">
        {t('common.noAccessibleModules')}
      </p>
    </div>
  );
};

export const App: React.FC = () => {
  return (
    <QueryClientProvider client={queryClient}>
      <I18nProvider>
        <AuthProvider>
          <BranchProvider>
            <BrowserRouter>
              <Routes>
                <Route path="/login" element={<LoginPage />} />

                <Route element={<ProtectedRoute />}>
                  <Route element={<AppLayout />}>
                    <Route index element={<IndexRedirect />} />
                    <Route element={<ProtectedRoute requiredPermission="COMPANIES_READ" />}>
                      <Route path="/companies" element={<CompaniesPage />} />
                    </Route>
                    <Route element={<ProtectedRoute requiredPermission="USERS_READ" />}>
                      <Route path="/users" element={<UsersPage />} />
                    </Route>
                    <Route element={<ProtectedRoute requiredPermission="PROFILES_MANAGE" />}>
                      <Route path="/profiles" element={<ProfilesPage />} />
                    </Route>
                    <Route element={<ProtectedRoute requiredPermission="RULES_READ" />}>
                      <Route path="/rules" element={<RulesPage />} />
                    </Route>
                    <Route element={<ProtectedRoute requiredPermission="CUSTOM_FIELDS_READ" />}>
                      <Route path="/settings/custom-fields" element={<CustomFieldsSettingsPage />} />
                    </Route>
                    <Route element={<ProtectedRoute requiredPermission="AUDIT_READ" />}>
                      <Route path="/audit" element={<AuditPage />} />
                    </Route>
                  </Route>
                </Route>

                <Route path="*" element={<Navigate to="/" replace />} />
              </Routes>
            </BrowserRouter>
          </BranchProvider>
        </AuthProvider>
      </I18nProvider>
    </QueryClientProvider>
  );
};

export default App;
