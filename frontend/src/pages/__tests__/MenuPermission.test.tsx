import { describe, it, expect } from 'vitest';

describe('Navigation Permission Gate Logic', () => {
  const navigation = [
    { name: 'Users', href: '/users', permission: 'USERS_READ' },
    { name: 'Profiles & Permissions', href: '/profiles', permission: 'PROFILES_MANAGE' },
    { name: 'Audit Trail', href: '/audit', permission: 'AUDIT_READ' },
  ];

  it('should only display allowed menus for user with USERS_READ', () => {
    const userPermissions = ['USERS_READ'];
    const hasPermission = (p: string) => userPermissions.includes(p);

    const visibleNavigation = navigation.filter(
      (item) => !item.permission || hasPermission(item.permission)
    );

    expect(visibleNavigation.map((n) => n.name)).toEqual(['Users']);
  });

  it('should display all menus for administrator', () => {
    const isAdministrator = true;
    const hasPermission = (_p: string) => isAdministrator;

    const visibleNavigation = navigation.filter(
      (item) => !item.permission || hasPermission(item.permission)
    );

    expect(visibleNavigation.map((n) => n.name)).toEqual([
      'Users',
      'Profiles & Permissions',
      'Audit Trail',
    ]);
  });

  it('should display no menus when user has no matching permissions', () => {
    const userPermissions: string[] = [];
    const hasPermission = (p: string) => userPermissions.includes(p);

    const visibleNavigation = navigation.filter(
      (item) => !item.permission || hasPermission(item.permission)
    );

    expect(visibleNavigation).toHaveLength(0);
  });
});
