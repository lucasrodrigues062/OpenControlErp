import { describe, it, expect } from 'vitest';
import { enUS } from '../../locales/en-US';
import { ptBR } from '../../locales/pt-BR';

describe('I18n Catalogs & Formatting Logic', () => {
  it('should have 1:1 key parity between en-US and pt-BR dictionaries', () => {
    const enKeys = {
      common: Object.keys(enUS.common).sort(),
      auth: Object.keys(enUS.auth).sort(),
      iam: Object.keys(enUS.iam).sort(),
      audit: Object.keys(enUS.audit).sort(),
    };

    const ptKeys = {
      common: Object.keys(ptBR.common).sort(),
      auth: Object.keys(ptBR.auth).sort(),
      iam: Object.keys(ptBR.iam).sort(),
      audit: Object.keys(ptBR.audit).sort(),
    };

    expect(ptKeys.common).toEqual(enKeys.common);
    expect(ptKeys.auth).toEqual(enKeys.auth);
    expect(ptKeys.iam).toEqual(enKeys.iam);
    expect(ptKeys.audit).toEqual(enKeys.audit);
  });

  it('should format currency correctly according to locale standard', () => {
    const amount = 1250.5;
    const usd = new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(amount);
    const brl = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(amount);

    expect(usd).toContain('1,250.50');
    expect(brl).toContain('1.250,50');
  });

  it('should format dates according to locale conventions', () => {
    const testDate = new Date('2026-11-02T15:30:00Z');

    const enDate = new Intl.DateTimeFormat('en-US', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      timeZone: 'UTC',
    }).format(testDate);

    const ptDate = new Intl.DateTimeFormat('pt-BR', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      timeZone: 'UTC',
    }).format(testDate);

    expect(enDate).toBe('11/02/2026');
    expect(ptDate).toBe('02/11/2026');
  });

  it('should resolve translations and replace parameters correctly', () => {
    const template = 'User {name} has role {role}';
    const params = { name: 'admin', role: 'Administrator' };

    let text = template;
    Object.entries(params).forEach(([k, v]) => {
      text = text.replace(new RegExp(`\\{${k}\\}`, 'g'), String(v));
    });

    expect(text).toBe('User admin has role Administrator');
  });
});
