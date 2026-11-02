import React from 'react';
import { Link, Outlet, useLocation } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { Users, Shield, History, LogOut, LayoutDashboard } from 'lucide-react';
import { Badge } from '../ui/badge';
import { Button } from '../ui/button';

export const AppLayout: React.FC = () => {
  const { user, logout } = useAuth();
  const location = useLocation();

  const navigation = [
    { name: 'Users', href: '/users', icon: Users, permission: 'USERS_READ' },
    { name: 'Profiles & Permissions', href: '/profiles', icon: Shield, permission: 'PROFILES_MANAGE' },
    { name: 'Audit Trail', href: '/audit', icon: History, permission: 'AUDIT_READ' },
  ];

  return (
    <div className="min-h-screen bg-slate-50 dark:bg-slate-950 flex flex-col md:flex-row">
      {/* Sidebar */}
      <aside className="w-full md:w-64 bg-white dark:bg-slate-900 border-r border-slate-200 dark:border-slate-800 flex flex-col justify-between p-4 shadow-sm">
        <div>
          <div className="flex items-center gap-3 px-2 py-3 mb-6 border-b border-slate-100 dark:border-slate-800">
            <div className="h-9 w-9 rounded-lg bg-primary text-primary-foreground flex items-center justify-center font-bold text-lg shadow-sm">
              OC
            </div>
            <div>
              <h1 className="font-bold text-slate-900 dark:text-white leading-tight">OpenControl</h1>
              <span className="text-xs text-slate-500 font-medium">Enterprise ERP</span>
            </div>
          </div>

          <nav className="space-y-1">
            {navigation.map((item) => {
              const Icon = item.icon;
              const isActive = location.pathname === item.href;
              return (
                <Link
                  key={item.name}
                  to={item.href}
                  className={`flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-colors ${
                    isActive
                      ? 'bg-primary/10 text-primary font-semibold'
                      : 'text-slate-600 dark:text-slate-400 hover:bg-slate-100 dark:hover:bg-slate-800 hover:text-slate-900 dark:hover:text-white'
                  }`}
                >
                  <Icon className="h-4 w-4" />
                  {item.name}
                </Link>
              );
            })}
          </nav>
        </div>

        {/* User Card */}
        <div className="pt-4 border-t border-slate-100 dark:border-slate-800">
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
            <Button variant="ghost" size="icon" onClick={logout} title="Sign Out">
              <LogOut className="h-4 w-4 text-slate-500 hover:text-destructive" />
            </Button>
          </div>
        </div>
      </aside>

      {/* Main Content */}
      <main className="flex-1 flex flex-col min-w-0 overflow-y-auto">
        <header className="h-16 bg-white dark:bg-slate-900 border-b border-slate-200 dark:border-slate-800 px-6 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <LayoutDashboard className="h-4 w-4 text-slate-400" />
            <span className="text-sm font-medium text-slate-600 dark:text-slate-300">
              {navigation.find((n) => n.href === location.pathname)?.name || 'Dashboard'}
            </span>
          </div>
          <div className="text-xs text-slate-400">Jurisdiction: BR (Federal)</div>
        </header>

        <div className="p-6 md:p-8 max-w-7xl w-full mx-auto">
          <Outlet />
        </div>
      </main>
    </div>
  );
};
