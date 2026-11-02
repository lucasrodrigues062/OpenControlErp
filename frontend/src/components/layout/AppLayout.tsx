import React, { useState } from 'react';
import { Link, Outlet, useLocation } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { useI18n } from '../../context/I18nContext';
import {
  Users,
  Shield,
  History,
  LogOut,
  LayoutDashboard,
  AlertCircle,
  Sliders,
  Database,
  Building2,
  PanelLeftClose,
  PanelLeft,
} from 'lucide-react';
import { Badge } from '../ui/badge';
import { Button } from '../ui/button';
import { LanguageSwitcher } from '../common/LanguageSwitcher';
import { BranchSwitcher } from '../companies/BranchSwitcher';

export const AppLayout: React.FC = () => {
  const { user, logout, hasPermission } = useAuth();
  const { t } = useI18n();
  const location = useLocation();

  const [collapsed, setCollapsed] = useState<boolean>(() => {
    return localStorage.getItem('opencontrol_sidebar_collapsed') === 'true';
  });

  const toggleCollapsed = () => {
    setCollapsed((prev) => {
      const next = !prev;
      localStorage.setItem('opencontrol_sidebar_collapsed', String(next));
      return next;
    });
  };

  const navigation = [
    { nameKey: 'companies.title', defaultName: 'Companies & Branches', href: '/companies', icon: Building2, permission: 'COMPANIES_READ' },
    { nameKey: 'iam.usersTitle', defaultName: 'Users', href: '/users', icon: Users, permission: 'USERS_READ' },
    { nameKey: 'iam.profilesTitle', defaultName: 'Profiles & Permissions', href: '/profiles', icon: Shield, permission: 'PROFILES_MANAGE' },
    { nameKey: 'rules.title', defaultName: 'Custom Rules', href: '/rules', icon: Sliders, permission: 'RULES_READ' },
    { nameKey: 'customFields.title', defaultName: 'Custom Fields', href: '/settings/custom-fields', icon: Database, permission: 'CUSTOM_FIELDS_READ' },
    { nameKey: 'audit.title', defaultName: 'Audit Trail', href: '/audit', icon: History, permission: 'AUDIT_READ' },
  ];

  const visibleNavigation = navigation.filter(
    (item) => !item.permission || hasPermission(item.permission)
  );

  const currentNav = navigation.find((n) => n.href === location.pathname);

  return (
    <div className="min-h-screen bg-slate-50 dark:bg-slate-950 flex flex-col md:flex-row">
      {/* Sidebar */}
      <aside
        className={`bg-white dark:bg-slate-900 border-r border-slate-200 dark:border-slate-800 flex flex-col justify-between p-4 shadow-sm transition-all duration-300 ease-in-out shrink-0 ${
          collapsed ? 'w-full md:w-20' : 'w-full md:w-64'
        }`}
      >
        <div>
          {/* Brand & Collapse Header */}
          {collapsed ? (
            <div className="flex flex-col items-center gap-2 px-1 py-3 mb-6 border-b border-slate-100 dark:border-slate-800">
              <button
                onClick={toggleCollapsed}
                title="Expand sidebar"
                className="h-10 w-10 rounded-xl bg-primary text-primary-foreground flex items-center justify-center font-bold text-lg shadow-sm hover:scale-105 transition-transform"
              >
                OC
              </button>
              <Button
                variant="ghost"
                size="icon"
                onClick={toggleCollapsed}
                title="Expand sidebar"
                className="h-7 w-7 text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 hidden md:flex"
              >
                <PanelLeft className="h-4 w-4" />
              </Button>
            </div>
          ) : (
            <div className="flex items-center justify-between px-2 py-3 mb-6 border-b border-slate-100 dark:border-slate-800">
              <div className="flex items-center gap-3">
                <div className="h-9 w-9 rounded-lg bg-primary text-primary-foreground flex items-center justify-center font-bold text-lg shadow-sm">
                  OC
                </div>
                <div>
                  <h1 className="font-bold text-slate-900 dark:text-white leading-tight">OpenControl</h1>
                  <span className="text-xs text-slate-500 font-medium">Enterprise ERP</span>
                </div>
              </div>
              <Button
                variant="ghost"
                size="icon"
                onClick={toggleCollapsed}
                title="Collapse sidebar"
                className="h-8 w-8 text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 hidden md:flex"
              >
                <PanelLeftClose className="h-4 w-4" />
              </Button>
            </div>
          )}

          {/* Nav Items */}
          <nav className="space-y-1">
            {visibleNavigation.length === 0 ? (
              <div className="px-3 py-4 text-xs text-slate-400 flex items-center gap-2">
                <AlertCircle className="h-4 w-4 text-amber-500 shrink-0" />
                {!collapsed && <span>{t('common.noAccessibleModules')}</span>}
              </div>
            ) : (
              visibleNavigation.map((item) => {
                const Icon = item.icon;
                const isActive = location.pathname === item.href;
                const localizedName = t(item.nameKey);
                return (
                  <Link
                    key={item.href}
                    to={item.href}
                    title={localizedName}
                    className={`flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-colors ${
                      collapsed ? 'justify-center px-2 py-2.5' : ''
                    } ${
                      isActive
                        ? 'bg-primary/10 text-primary font-semibold'
                        : 'text-slate-600 dark:text-slate-400 hover:bg-slate-100 dark:hover:bg-slate-800 hover:text-slate-900 dark:hover:text-white'
                    }`}
                  >
                    <Icon className="h-4 w-4 shrink-0" />
                    {!collapsed && <span className="truncate">{localizedName}</span>}
                  </Link>
                );
              })
            )}
          </nav>
        </div>

        {/* User Card */}
        <div className="pt-4 border-t border-slate-100 dark:border-slate-800">
          {collapsed ? (
            <div className="flex flex-col items-center gap-2 py-1">
              <div
                className="h-8 w-8 rounded-full bg-primary/10 text-primary flex items-center justify-center text-xs font-semibold"
                title={user?.fullName || user?.username}
              >
                {(user?.fullName || user?.username || 'U').charAt(0).toUpperCase()}
              </div>
              <Button
                variant="ghost"
                size="icon"
                onClick={logout}
                title={t('common.signOut')}
                className="h-8 w-8 text-slate-500 hover:text-destructive"
              >
                <LogOut className="h-4 w-4" />
              </Button>
            </div>
          ) : (
            <div className="flex items-center justify-between px-2 py-2">
              <div className="truncate">
                <p className="text-sm font-medium text-slate-900 dark:text-white truncate">
                  {user?.fullName || user?.username}
                </p>
                <div className="flex items-center gap-1.5 mt-0.5">
                  <Badge variant="outline" className="text-[10px] px-1.5 py-0">
                    {user?.profile?.name || 'User'}
                  </Badge>
                </div>
              </div>
              <Button variant="ghost" size="icon" onClick={logout} title={t('common.signOut')}>
                <LogOut className="h-4 w-4 text-slate-500 hover:text-destructive" />
              </Button>
            </div>
          )}
        </div>
      </aside>

      {/* Main Content */}
      <main className="flex-1 flex flex-col min-w-0 overflow-y-auto">
        <header className="h-16 bg-white dark:bg-slate-900 border-b border-slate-200 dark:border-slate-800 px-6 flex items-center justify-between gap-4">
          <div className="flex items-center gap-2 min-w-0">
            <LayoutDashboard className="h-4 w-4 text-slate-400 shrink-0" />
            <span className="text-sm font-medium text-slate-600 dark:text-slate-300 truncate">
              {currentNav ? t(currentNav.nameKey) : t('common.dashboard')}
            </span>
          </div>
          <div className="flex items-center gap-3 shrink-0">
            <BranchSwitcher />
            <div className="text-xs text-slate-400 hidden lg:block">{t('common.jurisdiction')}</div>
            <LanguageSwitcher variant="full" />
          </div>
        </header>

        <div className="p-6 md:p-8 max-w-7xl w-full mx-auto">
          <Outlet />
        </div>
      </main>
    </div>
  );
};
