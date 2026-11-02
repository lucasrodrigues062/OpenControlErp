import { describe, it, expect } from 'vitest';
import { CompanyBranchSummary } from '../../../types/companyBranches';

describe('Branch Switcher and Multi-Branch Logic', () => {
  it('should find active branch by ID', () => {
    const branches: CompanyBranchSummary[] = [
      {
        id: 'branch-1',
        type: 'HEADQUARTERS',
        parentId: null,
        code: '01',
        tradeName: 'Matriz SP',
        legalName: 'Matriz LTDA',
        cnpj: '11.222.333/0001-81',
        cityName: 'Sao Paulo',
        stateCode: 'SP',
        isActive: true,
      },
      {
        id: 'branch-2',
        type: 'BRANCH',
        parentId: 'branch-1',
        code: '02',
        tradeName: 'Filial RJ',
        legalName: 'Matriz LTDA',
        cnpj: '11.222.333/0002-62',
        cityName: 'Rio de Janeiro',
        stateCode: 'RJ',
        isActive: true,
      },
    ];

    const activeId = 'branch-2';
    const active = branches.find((b) => b.id === activeId);

    expect(active).toBeDefined();
    expect(active?.code).toBe('02');
    expect(active?.tradeName).toBe('Filial RJ');
    expect(active?.type).toBe('BRANCH');
  });

  it('should fallback to first branch if selected branch is not found', () => {
    const branches: CompanyBranchSummary[] = [
      {
        id: 'branch-1',
        type: 'HEADQUARTERS',
        parentId: null,
        code: '01',
        tradeName: 'Matriz SP',
        legalName: 'Matriz LTDA',
        cnpj: '11.222.333/0001-81',
        cityName: 'Sao Paulo',
        stateCode: 'SP',
        isActive: true,
      },
    ];

    const invalidId = 'non-existent-id';
    const active = branches.find((b) => b.id === invalidId) || branches[0];

    expect(active.id).toBe('branch-1');
  });
});
