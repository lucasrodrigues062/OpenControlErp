import React, { useState, useEffect } from 'react';
import { profilesApi } from '../api/profilesApi';
import { PermissionCatalogItem, Profile } from '../types/profile';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '../components/ui/card';
import { Button } from '../components/ui/button';
import { Input } from '../components/ui/input';
import { Badge } from '../components/ui/badge';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from '../components/ui/dialog';
import { PermissionGate } from '../components/common/PermissionGate';
import { useI18n } from '../context/I18nContext';
import { Shield, ShieldPlus, Trash2, Edit2, Users, AlertCircle } from 'lucide-react';

export const ProfilesPage: React.FC = () => {
  const { t } = useI18n();
  const [profiles, setProfiles] = useState<Profile[]>([]);
  const [permissions, setPermissions] = useState<PermissionCatalogItem[]>([]);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingProfile, setEditingProfile] = useState<Profile | null>(null);

  // Form State
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [selectedPermissions, setSelectedPermissions] = useState<Set<string>>(new Set());
  const [formError, setFormError] = useState<string | null>(null);
  const [formLoading, setFormLoading] = useState(false);

  const loadData = async () => {
    try {
      const [profilesData, permCatalog] = await Promise.all([
        profilesApi.listProfiles(),
        profilesApi.getPermissionCatalog(),
      ]);
      setProfiles(profilesData);
      setPermissions(permCatalog);
    } catch (err) {
      console.error('Failed to load profile data', err);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const handleOpenCreateModal = () => {
    setEditingProfile(null);
    setName('');
    setDescription('');
    setSelectedPermissions(new Set());
    setFormError(null);
    setIsModalOpen(true);
  };

  const handleOpenEditModal = (profile: Profile) => {
    setEditingProfile(profile);
    setName(profile.name);
    setDescription(profile.description || '');
    setSelectedPermissions(new Set(profile.permissions));
    setFormError(null);
    setIsModalOpen(true);
  };

  const togglePermission = (code: string) => {
    setSelectedPermissions((prev) => {
      const next = new Set(prev);
      if (next.has(code)) {
        next.delete(code);
      } else {
        next.add(code);
      }
      return next;
    });
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setFormError(null);
    setFormLoading(true);

    try {
      if (editingProfile) {
        await profilesApi.updateProfile(editingProfile.id, {
          name,
          description,
          permissions: Array.from(selectedPermissions),
        });
      } else {
        await profilesApi.createProfile({
          name,
          description,
          permissions: Array.from(selectedPermissions),
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

  const handleDelete = async (profile: Profile) => {
    if (!confirm(`Are you sure you want to delete profile '${profile.name}'?`)) return;
    try {
      await profilesApi.deleteProfile(profile.id);
      loadData();
    } catch (err: any) {
      alert(err.response?.data?.detail || t('iam.cannotDeleteSystemProfile'));
    }
  };

  // Group permissions by category
  const categories = Array.from(new Set(permissions.map((p) => p.category)));

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h2 className="text-2xl font-bold tracking-tight text-slate-900 dark:text-white">
            {t('iam.profilesTitle')}
          </h2>
          <p className="text-sm text-slate-500 dark:text-slate-400">
            {t('iam.profilesDescription')}
          </p>
        </div>
        <PermissionGate permission="PROFILES_MANAGE">
          <Button onClick={handleOpenCreateModal} className="gap-2">
            <ShieldPlus className="h-4 w-4" />
            {t('iam.createProfile')}
          </Button>
        </PermissionGate>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {profiles.map((profile) => (
          <Card key={profile.id} className="flex flex-col justify-between">
            <CardHeader className="pb-3">
              <div className="flex items-start justify-between">
                <div className="space-y-1">
                  <div className="flex items-center gap-2">
                    <CardTitle className="text-lg font-bold">{profile.name}</CardTitle>
                    {profile.isSystem && (
                      <Badge variant="secondary" className="text-[10px]">
                        {t('common.system')}
                      </Badge>
                    )}
                  </div>
                  <CardDescription className="text-xs line-clamp-2">
                    {profile.description || t('common.noData')}
                  </CardDescription>
                </div>
                <div className="h-8 w-8 rounded-lg bg-primary/10 text-primary flex items-center justify-center flex-shrink-0">
                  <Shield className="h-4 w-4" />
                </div>
              </div>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="flex items-center gap-4 text-xs text-slate-500 border-t border-b py-2.5">
                <div className="flex items-center gap-1.5">
                  <Users className="h-3.5 w-3.5 text-slate-400" />
                  <span>{profile.userCount || 0} user(s)</span>
                </div>
                <div className="flex items-center gap-1.5">
                  <Shield className="h-3.5 w-3.5 text-slate-400" />
                  <span>{profile.isSystem ? t('common.all') : profile.permissions.length} {t('iam.permissions')}</span>
                </div>
              </div>

              <div className="flex items-center justify-end gap-2 pt-1">
                <PermissionGate permission="PROFILES_MANAGE">
                  <Button
                    variant="outline"
                    size="sm"
                    className="gap-1.5"
                    onClick={() => handleOpenEditModal(profile)}
                  >
                    <Edit2 className="h-3.5 w-3.5" />
                    {t('common.edit')}
                  </Button>
                  {!profile.isSystem && (
                    <Button
                      variant="ghost"
                      size="sm"
                      className="text-destructive hover:bg-destructive/10"
                      onClick={() => handleDelete(profile)}
                      disabled={(profile.userCount || 0) > 0}
                      title={
                        (profile.userCount || 0) > 0
                          ? t('iam.cannotDeleteSystemProfile')
                          : t('common.delete')
                      }
                    >
                      <Trash2 className="h-3.5 w-3.5" />
                    </Button>
                  )}
                </PermissionGate>
              </div>
            </CardContent>
          </Card>
        ))}
      </div>

      {/* Profile Create / Edit Dialog with Matrix */}
      <Dialog open={isModalOpen} onOpenChange={setIsModalOpen}>
        <DialogContent className="max-w-2xl">
          <DialogHeader>
            <DialogTitle>{editingProfile ? t('iam.editProfile') : t('iam.createProfile')}</DialogTitle>
          </DialogHeader>

          <form onSubmit={handleSubmit} className="space-y-4 py-2">
            {formError && (
              <div className="p-3 rounded-lg bg-destructive/10 text-destructive text-sm font-medium flex items-center gap-2">
                <AlertCircle className="h-4 w-4 flex-shrink-0" />
                <span>{formError}</span>
              </div>
            )}

            <div className="space-y-1.5">
              <label className="text-xs font-semibold text-slate-600 dark:text-slate-400">
                {t('iam.profileName')}
              </label>
              <Input
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder={t('iam.profileNamePlaceholder')}
                required
              />
            </div>

            <div className="space-y-1.5">
              <label className="text-xs font-semibold text-slate-600 dark:text-slate-400">
                {t('iam.description')}
              </label>
              <Input
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                placeholder={t('iam.descriptionPlaceholder')}
              />
            </div>

            {/* Permission Matrix */}
            <div className="space-y-3 pt-2">
              <label className="text-xs font-bold uppercase tracking-wider text-slate-500">
                {t('iam.permissions')}
              </label>

              <div className="max-h-72 overflow-y-auto space-y-4 border rounded-lg p-4 bg-slate-50/50 dark:bg-slate-900/50">
                {categories.map((cat) => (
                  <div key={cat} className="space-y-2">
                    <h4 className="text-xs font-bold text-slate-700 dark:text-slate-300 border-b pb-1">
                      {cat}
                    </h4>
                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
                      {permissions
                        .filter((p) => p.category === cat)
                        .map((perm) => {
                          const isChecked = selectedPermissions.has(perm.code);
                          return (
                            <label
                              key={perm.code}
                              className={`flex items-start gap-2.5 p-2 rounded-md border text-xs cursor-pointer transition-colors ${
                                isChecked
                                  ? 'bg-primary/5 border-primary/40 text-primary font-medium'
                                  : 'bg-white dark:bg-slate-800 border-slate-200 dark:border-slate-700 hover:bg-slate-50'
                              }`}
                            >
                              <input
                                type="checkbox"
                                className="mt-0.5 rounded border-slate-300 text-primary focus:ring-primary"
                                checked={isChecked}
                                onChange={() => togglePermission(perm.code)}
                              />
                              <div>
                                <div className="font-semibold text-slate-900 dark:text-white">
                                  {perm.label}
                                </div>
                                <div className="text-[11px] text-slate-500 dark:text-slate-400 leading-tight">
                                  {perm.description}
                                </div>
                              </div>
                            </label>
                          );
                        })}
                    </div>
                  </div>
                ))}
              </div>
            </div>

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
