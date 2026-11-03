import React, { useState, useEffect } from 'react';
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
  Contact,
  UserCheck,
  Kanban,
  ChevronDown,
  ChevronRight,
  FolderKanban,
  ShieldCheck,
  Settings2,
} from 'lucide-react';
import { Badge } from '../ui/badge';
import { Button } from '../ui/button';
import { LanguageSwitcher } from '../common/LanguageSwitcher';
import { BranchSwitcher } from '../companies/BranchSwitcher';

interface NavItem {
  nameKey: string;
  defaultName: string;
  href: string;
  icon: any;
  permission?: string;
}

interface NavGroup {
  id: string;
  titleKey: { pt: string; en: string };
  icon: any;
  items: NavItem[];
}

const navGroups: NavGroup[] = [
  {
    id: 'crm',
    titleKey: { pt: 'Comercial & CRM', en: 'Sales & CRM' },
    icon: FolderKanban,
    items: [
      { nameKey: 'crm.customersTitle', defaultName: 'Clientes (CRM)', href: '/crm/customers', icon: Contact, permission: 'CRM_READ' },
      { nameKey: 'crm.leadsTitle', defaultName: 'Leads & Funil', href: '/crm/leads', icon: UserCheck, permission: 'CRM_READ' },
      { nameKey: 'crm.pipelineTitle', defaultName: 'Pipeline de Vendas', href: '/crm/pipeline', icon: Kanban, permission: 'CRM_READ' },
    ],
  },
  {
    id: 'organization',
    titleKey: { pt: 'Estrutura Empresarial', en: 'Organization' },
    icon: Building2,
    items: [
      { nameKey: 'companies.title', defaultName: 'Empresas & Filiais', href: '/companies', icon: Building2, permission: 'COMPANIES_READ' },
    ],
  },
  {
    id: 'iam',
    titleKey: { pt: 'Segurança & Acesso', en: 'Security & Access' },
    icon: ShieldCheck,
    items: [
      { nameKey: 'iam.usersTitle', defaultName: 'Usuários', href: '/users', icon: Users, permission: 'USERS_READ' },
      { nameKey: 'iam.profilesTitle', defaultName: 'Perfis & Permissões', href: '/profiles', icon: Shield, permission: 'PROFILES_MANAGE' },
    ],
  },
  {
    id: 'system',
    titleKey: { pt: 'Governança & Sistema', en: 'System & Governance' },
    icon: Settings2,
    items: [
      { nameKey: 'rules.title', defaultName: 'Regras de Negócio', href: '/rules', icon: Sliders, permission: 'RULES_READ' },
      { nameKey: 'customFields.title', defaultName: 'Campos Customizados', href: '/settings/custom-fields', icon: Database, permission: 'CUSTOM_FIELDS_READ' },
      { nameKey: 'audit.title', defaultName: 'Trilha de Auditoria', href: '/audit', icon: History, permission: 'AUDIT_READ' },
    ],
  },
];

