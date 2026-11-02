import React, { useState, useEffect } from 'react';
import { usersApi } from '../api/usersApi';
import { profilesApi } from '../api/profilesApi';
import { User } from '../types/user';
import { Profile } from '../types/profile';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '../components/ui/card';
import { Button } from '../components/ui/button';
import { Input } from '../components/ui/input';
import { Badge } from '../components/ui/badge';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from '../components/ui/dialog';
import { PermissionGate } from '../components/common/PermissionGate';
import { useI18n } from '../context/I18nContext';
import { UserPlus, Search, Edit2, ShieldAlert } from 'lucide-react';

export const UsersPage: React.FC = () => {
  const { t } = useI18n();
  const [users, setUsers] = useState<User[]>([]);
  const [profiles, setProfiles] = useState<Profile[]>([]);
  const [query, setQuery] = useState('');
  const [loading, setLoading] = useState(false);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingUser, setEditingUser] = useState<User | null>(null);

  // Form State
  const [formData, setFormData] = useState({
    username: '',
    email: '',
    password: '',
    fullName: '',
    status: 'ACTIVE',
    profileId: '',
  });
  const [formError, setFormError] = useState<string | null>(null);
  const [formLoading, setFormLoading] = useState(false);

  const loadData = async () => {
    setLoading(true);
    try {
      const [usersData, profilesData] = await Promise.all([
        usersApi.listUsers(query),
        profilesApi.listProfiles(),
      ]);
      setUsers(usersData.items);
      setProfiles(profilesData);
      if (profilesData.length > 0 && !formData.profileId) {
        setFormData((prev) => ({ ...prev, profileId: profilesData[0].id }));
      }
    } catch (err: any) {
      console.error('Failed to load users data', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, [query]);

  const handleOpenCreateModal = () => {
    setEditingUser(null);
    setFormData({
      username: '',
      email: '',
      password: '',
      fullName: '',
      status: 'ACTIVE',
      profileId: profiles[0]?.id || '',
    });
    setFormError(null);
    setIsModalOpen(true);
  };

  const handleOpenEditModal = (user: User) => {
    setEditingUser(user);
    setFormData({
      username: user.username,
      email: user.email,
      password: '',
      fullName: user.fullName,
      status: user.status,
      profileId: user.profile.id,
    });
    setFormError(null);
    setIsModalOpen(true);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setFormError(null);
    setFormLoading(true);

    try {
      if (editingUser) {
        await usersApi.updateUser(editingUser.id, {
          email: formData.email,
          fullName: formData.fullName,
          status: formData.status,
          profileId: formData.profileId,
        });
      } else {
        await usersApi.createUser({
          username: formData.username,
          email: formData.email,
          password: formData.password,
          fullName: formData.fullName,
          profileId: formData.profileId,
        });
      }
      setIsModalOpen(false);
      loadData();
    } catch (err: any) {
      setFormError(err.response?.data?.detail || t('common.actions'));
    } finally {
      setFormLoading(false);
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h2 className="text-2xl font-bold tracking-tight text-slate-900 dark:text-white">
            {t('iam.usersTitle')}
          </h2>
          <p className="text-sm text-slate-500 dark:text-slate-400">
            {t('iam.usersDescription')}
          </p>
        </div>
        <PermissionGate permission="USERS_WRITE">
          <Button onClick={handleOpenCreateModal} className="gap-2">
            <UserPlus className="h-4 w-4" />
            {t('iam.createUser')}
          </Button>
        </PermissionGate>
      </div>

      <Card>
        <CardHeader className="pb-3">
          <div className="flex items-center justify-between">
            <div>
              <CardTitle>{t('iam.usersTitle')}</CardTitle>
              <CardDescription>{t('common.actions')}: {users.length}</CardDescription>
            </div>
            <div className="relative w-64">
              <Search className="absolute left-3 top-2.5 h-4 w-4 text-slate-400" />
              <Input
                placeholder={t('common.search')}
                className="pl-9 h-9"
                value={query}
                onChange={(e) => setQuery(e.target.value)}
              />
            </div>
          </div>
        </CardHeader>
        <CardContent>
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="bg-slate-50 dark:bg-slate-900/50 text-slate-500 uppercase text-[11px] font-semibold border-y">
                <tr>
                  <th className="py-3 px-4">{t('auth.username')}</th>
                  <th className="py-3 px-4">{t('iam.email')}</th>
                  <th className="py-3 px-4">{t('iam.profile')}</th>
                  <th className="py-3 px-4">{t('common.status')}</th>
                  <th className="py-3 px-4 text-right">{t('common.actions')}</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 dark:divide-slate-800">
                {users.map((user) => (
                  <tr key={user.id} className="hover:bg-slate-50/50 dark:hover:bg-slate-900/50">
                    <td className="py-3 px-4">
                      <div className="font-medium text-slate-900 dark:text-white">
                        {user.fullName}
                      </div>
                      <div className="text-xs text-slate-500">@{user.username}</div>
                    </td>
                    <td className="py-3 px-4 text-slate-600 dark:text-slate-300">{user.email}</td>
                    <td className="py-3 px-4">
                      <Badge variant="outline" className="font-medium bg-primary/5 text-primary">
                        {user.profile.name}
                      </Badge>
                    </td>
                    <td className="py-3 px-4">
                      <Badge
                        variant={user.status === 'ACTIVE' ? 'success' : 'destructive'}
                        className="text-[11px]"
                      >
                        {user.status === 'ACTIVE' ? t('common.active') : t('common.inactive')}
                      </Badge>
                    </td>
                    <td className="py-3 px-4 text-right">
                      <PermissionGate permission="USERS_WRITE">
                        <Button
                          variant="ghost"
                          size="icon"
                          onClick={() => handleOpenEditModal(user)}
                          title={t('common.edit')}
                        >
                          <Edit2 className="h-4 w-4 text-slate-500 hover:text-primary" />
                        </Button>
                      </PermissionGate>
                    </td>
                  </tr>
                ))}
                {users.length === 0 && !loading && (
                  <tr>
                    <td colSpan={5} className="py-8 text-center text-slate-500">
                      {t('common.noData')}
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </CardContent>
      </Card>

      {/* User Create / Edit Dialog */}
      <Dialog open={isModalOpen} onOpenChange={setIsModalOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>{editingUser ? t('iam.editUser') : t('iam.createUser')}</DialogTitle>
          </DialogHeader>

          <form onSubmit={handleSubmit} className="space-y-4 py-2">
            {formError && (
              <div className="p-3 rounded-lg bg-destructive/10 text-destructive text-sm font-medium flex items-center gap-2">
                <ShieldAlert className="h-4 w-4 flex-shrink-0" />
                <span>{formError}</span>
              </div>
            )}

            {!editingUser && (
              <div className="space-y-1.5">
                <label className="text-xs font-semibold text-slate-600 dark:text-slate-400">
                  {t('auth.username')}
                </label>
                <Input
                  value={formData.username}
                  onChange={(e) => setFormData({ ...formData, username: e.target.value })}
                  placeholder="e.g. maria.souza"
                  required
                />
              </div>
            )}

            <div className="space-y-1.5">
              <label className="text-xs font-semibold text-slate-600 dark:text-slate-400">
                {t('iam.fullName')}
              </label>
              <Input
                value={formData.fullName}
                onChange={(e) => setFormData({ ...formData, fullName: e.target.value })}
                placeholder={t('iam.fullNamePlaceholder')}
                required
              />
            </div>

            <div className="space-y-1.5">
              <label className="text-xs font-semibold text-slate-600 dark:text-slate-400">
                {t('iam.email')}
              </label>
              <Input
                type="email"
                value={formData.email}
                onChange={(e) => setFormData({ ...formData, email: e.target.value })}
                placeholder={t('iam.emailPlaceholder')}
                required
              />
            </div>

            {!editingUser && (
              <div className="space-y-1.5">
                <label className="text-xs font-semibold text-slate-600 dark:text-slate-400">
                  {t('auth.password')}
                </label>
                <Input
                  type="password"
                  value={formData.password}
                  onChange={(e) => setFormData({ ...formData, password: e.target.value })}
                  placeholder={t('auth.passwordPlaceholder')}
                  required
                />
              </div>
            )}

            <div className="space-y-1.5">
              <label className="text-xs font-semibold text-slate-600 dark:text-slate-400">
                {t('iam.profile')}
              </label>
              <select
                className="w-full h-9 rounded-md border border-input bg-background px-3 py-1 text-sm shadow-sm focus:outline-none focus:ring-1 focus:ring-ring"
                value={formData.profileId}
                onChange={(e) => setFormData({ ...formData, profileId: e.target.value })}
                required
              >
                {profiles.map((p) => (
                  <option key={p.id} value={p.id}>
                    {p.name} {p.isSystem ? `(${t('common.system')})` : ''}
                  </option>
                ))}
              </select>
            </div>

            {editingUser && (
              <div className="space-y-1.5">
                <label className="text-xs font-semibold text-slate-600 dark:text-slate-400">
                  {t('common.status')}
                </label>
                <select
                  className="w-full h-9 rounded-md border border-input bg-background px-3 py-1 text-sm shadow-sm focus:outline-none focus:ring-1 focus:ring-ring"
                  value={formData.status}
                  onChange={(e) => setFormData({ ...formData, status: e.target.value })}
                >
                  <option value="ACTIVE">{t('common.active')}</option>
                  <option value="INACTIVE">{t('common.inactive')}</option>
                </select>
              </div>
            )}

            <DialogFooter className="pt-2">
              <Button type="button" variant="outline" onClick={() => setIsModalOpen(false)}>
                {t('common.cancel')}
              </Button>
              <Button type="submit" disabled={formLoading}>
                {formLoading ? t('common.loading') : t('common.save')}
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>
    </div>
  );
};
