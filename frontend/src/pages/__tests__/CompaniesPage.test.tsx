import { describe, it, expect } from 'vitest';
import { CreateCompanyBranchRequest, FiscalSettingsDto } from '../../types/companyBranches';

describe('Company and Branch Model and Logic Tests', () => {
  it('should validate creation request structure for Headquarters', () => {
    const req: CreateCompanyBranchRequest = {
      type: 'HEADQUARTERS',
      code: '01',
      tradeName: 'OpenControl Matriz',
      legalName: 'OpenControl Tecnologia LTDA',
      cnpj: '11222333000181',
      stateRegistration: '111222333444',
      isStateRegistrationExempt: false,
      address: {
        cityName: 'Sao Paulo',
        stateCode: 'SP',
        countryCode: 'BRA',
      },
    };

    expect(req.type).toBe('HEADQUARTERS');
    expect(req.code).toBe('01');
    expect(req.cnpj).toBe('11222333000181');
    expect(req.address.stateCode).toBe('SP');
  });

  it('should link Branch to parent Headquarters', () => {
    const req: CreateCompanyBranchRequest = {
      type: 'BRANCH',
      parentId: 'hq-uuid-1234',
      code: '02',
      tradeName: 'OpenControl Filial Campinas',
      legalName: 'OpenControl Tecnologia LTDA',
      cnpj: '11222333000262',
      address: {
        cityName: 'Campinas',
        stateCode: 'SP',
      },
    };

    expect(req.type).toBe('BRANCH');
    expect(req.parentId).toBe('hq-uuid-1234');
    expect(req.code).toBe('02');
  });

  it('should handle fiscal settings initialization', () => {
    const settings: FiscalSettingsDto = {
      taxRegime: 'SIMPLES_NACIONAL',
      pisCofinsRegime: 'CUMULATIVO',
      isTaxSubstitute: false,
      isIpiTaxpayer: false,
      isCulturalPromoter: false,
      calculateFcp: false,
      deductIcmsFromPiscofinsBasis: true,
      nfeEnvironment: 'HOMOLOGATION',
      nfeSeries: '1',
      nfeModel: '55',
    };

    expect(settings.taxRegime).toBe('SIMPLES_NACIONAL');
    expect(settings.nfeEnvironment).toBe('HOMOLOGATION');
    expect(settings.deductIcmsFromPiscofinsBasis).toBe(true);
  });
});