export const AppLayout: React.FC = () => {
  const { user, logout, hasPermission } = useAuth();
  const { t, currentLanguage } = useI18n();
  const location = useLocation();

  const [collapsed, setCollapsed] = useState<boolean>(() => {
    return localStorage.getItem('opencontrol_sidebar_collapsed') === 'true';
  });

  const [collapsedGroups, setCollapsedGroups] = useState<Record<string, boolean>>(() => {
    try {
      const saved = localStorage.getItem('opencontrol_sidebar_groups');
      return saved ? JSON.parse(saved) : {};
    } catch {
      return {};
    }
  });

  // Auto-expand group of currently active route
  useEffect(() => {
    const activeGroup = navGroups.find((g) => g.items.some((item) => item.href === location.pathname));
    if (activeGroup && collapsedGroups[activeGroup.id]) {
      setCollapsedGroups((prev) => {
        const next = { ...prev, [activeGroup.id]: false };
        localStorage.setItem('opencontrol_sidebar_groups', JSON.stringify(next));
        return next;
      });
    }
  }, [location.pathname]);

  const toggleCollapsed = () => {
    setCollapsed((prev) => {
      const next = !prev;
      localStorage.setItem('opencontrol_sidebar_collapsed', String(next));
      return next;
    });
  };

  const toggleGroup = (groupId: string) => {
    setCollapsedGroups((prev) => {
      const next = { ...prev, [groupId]: !prev[groupId] };
      localStorage.setItem('opencontrol_sidebar_groups', JSON.stringify(next));
      return next;
    });
  };

  const isPt = currentLanguage === 'pt-BR';

  const allNavItems = navGroups.flatMap((g) => g.items);
  const currentNav = allNavItems.find((n) => n.href === location.pathname);

  // Filter accessible items per group
  const getVisibleItemsForGroup = (group: NavGroup) => {
    return group.items.filter((item) => !item.permission || hasPermission(item.permission));
  };

  const totalVisibleItems = navGroups.reduce(
    (acc, group) => acc + getVisibleItemsForGroup(group).length,
    0
  );

  return (
    <div className="h-screen bg-slate-50 dark:bg-slate-950 flex flex-col md:flex-row overflow-hidden">
      {/* Sidebar */}
      <aside
        className={`bg-white dark:bg-slate-900 border-r border-slate-200 dark:border-slate-800 flex flex-col justify-between p-3.5 shadow-sm transition-all duration-300 ease-in-out shrink-0 h-full overflow-y-auto ${
          collapsed ? 'w-full md:w-20' : 'w-full md:w-64'
        }`}
      >
        <div className="flex-1 flex flex-col min-h-0">
          {/* Brand & Collapse Header */}
          {collapsed ? (
            <div className="flex flex-col items-center gap-2 px-1 py-2 mb-4 border-b border-slate-100 dark:border-slate-800 shrink-0">
              <button
                onClick={toggleCollapsed}
                title="Expandir barra lateral"
                className="h-10 w-10 rounded-xl bg-primary text-primary-foreground flex items-center justify-center font-bold text-lg shadow-sm hover:scale-105 transition-transform"
              >
                OC
              </button>
              <Button
                variant="ghost"
                size="icon"
                onClick={toggleCollapsed}
                title="Expandir barra lateral"
                className="h-7 w-7 text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 hidden md:flex"
              >
                <PanelLeft className="h-4 w-4" />
              </Button>
            </div>
          ) : (
            <div className="flex items-center justify-between px-2 py-2 mb-4 border-b border-slate-100 dark:border-slate-800 shrink-0">
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
                title="Recolher barra lateral"
                className="h-8 w-8 text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 hidden md:flex"
              >
                <PanelLeftClose className="h-4 w-4" />
              </Button>
            </div>
          )}

          {/* Categorized Nav Items */}
          <nav className="flex-1 overflow-y-auto space-y-3 pr-1 scrollbar-thin">
            {totalVisibleItems === 0 ? (
              <div className="px-3 py-4 text-xs text-slate-400 flex items-center gap-2">
                <AlertCircle className="h-4 w-4 text-amber-500 shrink-0" />
                {!collapsed && <span>{t('common.noAccessibleModules')}</span>}
              </div>
            ) : (
              navGroups.map((group, groupIdx) => {
                const visibleItems = getVisibleItemsForGroup(group);
                if (visibleItems.length === 0) return null;

                const isGroupCollapsed = !!collapsedGroups[group.id];
                const groupTitle = isPt ? group.titleKey.pt : group.titleKey.en;
                const GroupIcon = group.icon;
                const hasActiveChild = visibleItems.some((item) => location.pathname === item.href);

                // When sidebar is collapsed (mini 80px)
                if (collapsed) {
                  return (
                    <div key={group.id} className="space-y-1">
                      {groupIdx > 0 && (
                        <div className="my-2 border-t border-slate-100 dark:border-slate-800" />
                      )}
                      {visibleItems.map((item) => {
                        const Icon = item.icon;
                        const isActive = location.pathname === item.href;
                        const localizedName = t(item.nameKey) || item.defaultName;
                        return (
                          <Link
                            key={item.href}
                            to={item.href}
                            title={`${groupTitle} • ${localizedName}`}
                            className={`flex items-center justify-center h-10 w-10 mx-auto rounded-lg text-sm font-medium transition-colors ${
                              isActive
                                ? 'bg-primary/10 text-primary font-semibold shadow-sm'
                                : 'text-slate-600 dark:text-slate-400 hover:bg-slate-100 dark:hover:bg-slate-800 hover:text-slate-900 dark:hover:text-white'
                            }`}
                          >
                            <Icon className="h-4 w-4 shrink-0" />
                          </Link>
                        );
                      })}
                    </div>
                  );
                }

                // When sidebar is expanded (full 256px)
                return (
                  <div key={group.id} className="space-y-1">
                    {/* Collapsible Group Header */}
                    <button
                      type="button"
                      onClick={() => toggleGroup(group.id)}
                      className="group flex w-full items-center justify-between px-2 py-1 text-[11px] font-bold uppercase tracking-wider text-slate-400 hover:text-slate-700 dark:text-slate-500 dark:hover:text-slate-300 transition-colors rounded-md hover:bg-slate-50 dark:hover:bg-slate-850"
                    >
                      <div className="flex items-center gap-1.5 truncate">
                        <GroupIcon className="h-3.5 w-3.5 text-slate-400 group-hover:text-primary transition-colors shrink-0" />
                        <span className="truncate">{groupTitle}</span>
                        {isGroupCollapsed && hasActiveChild && (
                          <span
                            className="h-1.5 w-1.5 rounded-full bg-primary shrink-0"
                            title="Módulo ativo nesta categoria"
                          />
                        )}
                      </div>
                      <span className="text-slate-400 group-hover:text-slate-600 dark:group-hover:text-slate-300 shrink-0 ml-1">
                        {isGroupCollapsed ? (
                          <ChevronRight className="h-3 w-3" />
                        ) : (
                          <ChevronDown className="h-3 w-3" />
                        )}
                      </span>
                    </button>

                    {/* Group Submenu Items */}
                    {!isGroupCollapsed && (
                      <div className="ml-2 pl-2 border-l border-slate-100 dark:border-slate-800 space-y-0.5 mt-0.5">
                        {visibleItems.map((item) => {
                          const Icon = item.icon;
                          const isActive = location.pathname === item.href;
                          const localizedName = t(item.nameKey) || item.defaultName;
                          return (
                            <Link
                              key={item.href}
                              to={item.href}
                              title={localizedName}
                              className={`flex items-center gap-2.5 px-2.5 py-2 rounded-lg text-xs font-medium transition-colors ${
                                isActive
                                  ? 'bg-primary/10 text-primary font-bold shadow-sm'
                                  : 'text-slate-600 dark:text-slate-400 hover:bg-slate-100 dark:hover:bg-slate-800 hover:text-slate-900 dark:hover:text-white'
                              }`}
                            >
                              <Icon className="h-3.5 w-3.5 shrink-0" />
                              <span className="truncate">{localizedName}</span>
                            </Link>
                          );
                        })}
                      </div>
                    )}
                  </div>
                );
              })
            )}
          </nav>
        </div>

        {/* User Card */}
        <div className="pt-3 border-t border-slate-100 dark:border-slate-800 shrink-0">
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
            <div className="flex items-center justify-between px-2 py-1.5">
              <div className="truncate">
                <p className="text-xs font-semibold text-slate-900 dark:text-white truncate">
                  {user?.fullName || user?.username}
                </p>
                <div className="flex items-center gap-1.5 mt-0.5">
                  <Badge variant="outline" className="text-[10px] px-1 py-0">
                    {user?.profile?.name || 'User'}
                  </Badge>
                </div>
              </div>
              <Button variant="ghost" size="icon" onClick={logout} title={t('common.signOut')} className="h-7 w-7">
                <LogOut className="h-3.5 w-3.5 text-slate-500 hover:text-destructive" />
              </Button>
            </div>
          )}
        </div>
      </aside>

      {/* Main Content */}
      <main className="flex-1 flex flex-col min-w-0 min-h-0 h-full overflow-hidden">
        <header className="h-16 bg-white dark:bg-slate-900 border-b border-slate-200 dark:border-slate-800 px-6 flex items-center justify-between gap-4 shrink-0">
          <div className="flex items-center gap-2 min-w-0">
            <LayoutDashboard className="h-4 w-4 text-slate-400 shrink-0" />
            <span className="text-sm font-medium text-slate-600 dark:text-slate-300 truncate">
              {currentNav ? (t(currentNav.nameKey) || currentNav.defaultName) : t('common.dashboard')}
            </span>
          </div>
          <div className="flex items-center gap-3 shrink-0">
            <BranchSwitcher />
            <div className="text-xs text-slate-400 hidden lg:block">{t('common.jurisdiction')}</div>
            <LanguageSwitcher variant="full" />
          </div>
        </header>

        {location.pathname.startsWith('/crm/pipeline') ? (
          <div className="flex-1 min-h-0 flex flex-col w-full p-2.5 md:p-4 overflow-hidden">
            <Outlet />
          </div>
        ) : (
          <div className="flex-1 overflow-y-auto p-6 md:p-8 max-w-7xl w-full mx-auto">
            <Outlet />
          </div>
        )}
      </main>
    </div>
  );
};
